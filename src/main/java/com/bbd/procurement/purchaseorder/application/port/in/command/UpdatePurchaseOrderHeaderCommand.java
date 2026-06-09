package com.bbd.procurement.purchaseorder.application.port.in.command;

import java.time.LocalDate;

public record UpdatePurchaseOrderHeaderCommand(
        String poNumber,
        String vendorCode,
        String warehouseCode,
        String soId,
        LocalDate expectedArrival,
        String note
) {
}
