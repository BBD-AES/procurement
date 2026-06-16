package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrder;

public interface SaveWorkOrderPort {

    WorkOrder save(WorkOrder workOrder);

}
