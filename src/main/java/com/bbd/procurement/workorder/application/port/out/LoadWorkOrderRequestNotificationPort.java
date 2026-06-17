package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;

import java.util.List;

public interface LoadWorkOrderRequestNotificationPort {

    List<WorkOrderRequestNotification> findAllOrderByReceivedAtDesc();

    List<WorkOrderRequestNotification> findPendingBySoNumber(String soNumber);

}
