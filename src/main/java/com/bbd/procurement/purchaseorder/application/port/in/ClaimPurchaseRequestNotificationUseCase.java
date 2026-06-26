package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;

public interface ClaimPurchaseRequestNotificationUseCase {

    /** eventId 요청을 담당자(userId/userName)가 처리중으로 선점. 다른 담당자가 처리중이면 REQUEST_ALREADY_CLAIMED. */
    PurchaseRequestNotification claim(String eventId, Long userId, String userName);

    /** eventId 요청의 클레임 해제(본인만). */
    PurchaseRequestNotification release(String eventId, Long userId);
}
