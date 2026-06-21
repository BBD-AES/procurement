package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

import java.util.List;
import java.util.Optional;

public interface LoadPurchaseOrderPort {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    Optional<PurchaseOrder> findByRequestId(String requestId);

    List<PurchaseOrder> findAll();
}
