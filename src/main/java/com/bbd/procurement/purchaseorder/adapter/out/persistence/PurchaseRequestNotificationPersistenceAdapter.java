package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PurchaseRequestNotificationPersistenceAdapter implements SavePurchaseRequestNotificationPort, LoadPurchaseRequestNotificationPort {

    private final PurchaseRequestNotificationJpaRepository purchaseRequestNotificationJpaRepository;

    @Override
    public void save(PurchaseRequestNotification notification) {
        purchaseRequestNotificationJpaRepository.save(notification);
    }

    @Override
    public List<PurchaseRequestNotification> findAllOrderByReceivedAtDesc() {
        return purchaseRequestNotificationJpaRepository.findAllByOrderByReceivedAtDesc();
    }
}
