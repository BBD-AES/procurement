package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface GetPurchaseOrderQuery {

    PurchaseOrder getByPoNumber(String poNumber);

}
