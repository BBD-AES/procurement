package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderHeaderCommand;
import com.bbd.procurement.workorder.domain.WorkOrder;

public interface UpdateWorkOrderHeaderUseCase {

    WorkOrder updateHeader(UpdateWorkOrderHeaderCommand command);

}
