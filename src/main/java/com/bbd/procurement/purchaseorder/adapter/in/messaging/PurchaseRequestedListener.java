package com.bbd.procurement.purchaseorder.adapter.in.messaging;

import com.bbd.procurement.purchaseorder.application.service.PurchaseRequestNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseRequestedListener {

    private final PurchaseRequestNotificationService purchaseRequestNotificationService;

    @KafkaListener(topics = "sales.purchase-requested", groupId = "procurement-purchase")
    public void onMessage(String message) {
        purchaseRequestNotificationService.handle(message);
    }
}
