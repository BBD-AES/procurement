package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrderHistory;

public interface SavePurchaseOrderHistoryPort {

    void save(PurchaseOrderHistory history);

}
