package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.domain.WorkOrder;

public interface StartWorkOrderUseCase {

    WorkOrder start(String workOrderNumber);

}
