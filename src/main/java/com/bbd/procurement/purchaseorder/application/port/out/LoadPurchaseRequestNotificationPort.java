package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;

import java.util.List;

public interface LoadPurchaseRequestNotificationPort {

    /** 아직 발주(PO)로 완전히 충당되지 않은(PENDING/PARTIAL) 대기 알림만 receivedAt 내림차순으로 조회. */
    List<PurchaseRequestNotification> findActiveOrderByReceivedAtDesc();

    /** 같은 soNumber 의 아직 충당 여지가 있는(PENDING/PARTIAL) 알림을 receivedAt 오름차순(FIFO)으로, 쓰기 락과 함께 조회. */
    List<PurchaseRequestNotification> findActiveBySoNumber(String soNumber);

    /** 아직 발주(PO)로 완전히 충당되지 않은(PENDING/PARTIAL) 대기 발주요청 알림 수. */
    long countPending();
}
