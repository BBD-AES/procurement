package com.bbd.procurement.workorder.application.port.in.command;

public record CancelWorkOrderCommand(
        String workOrderNumber,
        Long requesterId
) {
}
