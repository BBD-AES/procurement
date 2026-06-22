package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadPurchaseOrderPort {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    Optional<PurchaseOrder> findByRequestId(String requestId);

    List<PurchaseOrder> findAll();

    /** 발주(PO) 상태별 건수. 건수가 0인 상태는 포함되지 않을 수 있다. */
    Map<PurchaseOrderStatus, Long> countByStatus();
}
