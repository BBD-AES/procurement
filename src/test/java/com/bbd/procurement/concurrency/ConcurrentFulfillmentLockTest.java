package com.bbd.procurement.concurrency;

import com.bbd.procurement.purchaseorder.adapter.out.persistence.PurchaseRequestNotificationJpaRepository;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotificationLine;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 비관락(FOR UPDATE) 동시성 검증 — 진짜 스레드 2개 + 실제 Postgres.
 *
 * 시나리오: 부분발주 PO1(60)·PO2(40)이 같은 SO 요청 수량을 동시에 차감.
 *   - 락이 없으면 둘 다 0을 읽고 한쪽이 덮어써(lost update) 충당이 60 또는 40으로 깨진다.
 *   - findActiveBySoNumberForUpdate 의 @Lock(PESSIMISTIC_WRITE)=SELECT … FOR UPDATE 가
 *     두 트랜잭션을 직렬화 → 0→60→100 으로 정확히 합산된다.
 *
 * 실행 전제:
 *   - 로컬 Postgres 기동: docker-compose up -d  (H2로는 FOR UPDATE 동시성 재현이 불안정해 Postgres 필요)
 *   - 스키마가 이미 마이그레이션돼 있어야 함(local 프로파일 ddl-auto=validate). 없으면 앱을 local로 한 번 띄워 Flyway 적용.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 DB(local Postgres) 사용
@ActiveProfiles("local")
class ConcurrentFulfillmentLockTest {

    @Autowired
    PurchaseRequestNotificationJpaRepository notiRepo;

    @Autowired
    PlatformTransactionManager txManager;

    private static final List<PurchaseRequestStatus> ACTIVE =
            List.of(PurchaseRequestStatus.PENDING, PurchaseRequestStatus.PARTIAL);

    @Test
    @DisplayName("부분발주 PO1(60)·PO2(40) 동시 수량 차감 → 비관락 직렬화로 충당 정확히 100")
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 테스트 자체 트랜잭션 비활성 → 스레드가 각자 커밋된 트랜잭션
    void 동시_수량차감_정확히_100() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        String evt = UUID.randomUUID().toString(); // event_id 컬럼 길이 36 = UUID 길이와 동일
        String so = "SO-" + UUID.randomUUID().toString().substring(0, 8);

        // given: 요청수량 100짜리 알림 1건 커밋
        tx.executeWithoutResult(s -> notiRepo.save(PurchaseRequestNotification.create(
                evt, so, "WH-HQ-001", "{}", LocalDateTime.now(),
                List.of(PurchaseRequestNotificationLine.create("A-001", 100)))));

        // when: 두 스레드가 동시에 FOR UPDATE 로 잠그고 차감(60, 40)
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        Future<?> f1 = pool.submit(() -> { go.await(); deduct(tx, so, 60); return null; });
        Future<?> f2 = pool.submit(() -> { go.await(); deduct(tx, so, 40); return null; });
        go.countDown(); // 동시 출발
        f1.get();
        f2.get();
        pool.shutdown();

        // then: 충당 = 100 (한쪽이 덮어쓰는 lost update 없이 둘 다 합산)
        int fulfilled = notiRepo.findByEventId(evt).orElseThrow()
                .getLines().get(0).getFulfilledQty();
        assertEquals(100, fulfilled);
    }

    /** 한 트랜잭션: FOR UPDATE 로 알림을 잠그고 수량을 충당한다. */
    private void deduct(TransactionTemplate tx, String so, int qty) {
        tx.executeWithoutResult(s -> {
            PurchaseRequestNotification n = notiRepo
                    .findActiveBySoNumberForUpdate(so, ACTIVE) // ← @Lock(PESSIMISTIC_WRITE) = SELECT … FOR UPDATE
                    .get(0);
            n.applyFulfillment("A-001", qty);
            notiRepo.save(n);
        });
    }
}
