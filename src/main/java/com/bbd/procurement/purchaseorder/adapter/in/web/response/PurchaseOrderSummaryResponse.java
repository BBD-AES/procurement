package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseOrderSummaryResponse(
        String poNumber,
        String vendorCode,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        LocalDate expectedArrival,
        LocalDateTime createdAt
) {
    public static PurchaseOrderSummaryResponse from(PurchaseOrder po) {
        return new PurchaseOrderSummaryResponse(
                po.getPoNumber(),
                po.getVendorCode(),
                po.getStatus(),
                po.getTotalAmount(),
                po.getExpectedArrival(),
                po.getCreatedAt()
        );
    }
}
