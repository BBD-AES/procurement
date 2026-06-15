package com.bbd.procurement.purchaseorder.adapter.in.messaging;

import com.bbd.procurement.purchaseorder.application.port.in.HandlePurchaseRequestedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseRequestedListener {

    private final HandlePurchaseRequestedUseCase handlePurchaseRequestedUseCase;

    @KafkaListener(topics = "sales.purchase-requested", groupId = "procurement-purchase")
    public void onMessage(String message) {
        handlePurchaseRequestedUseCase.handle(message);
    }
}
