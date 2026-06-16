package com.bbd.procurement.workorder.adapter.in.web.response;

import com.bbd.procurement.workorder.domain.WorkOrderLine;

import java.math.BigDecimal;

public record WorkOrderLineResponse(
        int lineOrder,
        String sku,
        String partName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
    public static WorkOrderLineResponse from(WorkOrderLine line) {
        return new WorkOrderLineResponse(
                line.getLineOrder(), line.getSku(), line.getPartName(),
                line.getUnitPrice(), line.getQuantity(), line.getSubTotal()
        );
    }
}
