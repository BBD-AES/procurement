package com.bbd.procurement.workorder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 생산요청 알림 라인의 4단계 수량 추적 도메인 단위테스트(구매요청과 대칭).
 * 작업지시 생성=생산중(orderedQty), 완료=생산완료(fulfilledQty), 취소=생산중 해제.
 */
class WorkOrderRequestNotificationTest {

    private static WorkOrderRequestNotification withLine(String sku, int requestedQty) {
        return WorkOrderRequestNotification.create(
                "evt-1", "SO-1", "WH-HQ-001", "{}", LocalDateTime.now(),
                List.of(WorkOrderRequestNotificationLine.create(sku, requestedQty)));
    }

    @Test
    @DisplayName("작업지시→완료 시 생산중이 생산완료로 이동하고, 잔여만큼은 미지시로 남는다")
    void 작업지시_완료_이동() {
        WorkOrderRequestNotification n = withLine("A-001", 100);

        n.applyOrder("A-001", 60);
        WorkOrderRequestNotificationLine line = n.getLines().get(0);
        assertEquals(60, line.getOrderedQty());
        assertEquals(40, line.orderableRemaining());
        assertEquals(WorkOrderRequestStatus.PARTIAL, n.getStatus());

        n.applyFulfillment("A-001", 60);
        assertEquals(60, line.getFulfilledQty());
        assertEquals(0, line.getOrderedQty());
        assertEquals(WorkOrderRequestStatus.PARTIAL, n.getStatus());
    }

    @Test
    @DisplayName("작업지시 취소 시 생산중이 해제되어 미지시로 복귀하고 PENDING")
    void 작업지시_취소_해제() {
        WorkOrderRequestNotification n = withLine("A-001", 100);
        n.applyOrder("A-001", 100);

        int released = n.releaseOrder("A-001", 100);

        WorkOrderRequestNotificationLine line = n.getLines().get(0);
        assertEquals(100, released);
        assertEquals(0, line.getOrderedQty());
        assertEquals(100, line.orderableRemaining());
        assertEquals(WorkOrderRequestStatus.PENDING, n.getStatus());
    }
}
