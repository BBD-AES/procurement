package com.bbd.procurement.workorder.application.port.in.command;

public record WorkOrderLineItem(
        int lineOrder,
        String sku,
        int quantity
) {
}
