package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestStatus;
import tools.jackson.databind.ObjectMapper;

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
            int quantity
    ) {

    }

    public static PurchaseRequestNotificationResponse from(PurchaseRequestNotification notification, ObjectMapper objectMapper) {
        PurchaseRequested event = objectMapper.readValue(notification.getPayload(), PurchaseRequested.class);
        List<LineResponse> lines = event.lines().stream()
                .map(line -> new LineResponse(line.sku(), line.quantity()))
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
