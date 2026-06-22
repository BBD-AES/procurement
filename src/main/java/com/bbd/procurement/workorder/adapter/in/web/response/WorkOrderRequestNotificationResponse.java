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
        List<LineResponse> lines
) {
    public record LineResponse(
            String sku,
            int requestedQty,
            int fulfilledQty,
            int remainingQty,
            WorkOrderRequestStatus status
    ) {
    }

    public static WorkOrderRequestNotificationResponse from(WorkOrderRequestNotification notification) {
        List<LineResponse> lines = notification.getLines().stream()
                .map(line -> new LineResponse(
                        line.getSku(),
                        line.getRequestedQty(),
                        line.getFulfilledQty(),
                        line.remaining(),
                        line.getStatus()))
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
