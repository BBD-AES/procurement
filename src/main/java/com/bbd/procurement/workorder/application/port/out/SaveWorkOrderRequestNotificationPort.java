package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;

public interface SaveWorkOrderRequestNotificationPort {

    void save(WorkOrderRequestNotification notification);

}
