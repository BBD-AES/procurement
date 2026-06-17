package com.bbd.procurement.workorder.adapter.out.persistence.repository;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import com.bbd.procurement.workorder.domain.WorkOrderRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRequestNotificationJpaRepository extends JpaRepository<WorkOrderRequestNotification, Long> {

    List<WorkOrderRequestNotification> findAllByOrderByReceivedAtDesc();

    List<WorkOrderRequestNotification> findBySoNumberAndStatus(String soNumber, WorkOrderRequestStatus status);

}
