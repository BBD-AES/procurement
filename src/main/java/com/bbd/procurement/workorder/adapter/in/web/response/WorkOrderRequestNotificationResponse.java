package com.bbd.procurement.workorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import com.bbd.procurement.workorder.domain.WorkOrderRequestStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderRequestNotificationResponse(
        String eventId,
        String soNumber,
        String warehouseCode,
        WorkOrderRequestStatus status,
        LocalDateTime receivedAt,
        List<LineResponse> lines
) {
    public record LineResponse(
            String sku,
            int quantity
    ) {

    }

    public static WorkOrderRequestNotificationResponse from(WorkOrderRequestNotification notification,
                                                            ObjectMapper objectMapper) {
        PurchaseRequested event = objectMapper.readValue(notification.getPayload(), PurchaseRequested.class);

        List<LineResponse> lines = event.lines().stream()
                .map(line -> new LineResponse(line.sku(), line.quantity()))
                .toList();

        return new WorkOrderRequestNotificationResponse(
                notification.getEventId(),
                notification.getSoNumber(),
                notification.getWarehouseCode(),
                notification.getStatus(),
                notification.getReceivedAt(),
                lines
        );
    }
}
