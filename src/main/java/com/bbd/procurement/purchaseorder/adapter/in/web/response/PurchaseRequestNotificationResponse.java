package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PurchaseRequestNotificationResponse(
        String eventId,
        String soNumber,
        String warehouseCode,
        PurchaseRequestStatus status,
        LocalDateTime receivedAt,
        Long claimedBy,          // 처리중 담당자 userId (null=미점유)
        String claimedByName,    // 처리중 담당자 표시명
        LocalDateTime claimedAt, // 클레임 시각
        List<LineResponse> lines
) {
    public record LineResponse(
            String sku,
            int requestedQty,        // 요청 원수량
            int orderedQty,          // 발주중(발주됐으나 미입고)
            int fulfilledQty,        // 입고완료
            int remainingQty,        // 미입고(발주중 + 미발주) = requested - fulfilled
            int remainingToOrderQty, // 미발주 잔여(아직 발주해야 할 양) = requested - ordered - fulfilled
            PurchaseRequestStatus status
    ) {
    }

    public static PurchaseRequestNotificationResponse from(PurchaseRequestNotification notification) {
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

        return new PurchaseRequestNotificationResponse(
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
