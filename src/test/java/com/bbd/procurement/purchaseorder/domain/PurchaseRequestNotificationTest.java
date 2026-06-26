package com.bbd.procurement.purchaseorder.domain;

import com.bbd.procurement.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 구매요청 알림 라인의 4단계 수량 추적(요청/발주중/입고완료/미발주잔여) 도메인 단위테스트.
 *
 * 핵심:
 *  - applyOrder   : 발주(DRAFT→ORDERED) 시 발주중(orderedQty) 누적, 미발주 잔여 한도 내에서만
 *  - applyFulfillment : 입고완료(RECEIVED) 시 발주중 → 입고완료로 이동
 *  - releaseOrder : 발주/작업지시 취소 시 발주중 해제
 *  - 상태(PENDING/PARTIAL/DONE)는 라인 수량에서 파생
 */
class PurchaseRequestNotificationTest {

    private static PurchaseRequestNotification notificationWithLine(String sku, int requestedQty) {
        return PurchaseRequestNotification.create(
                "evt-1", "SO-1", "WH-HQ-001", "{}", LocalDateTime.now(),
                List.of(PurchaseRequestNotificationLine.create(sku, requestedQty)));
    }

    private static PurchaseRequestNotificationLine onlyLine(PurchaseRequestNotification n) {
        return n.getLines().get(0);
    }

    @Nested
    @DisplayName("applyOrder — 발주중 누적")
    class ApplyOrder {

        @Test
        @DisplayName("부분 발주하면 발주중이 늘고 미발주 잔여가 줄며 상태는 PARTIAL")
        void 부분발주() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);

            int consumed = n.applyOrder("A-001", 40);

