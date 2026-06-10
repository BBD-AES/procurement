package com.bbd.procurement.purchaseorder.application.port.in.command;

public record ConfirmPurchaseOrderCommand(
        String poNumber,
        String confirmedBy
) {
}
