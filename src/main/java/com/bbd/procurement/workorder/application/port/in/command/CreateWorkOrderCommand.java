package com.bbd.procurement.workorder.application.port.in.command;

import java.util.List;

public record CreateWorkOrderCommand(
        String soNumber,
        String warehouseCode,
        List<WorkOrderLineItem> lines,
        String createdBy
) {
}
