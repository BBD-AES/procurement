package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        String poNumber,
        String vendorCode,
        String warehouseCode,
        String soId,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        LocalDate expectedArrival,
        String note,
        String createdBy,
        String receivedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime receivedAt,
        List<PurchaseOrderLineResponse> lines
) {
    public static PurchaseOrderResponse from(PurchaseOrder po) {
        return new PurchaseOrderResponse(
                po.getPoNumber(),
                po.getVendorCode(),
                po.getWarehouseCode(),
                po.getSoId(),
                po.getStatus(),
                po.getTotalAmount(),
                po.getExpectedArrival(),
                po.getNote(),
                po.getCreatedBy(),
                po.getReceivedBy(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getReceivedAt(),
                po.getLines().stream()
                        .map(PurchaseOrderLineResponse::from)
                        .toList()
        );
    }
}
