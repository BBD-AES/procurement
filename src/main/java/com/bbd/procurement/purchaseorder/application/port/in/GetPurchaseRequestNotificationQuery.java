package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;

import java.util.List;

public interface GetPurchaseRequestNotificationQuery {

    List<PurchaseRequestNotification> list();

}
