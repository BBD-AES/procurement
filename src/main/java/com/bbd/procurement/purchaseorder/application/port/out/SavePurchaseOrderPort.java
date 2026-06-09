package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface SavePurchaseOrderPort {

    PurchaseOrder save(PurchaseOrder purchaseOrder);
}
