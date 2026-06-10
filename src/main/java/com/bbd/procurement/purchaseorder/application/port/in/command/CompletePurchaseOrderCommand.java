package com.bbd.procurement.purchaseorder.application.port.in.command;

public record CompletePurchaseOrderCommand(
        String poNumber,
        String receivedBy
) {
}
