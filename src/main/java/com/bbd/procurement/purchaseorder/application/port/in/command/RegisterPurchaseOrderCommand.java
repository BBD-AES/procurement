package com.bbd.procurement.purchaseorder.application.port.in.command;

import java.time.LocalDate;
import java.util.List;

public record RegisterPurchaseOrderCommand(
        String vendorCode,
        String warehouseCode,
        String soNumber,
        LocalDate expectedArrival,
        String note,
        List<PurchaseOrderLineItem> lines,
        Long createdBy,
        String requestId
) {
}
