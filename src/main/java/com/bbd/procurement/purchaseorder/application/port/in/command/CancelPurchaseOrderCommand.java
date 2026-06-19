package com.bbd.procurement.purchaseorder.application.port.in.command;

public record CancelPurchaseOrderCommand(
        String poNumber,
        Long requesterId
) {
}
