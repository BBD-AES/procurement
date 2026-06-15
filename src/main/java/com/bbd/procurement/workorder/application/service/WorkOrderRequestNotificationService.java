package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.workorder.application.port.in.GetWorkOrderRequestNotificationQuery;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderRequestNotificationService implements GetWorkOrderRequestNotificationQuery {

    private final LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderRequestNotification> list() {
        return loadWorkOrderRequestNotificationPort.findAllOrderByReceivedAtDesc();
    }
}
