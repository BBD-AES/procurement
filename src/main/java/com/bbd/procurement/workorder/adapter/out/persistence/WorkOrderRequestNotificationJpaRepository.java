package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRequestNotificationJpaRepository extends JpaRepository<WorkOrderRequestNotification, Long> {

    List<WorkOrderRequestNotification> findAllByOrderByReceivedAtDesc();

}
