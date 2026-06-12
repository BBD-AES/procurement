package com.bbd.procurement.purchaseorder.application.port.in.command;

import java.util.List;

public record UpdatePurchaseOrderLinesCommand(
        String poNumber,
        List<PurchaseOrderLineItem> lines,
        String updatedBy
) {
}
