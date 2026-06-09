package com.bbd.procurement.purchaseorder.application.port.in.command;

import java.math.BigDecimal;

public record PurchaseOrderLineItem(
        int lineOrder,
        String sku,
        String partName,
        BigDecimal unitPrice,
        int quantity
) {
}
