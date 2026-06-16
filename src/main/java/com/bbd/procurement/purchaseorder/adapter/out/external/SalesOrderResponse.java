package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesOrderResponse(
        String soNumber,
        String fromWarehouseCode,
        String fromWarehouseName,
        String toWarehouseCode,
        String toWarehouseName,
        String status,
        String priority,
        String requestedBy,
        String approvedBy,
        String receivedBy,
        String canceledBy,
        String requestedAt,
        String approvedAt,
        String canceledAt,
        String receivedAt,
        String rejectedReason,
        long totalAmount,
        String note,
        List<Line> lines
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(
            int lineNo,
            String sku,
            String nameSnapshot,
            int unitPriceSnapshot,
            int quantity,
            Integer reservedQuantity,
            String fulfillmentSource,
            String fromWarehouseCode
    ) {
    }
}