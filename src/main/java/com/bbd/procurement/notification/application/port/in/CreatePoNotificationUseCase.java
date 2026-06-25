package com.bbd.procurement.notification.application.port.in;

/** PO 작성 시 매니저에게 알림 생성. */
public interface CreatePoNotificationUseCase {
    void notifyManagerOfPoCreation(String poNumber, Long createdBy);
}
