package com.bbd.procurement.concurrency;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotificationLine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 동시성 중복처리 방지 — @Version 낙관적 락 통합 검증 (실제 H2 flush).
 *
 * 공통 시나리오: 두 직원이 같은 행을 각자 조회(둘 다 version 0 스냅샷) →
 *   ① 먼저 커밋한 직원의 변경은 성공(version 1) →
 *   ② 낡은 version 0 스냅샷으로 동시에 커밋하려는 둘째 직원은 OptimisticLockException 으로 거부.
 * → 재고 이중반영 / 중복 발주 / 수량 이중차감을 DB 레벨에서 차단한다.
 *
 * 발표자료 시나리오 매핑:
 *   1. 동일 PO/WO 동시에 다른 사람이 입고 완료 처리
 *   2. 요청 대기 탭에 쌓인 주문을 동시에 다른 직원이 처리 시도
 *   3. SO에 대한 부분 입고 동시 수량 차감
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
})
class ConcurrentDuplicatePreventionTest {

    @PersistenceContext
    EntityManager em;

    // ───────────────────────────── 헬퍼 ─────────────────────────────

    /**
     * 한 직원이 같은 행을 조회한 것처럼, version 0 스냅샷 1개를 돌려준다.
     * detach 전에 init 으로 지연로딩 연관(lines)을 초기화해 둔다(스냅샷이므로 이후 세션과 분리).
     */
    private <T> T snapshot(Class<T> type, Object id, Consumer<T> init) {
        T e = em.find(type, id);
        init.accept(e);
        em.detach(e);
        return e;
    }

    /** 한 직원의 변경을 커밋(merge + flush)해 version 을 올린다. */
    private void commit(Object entity) {
        em.merge(entity);
        em.flush();
        em.clear();
    }

    // ─────────────────────── 시나리오 1 ───────────────────────

    @Test
    @DisplayName("1. 동일 PO를 동시에 다른 사람이 입고 완료 처리 → 둘째는 낙관락으로 거부(재고 이중반영 방지)")
    void 동일_PO_동시_입고완료_차단() {
        // given: ORDERED 상태 PO 1건 적재 (version 0)
        PurchaseOrder po = PurchaseOrder.create(
                "PO-2026-000001", "V-001", "WH-HQ-001", "SO-1", null, null,
                List.of(PurchaseOrderLine.create(1, "A-001", "부품A",
                        new BigDecimal("1000"), 10, "C", "EA", 0, true, "BUY")),
                1L, null);
        po.markOrdered(1L);
        em.persist(po);
        em.flush();
        Long id = po.getId();
        em.clear();

        // 두 담당자가 같은 PO를 각자 조회 (둘 다 version 0)
        PurchaseOrder employeeA = snapshot(PurchaseOrder.class, id, p -> p.getLines().size());
        PurchaseOrder employeeB = snapshot(PurchaseOrder.class, id, p -> p.getLines().size());

        // 담당자 A가 먼저 입고완료 커밋 → 성공 (version 1, RECEIVED)
        employeeA.markReceived(1001L);
        commit(employeeA);

        // A의 입고완료가 한 번만 반영됐는지 확인 (정상 EM 상태)
        PurchaseOrder afterA = em.find(PurchaseOrder.class, id);
        assertEquals(PurchaseOrderStatus.RECEIVED, afterA.getStatus());
        assertEquals(1001L, afterA.getReceivedBy());
        em.clear();

        // 담당자 B가 동시에 낡은 스냅샷으로 입고완료 시도 → 낙관락 충돌로 거부
        employeeB.markReceived(2002L);
        assertThrows(OptimisticLockException.class, () -> commit(employeeB));
    }

    // ─────────────────────── 시나리오 2 ───────────────────────

    @Test
    @DisplayName("2. 요청 대기 주문을 동시에 다른 직원이 처리(claim) 시도 → 둘째는 낙관락으로 거부(중복 발주 방지)")
    void 요청대기_동시_처리시도_차단() {
        // given: PENDING 요청 알림 1건 적재 (version 0)
        Long id = persistNotification();

        PurchaseRequestNotification employeeA = snapshot(PurchaseRequestNotification.class, id, n -> n.getLines().size());
        PurchaseRequestNotification employeeB = snapshot(PurchaseRequestNotification.class, id, n -> n.getLines().size());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleThreshold = now.minusMinutes(30);

        // 직원 A가 먼저 선점(claim) 커밋 → 성공 (version 1)
        employeeA.claim(1001L, "담당자A", now, staleThreshold);
        commit(employeeA);

        // 한 명(A)만 처리중 담당자로 확정됐는지 확인 (정상 EM 상태)
        PurchaseRequestNotification afterA = em.find(PurchaseRequestNotification.class, id);
        assertEquals(1001L, afterA.getClaimedBy());
        assertEquals("담당자A", afterA.getClaimedByName());
        em.clear();

        // 직원 B가 동시에 낡은 스냅샷으로 선점 시도 → 낙관락 충돌로 거부
        employeeB.claim(2002L, "담당자B", now, staleThreshold);
        assertThrows(OptimisticLockException.class, () -> commit(employeeB));
    }

    // ─────────────────────── 시나리오 3 ───────────────────────

    @Test
    @DisplayName("3. SO 부분 입고 수량을 동시에 차감 시도 → 둘째는 낙관락으로 거부(수량 이중차감 방지)")
    void SO_부분입고_동시_수량차감_차단() {
        // given: 요청수량 100짜리 알림 1건 적재 (version 0)
        Long id = persistNotification();

        PurchaseRequestNotification employeeA = snapshot(PurchaseRequestNotification.class, id, n -> n.getLines().size());
        PurchaseRequestNotification employeeB = snapshot(PurchaseRequestNotification.class, id, n -> n.getLines().size());

        // 직원 A가 먼저 60개 발주 차감 커밋 → 성공 (version 1)
        employeeA.applyOrder("A-001", 60);
        commit(employeeA);

        // 차감이 한 번만 반영됐는지 확인 (발주중 60, 미발주 잔여 40)
        PurchaseRequestNotification afterA = em.find(PurchaseRequestNotification.class, id);
        PurchaseRequestNotificationLine line = afterA.getLines().get(0);
        assertEquals(60, line.getOrderedQty());
        assertEquals(40, line.orderableRemaining());
        em.clear();

        // 직원 B가 동시에 낡은 스냅샷으로 60개 추가 차감 시도 → 낙관락 충돌로 거부
        // (만약 통과했다면 발주중 120 > 요청 100 의 수량 이중차감이 발생했을 것)
        employeeB.applyOrder("A-001", 60);
        assertThrows(OptimisticLockException.class, () -> commit(employeeB));
    }

    private Long persistNotification() {
        PurchaseRequestNotification noti = PurchaseRequestNotification.create(
                "evt-1", "SO-1", "WH-HQ-001", "{}", LocalDateTime.now(),
                List.of(PurchaseRequestNotificationLine.create("A-001", 100)));
        em.persist(noti);
        em.flush();
        Long id = noti.getId();
        em.clear();
        return id;
    }
}
