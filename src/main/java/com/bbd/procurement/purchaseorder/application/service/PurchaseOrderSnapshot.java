package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderSnapshot(
        String poNumber,
        String vendorCode,
        String warehouseCode,
        String soNumber,
        String status,
        BigDecimal totalAmount,
        LocalDate expectedArrival,
        String note,
        Long createdBy,
        Long orderedBy,
        LocalDateTime orderedAt,
        Long receivedBy,
        LocalDateTime receivedAt,
        List<LineSnapshot> lines
) {
    public record LineSnapshot(
            int lineOrder,
            String sku,
            String partName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
        static LineSnapshot from(PurchaseOrderLine line) {
            return new LineSnapshot(
                    line.getLineOrder(),
                    line.getSku(),
                    line.getPartName(),
                    line.getUnitPrice(),
                    line.getQuantity(),
                    line.getSubtotal()
            );
        }
    }

    public static PurchaseOrderSnapshot from(PurchaseOrder po) {
        return new PurchaseOrderSnapshot(
                po.getPoNumber(),
                po.getVendorCode(),
                po.getWarehouseCode(),
                po.getSoNumber(),
                po.getStatus().name(),
                po.getTotalAmount(),
                po.getExpectedArrival(),
                po.getNote(),
                po.getCreatedBy(),
                po.getOrderedBy(),
                po.getOrderedAt(),
                po.getReceivedBy(),
                po.getReceivedAt(),
                po.getLines().stream()
                        .map(LineSnapshot::from)
                        .toList()
        );
    }
}
