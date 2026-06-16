package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.application.port.in.command.CreateWorkOrderCommand;
import com.bbd.procurement.workorder.domain.WorkOrder;

public interface CreateWorkOrderUseCase {

    WorkOrder create(CreateWorkOrderCommand command);

}
