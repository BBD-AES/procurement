package com.bbd.procurement.notification.adapter.out.persistence;

import com.bbd.procurement.notification.domain.PoNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PoNotificationJpaRepository extends JpaRepository<PoNotification, Long> {
    List<PoNotification> findTop100ByTargetRoleAndReadFalseOrderByIdDesc(String targetRole);
}
