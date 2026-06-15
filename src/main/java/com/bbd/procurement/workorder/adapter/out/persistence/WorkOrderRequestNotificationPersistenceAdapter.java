package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
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
    public List<WorkOrderRequestNotification> findAllOrderByReceivedAtDesc() {
        return workOrderRequestNotificationJpaRepository.findAllByOrderByReceivedAtDesc();
    }
}
