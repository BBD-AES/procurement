package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPersistenceAdapter implements SavePurchaseOrderPort, LoadPurchaseOrderPort {

    private final PurchaseOrderJpaRepository purchaseOrderJpaRepository;

    @Override
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        return purchaseOrderJpaRepository.save(purchaseOrder);
    }

    @Override
    public Optional<PurchaseOrder> findByPoNumber(String poNumber) {
        return purchaseOrderJpaRepository.findByPoNumber(poNumber);
    }

    @Override
    public Optional<PurchaseOrder> findByRequestId(String requestId) {
        return purchaseOrderJpaRepository.findByRequestId(requestId);
    }

    @Override
    public List<PurchaseOrder> findAll() {
        return purchaseOrderJpaRepository.findAll();
    }

    @Override
    public Map<PurchaseOrderStatus, Long> countByStatus() {
        Map<PurchaseOrderStatus, Long> counts = new EnumMap<>(PurchaseOrderStatus.class);
        purchaseOrderJpaRepository.countGroupByStatus()
                .forEach(row -> counts.put(row.getStatus(), row.getCount()));
        return counts;
    }
}
