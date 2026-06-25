package com.bbd.procurement.notification.adapter.out.persistence;

import com.bbd.procurement.notification.application.port.out.LoadPoNotificationPort;
import com.bbd.procurement.notification.application.port.out.SavePoNotificationPort;
import com.bbd.procurement.notification.domain.PoNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PoNotificationPersistenceAdapter implements SavePoNotificationPort, LoadPoNotificationPort {

    private final PoNotificationJpaRepository poNotificationJpaRepository;

    @Override
    public PoNotification save(PoNotification notification) {
        return poNotificationJpaRepository.save(notification);
    }

    @Override
    public List<PoNotification> findTop100ByTargetRoleAndReadFalseOrderByIdDesc(String targetRole) {
        return poNotificationJpaRepository.findTop100ByTargetRoleAndReadFalseOrderByIdDesc(targetRole);
    }

    @Override
    public Optional<PoNotification> findById(Long id) {
        return poNotificationJpaRepository.findById(id);
    }
}
