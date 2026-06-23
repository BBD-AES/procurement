package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.application.port.in.command.CancelWorkOrderCommand;
import com.bbd.procurement.workorder.domain.WorkOrder;

public interface CancelWorkOrderUseCase {

    WorkOrder cancel(CancelWorkOrderCommand command);

}
