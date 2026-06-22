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
        List<LineResponse> lines
) {
    public record LineResponse(
            String sku,
            int requestedQty,
            int fulfilledQty,
            int remainingQty,
            PurchaseRequestStatus status
    ) {
    }

    public static PurchaseRequestNotificationResponse from(PurchaseRequestNotification notification) {
        List<LineResponse> lines = notification.getLines().stream()
                .map(line -> new LineResponse(
                        line.getSku(),
                        line.getRequestedQty(),
                        line.getFulfilledQty(),
                        line.remaining(),
                        line.getStatus()))
                .toList();

        return new PurchaseRequestNotificationResponse(
                notification.getEventId(),
                notification.getSoNumber(),
                notification.getWarehouseCode(),
                notification.getStatus(),
                notification.getReceivedAt(),
                lines
        );
    }
}
