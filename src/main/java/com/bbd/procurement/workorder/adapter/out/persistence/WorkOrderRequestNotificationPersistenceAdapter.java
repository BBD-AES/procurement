package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.adapter.out.persistence.repository.WorkOrderRequestNotificationJpaRepository;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import com.bbd.procurement.workorder.domain.WorkOrderRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkOrderRequestNotificationPersistenceAdapter
        implements SaveWorkOrderRequestNotificationPort,
        LoadWorkOrderRequestNotificationPort {

    private final WorkOrderRequestNotificationJpaRepository workOrderRequestNotificationJpaRepository;

    @Override
    public void save(WorkOrderRequestNotification notification) {
        workOrderRequestNotificationJpaRepository.save(notification);
    }

    @Override
    public List<WorkOrderRequestNotification> findActiveOrderByReceivedAtDesc() {
        return workOrderRequestNotificationJpaRepository.findByStatusInWithLinesOrderByReceivedAtDesc(
                List.of(WorkOrderRequestStatus.PENDING, WorkOrderRequestStatus.PARTIAL));
    }

    @Override
    public List<WorkOrderRequestNotification> findActiveBySoNumber(String soNumber) {
        return workOrderRequestNotificationJpaRepository.findActiveBySoNumberForUpdate(
                soNumber,
                List.of(WorkOrderRequestStatus.PENDING, WorkOrderRequestStatus.PARTIAL));
    }

    @Override
    public long countPending() {
        return workOrderRequestNotificationJpaRepository.countByStatusIn(
                List.of(WorkOrderRequestStatus.PENDING, WorkOrderRequestStatus.PARTIAL));
    }
}
