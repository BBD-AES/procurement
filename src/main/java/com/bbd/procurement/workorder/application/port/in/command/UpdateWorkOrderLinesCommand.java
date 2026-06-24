package com.bbd.procurement.workorder.application.port.in.command;

import java.util.List;

public record UpdateWorkOrderLinesCommand(
        String workOrderNumber,
        List<WorkOrderLineItem> lines,
        Long updatedBy
) {
}
