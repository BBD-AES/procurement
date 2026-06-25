package com.bbd.procurement.notification.application.port.in;

import com.bbd.procurement.notification.domain.PoNotification;

import java.util.List;

/** 매니저 인박스 조회(미읽음 최신순). */
public interface GetPoNotificationQuery {
    List<PoNotification> listUnreadForManager();
}
