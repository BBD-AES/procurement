package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrderHistory;

import java.util.List;

public interface LoadPurchaseOrderHistoryPort {

    List<PurchaseOrderHistory> findByPoNumber(String poNumber);

}
