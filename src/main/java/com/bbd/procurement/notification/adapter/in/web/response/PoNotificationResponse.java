package com.bbd.procurement.notification.adapter.in.web.response;

import com.bbd.procurement.notification.domain.PoNotification;

public record PoNotificationResponse(
        Long id,
        String poNumber,
        String message,
        boolean read,
        String createdAt) {

    public static PoNotificationResponse from(PoNotification n) {
        return new PoNotificationResponse(
                n.getId(),
                n.getPoNumber(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt().toString());
    }
}
