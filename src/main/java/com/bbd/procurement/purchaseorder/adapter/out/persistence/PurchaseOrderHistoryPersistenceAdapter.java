package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PurchaseOrderHistoryPersistenceAdapter implements SavePurchaseOrderHistoryPort, LoadPurchaseOrderHistoryPort {

    private final PurchaseOrderHistoryJpaRepository purchaseOrderHistoryJpaRepository;

    @Override
    public void save(PurchaseOrderHistory history) {
        purchaseOrderHistoryJpaRepository.save(history);
    }

    @Override
    public List<PurchaseOrderHistory> findByPoNumber(String poNumber) {
        return  purchaseOrderHistoryJpaRepository.findByPoNumberOrderByChangedAtAsc(poNumber);
    }
}
