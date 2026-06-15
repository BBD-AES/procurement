package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;

public interface SavePurchaseRequestNotificationPort {

    void save(PurchaseRequestNotification notification);

}
