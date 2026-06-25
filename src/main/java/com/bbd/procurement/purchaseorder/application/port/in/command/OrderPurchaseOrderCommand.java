package com.bbd.procurement.purchaseorder.application.port.in.command;

public record OrderPurchaseOrderCommand(
        String poNumber,
        Long orderedBy
) {
}
