package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.application.port.in.command.CompleteWorkOrderCommand;
import com.bbd.procurement.workorder.domain.WorkOrder;

public interface CompleteWorkOrderUseCase {

    WorkOrder complete(CompleteWorkOrderCommand command);

}