            PurchaseRequestNotificationLine line = onlyLine(n);
            assertEquals(40, consumed);
            assertEquals(40, line.getOrderedQty());
            assertEquals(0, line.getFulfilledQty());
            assertEquals(60, line.orderableRemaining()); // 아직 발주해야 할 양
            assertEquals(PurchaseRequestStatus.PARTIAL, line.getStatus());
            assertEquals(PurchaseRequestStatus.PARTIAL, n.getStatus());
        }

        @Test
        @DisplayName("이미 발주/입고된 수량을 넘겨 발주하면 미발주 잔여까지만 반영(초과분 무시)")
        void 초과발주는_잔여까지만() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            n.applyOrder("A-001", 70);

            int consumed = n.applyOrder("A-001", 50); // 남은 30만 가능

            assertEquals(30, consumed);
            assertEquals(100, onlyLine(n).getOrderedQty());
            assertEquals(0, onlyLine(n).orderableRemaining());
            // 전량 발주됐지만 입고 전 → 여전히 PARTIAL(목록에 남아 중복 발주 방지)
            assertEquals(PurchaseRequestStatus.PARTIAL, n.getStatus());
        }
    }

    @Nested
    @DisplayName("applyFulfillment — 발주중 → 입고완료 이동")
    class ApplyFulfillment {

        @Test
        @DisplayName("입고완료 시 입고완료 수량이 늘고 같은 만큼 발주중이 줄어든다")
        void 발주중에서_입고완료로_이동() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            n.applyOrder("A-001", 60);

            int consumed = n.applyFulfillment("A-001", 60);

            PurchaseRequestNotificationLine line = onlyLine(n);
            assertEquals(60, consumed);
            assertEquals(60, line.getFulfilledQty());
            assertEquals(0, line.getOrderedQty());     // 발주중 → 입고완료로 이동
            assertEquals(40, line.orderableRemaining());
            assertEquals(PurchaseRequestStatus.PARTIAL, n.getStatus());
        }

        @Test
        @DisplayName("요청 수량 전체가 입고완료되면 DONE")
        void 전량_입고완료시_DONE() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            n.applyOrder("A-001", 100);

            n.applyFulfillment("A-001", 100);

            PurchaseRequestNotificationLine line = onlyLine(n);
            assertEquals(100, line.getFulfilledQty());
            assertEquals(0, line.getOrderedQty());
            assertEquals(0, line.orderableRemaining());
            assertEquals(PurchaseRequestStatus.DONE, line.getStatus());
            assertEquals(PurchaseRequestStatus.DONE, n.getStatus());
        }

        @Test
        @DisplayName("발주를 거치지 않고 입고완료돼도(발주중 0) 입고완료에 정확히 반영된다")
        void 발주없이_입고완료_폴백() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);

            int consumed = n.applyFulfillment("A-001", 30);

            PurchaseRequestNotificationLine line = onlyLine(n);
            assertEquals(30, consumed);
            assertEquals(30, line.getFulfilledQty());
            assertEquals(0, line.getOrderedQty()); // 음수로 가지 않음
            assertEquals(70, line.orderableRemaining());
        }
    }

    @Nested
    @DisplayName("releaseOrder — 발주중 해제(취소)")
    class ReleaseOrder {

        @Test
        @DisplayName("발주중 수량을 해제하면 다시 미발주 잔여로 돌아오고, 진행이 없으면 PENDING")
        void 해제하면_PENDING복귀() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            n.applyOrder("A-001", 40);

            int released = n.releaseOrder("A-001", 40);

            PurchaseRequestNotificationLine line = onlyLine(n);
            assertEquals(40, released);
            assertEquals(0, line.getOrderedQty());
            assertEquals(100, line.orderableRemaining());
            assertEquals(PurchaseRequestStatus.PENDING, n.getStatus());
        }

        @Test
        @DisplayName("입고완료된 수량은 해제되지 않는다(발주중만 해제)")
        void 입고완료분은_해제안됨() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            n.applyOrder("A-001", 100);
            n.applyFulfillment("A-001", 60); // ordered 40 남음, fulfilled 60

            int released = n.releaseOrder("A-001", 100); // 발주중 40만 해제 가능

            PurchaseRequestNotificationLine line = onlyLine(n);
            assertEquals(40, released);
            assertEquals(0, line.getOrderedQty());
            assertEquals(60, line.getFulfilledQty());
            assertEquals(40, line.orderableRemaining());
            assertEquals(PurchaseRequestStatus.PARTIAL, n.getStatus());
        }
    }

    @Nested
    @DisplayName("claim — 처리중 선점")
    class Claim {

        private static final long USER_A = 1L;
        private static final long USER_B = 2L;

        @Test
        @DisplayName("미점유 요청은 선점되고 담당자가 기록된다")
        void 미점유_선점() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            LocalDateTime now = LocalDateTime.now();

            n.claim(USER_A, "담당자A", now, now.minusMinutes(30));

            assertEquals(USER_A, n.getClaimedBy());
            assertEquals("담당자A", n.getClaimedByName());
            assertEquals(now, n.getClaimedAt());
        }

        @Test
        @DisplayName("다른 담당자가 유효하게 처리중이면 선점 거부(REQUEST_ALREADY_CLAIMED)")
        void 타인_점유중이면_거부() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            LocalDateTime now = LocalDateTime.now();
            n.claim(USER_A, "담당자A", now, now.minusMinutes(30));

            assertThrows(ApiException.class,
                    () -> n.claim(USER_B, "담당자B", now, now.minusMinutes(30)));
            assertEquals(USER_A, n.getClaimedBy()); // 그대로 유지
        }

        @Test
        @DisplayName("같은 담당자는 재선점(갱신) 가능")
        void 본인_재선점() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            LocalDateTime t1 = LocalDateTime.now().minusMinutes(5);
            n.claim(USER_A, "담당자A", t1, t1.minusMinutes(30));

            LocalDateTime t2 = LocalDateTime.now();
            n.claim(USER_A, "담당자A", t2, t2.minusMinutes(30));

            assertEquals(USER_A, n.getClaimedBy());
            assertEquals(t2, n.getClaimedAt());
        }

        @Test
        @DisplayName("만료된(stale) 클레임은 다른 담당자가 takeover 가능")
        void 만료_클레임_takeover() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            LocalDateTime old = LocalDateTime.now().minusMinutes(40);
            n.claim(USER_A, "담당자A", old, old.minusMinutes(30));

            LocalDateTime now = LocalDateTime.now();
            n.claim(USER_B, "담당자B", now, now.minusMinutes(30)); // A의 클레임(40분 전) < 만료기준 → takeover

            assertEquals(USER_B, n.getClaimedBy());
            assertEquals(now, n.getClaimedAt());
        }

        @Test
        @DisplayName("해제는 본인만 가능, 타인 해제는 거부")
        void 해제_권한() {
            PurchaseRequestNotification n = notificationWithLine("A-001", 100);
            LocalDateTime now = LocalDateTime.now();
            n.claim(USER_A, "담당자A", now, now.minusMinutes(30));

            assertThrows(ApiException.class, () -> n.releaseClaim(USER_B));

            n.releaseClaim(USER_A);
            assertNull(n.getClaimedBy());
            assertNull(n.getClaimedByName());
            assertNull(n.getClaimedAt());
        }
    }

    @Test
    @DisplayName("여러 SKU 라인은 sku별로 독립 추적되고, 모든 라인이 DONE일 때만 헤더 DONE")
    void 다중_SKU_독립추적() {
        PurchaseRequestNotification n = PurchaseRequestNotification.create(
                "evt-1", "SO-1", "WH-HQ-001", "{}", LocalDateTime.now(),
                List.of(
                        PurchaseRequestNotificationLine.create("A-001", 100),
                        PurchaseRequestNotificationLine.create("A-002", 50)
                ));

        n.applyOrder("A-001", 100);
        n.applyFulfillment("A-001", 100); // A-001 DONE
        n.applyOrder("A-002", 50);        // A-002 발주중

        assertEquals(PurchaseRequestStatus.DONE, n.getLines().get(0).getStatus());
        assertEquals(PurchaseRequestStatus.PARTIAL, n.getLines().get(1).getStatus());
        assertEquals(PurchaseRequestStatus.PARTIAL, n.getStatus()); // 아직 전부 DONE 아님

        n.applyFulfillment("A-002", 50); // A-002 까지 입고완료
        assertEquals(PurchaseRequestStatus.DONE, n.getStatus());
    }
}
