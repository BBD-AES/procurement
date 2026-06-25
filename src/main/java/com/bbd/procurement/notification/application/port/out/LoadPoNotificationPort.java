package com.bbd.procurement.notification.application.port.out;

import com.bbd.procurement.notification.domain.PoNotification;

import java.util.List;
import java.util.Optional;

public interface LoadPoNotificationPort {
    List<PoNotification> findTop100ByTargetRoleAndReadFalseOrderByIdDesc(String targetRole);
    Optional<PoNotification> findById(Long id);
}
