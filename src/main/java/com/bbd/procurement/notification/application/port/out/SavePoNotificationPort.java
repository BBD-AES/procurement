package com.bbd.procurement.notification.application.port.out;

import com.bbd.procurement.notification.domain.PoNotification;

public interface SavePoNotificationPort {
    PoNotification save(PoNotification notification);
}
