package com.bbd.procurement.workorder.application.port.in.command;

public record CompleteWorkOrderCommand(
        String workOrderNumber,
        Long completedBy
) {
}
