package com.bbd.procurement.purchaseorder.application.port.out.result;

import java.util.List;

public record SalesOrderResult(
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
        long totalAmount,
        String note,
        List<Line> lines
) {
    public record Line(
            int lineNo,
            String sku,
            String nameSnapshot,
            int unitPriceSnapshot,
            int quantity
    ){
    }
}
