package com.bbd.procurement.workorder.application.port.in.command;

public record UpdateWorkOrderHeaderCommand(
        String workOrderNumber,
        String warehouseCode,
        String soNumber,
        Long updatedBy
) {
}
