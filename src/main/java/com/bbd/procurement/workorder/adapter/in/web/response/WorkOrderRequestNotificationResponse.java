package com.bbd.procurement.workorder.adapter.in.web.response;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import com.bbd.procurement.workorder.domain.WorkOrderRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderRequestNotificationResponse(
        String eventId,
        String soNumber,
        String warehouseCode,
        WorkOrderRequestStatus status,
        LocalDateTime receivedAt,
        Long claimedBy,          // 처리중 담당자 userId (null=미점유)
        String claimedByName,    // 처리중 담당자 표시명
        LocalDateTime claimedAt, // 클레임 시각
        List<LineResponse> lines
) {
    public record LineResponse(
            String sku,
            int requestedQty,        // 요청 원수량
            int orderedQty,          // 생산중(작업지시됐으나 미완료)
            int fulfilledQty,        // 생산완료
            int remainingQty,        // 미완료(생산중 + 미지시) = requested - fulfilled
            int remainingToOrderQty, // 미지시 잔여(아직 작업지시해야 할 양) = requested - ordered - fulfilled
            WorkOrderRequestStatus status
    ) {
    }

    public static WorkOrderRequestNotificationResponse from(WorkOrderRequestNotification notification) {
        List<LineResponse> lines = notification.getLines().stream()
                .map(line -> new LineResponse(
                        line.getSku(),
                        line.getRequestedQty(),
                        line.getOrderedQty(),
                        line.getFulfilledQty(),
                        line.remaining(),
                        line.orderableRemaining(),
                        line.getStatus()))
                .toList();

        return new WorkOrderRequestNotificationResponse(
                notification.getEventId(),
                notification.getSoNumber(),
                notification.getWarehouseCode(),
                notification.getStatus(),
                notification.getReceivedAt(),
                notification.getClaimedBy(),
                notification.getClaimedByName(),
                notification.getClaimedAt(),
                lines
        );
    }
}
