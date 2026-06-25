package com.bbd.procurement.notification.application.port.in;

/** 알림 읽음 처리(멱등 — 없는 id는 no-op). */
public interface MarkPoNotificationReadUseCase {
    void markRead(Long id);
}
