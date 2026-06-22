package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;

import java.util.List;

public interface LoadPurchaseRequestNotificationPort {

    List<PurchaseRequestNotification> findAllOrderByReceivedAtDesc();

    /** 같은 soNumber 의 아직 충당 여지가 있는(PENDING/PARTIAL) 알림을 receivedAt 오름차순(FIFO)으로, 쓰기 락과 함께 조회. */
    List<PurchaseRequestNotification> findActiveBySoNumber(String soNumber);
}
