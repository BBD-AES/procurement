package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;

import java.util.List;

public interface LoadPurchaseRequestNotificationPort {

    List<PurchaseRequestNotification> findAllOrderByReceivedAtDesc();

    List<PurchaseRequestNotification> findPendingBySoNumber(String soNumber);

}
