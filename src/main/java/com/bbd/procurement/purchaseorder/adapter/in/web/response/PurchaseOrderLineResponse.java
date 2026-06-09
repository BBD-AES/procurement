package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrderLine;

import java.math.BigDecimal;

public record PurchaseOrderLineResponse(
        int lineOrder,
        String sku,
        String partName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
    public static PurchaseOrderLineResponse from(PurchaseOrderLine line) {
        return new PurchaseOrderLineResponse(
                line.getLineOrder(),
                line.getSku(),
                line.getPartName(),
                line.getUnitPrice(),
                line.getQuantity(),
                line.getSubtotal()
        );
    }
}
