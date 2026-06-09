package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

import java.util.List;

public interface ListPurchaseOrderQuery {

    List<PurchaseOrder> list();

}
