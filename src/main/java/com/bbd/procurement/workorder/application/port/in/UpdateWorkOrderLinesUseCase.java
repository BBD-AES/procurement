package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderLinesCommand;
import com.bbd.procurement.workorder.domain.WorkOrder;

public interface UpdateWorkOrderLinesUseCase {

    WorkOrder updateLines(UpdateWorkOrderLinesCommand command);

}
