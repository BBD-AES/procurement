package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;

import java.util.List;

public interface GetWorkOrderRequestNotificationQuery {

    List<WorkOrderRequestNotification> list();

}
