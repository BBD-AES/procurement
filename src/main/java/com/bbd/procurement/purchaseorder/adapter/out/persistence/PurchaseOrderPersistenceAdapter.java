package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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
}
