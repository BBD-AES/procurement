package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.application.port.out.result.SalesOrderResult;

import java.util.List;

public record SalesOrderRelayResponse(
        String soNumber,
        String fromWarehouseCode,
        String fromWarehouseName,
        String toWarehouseCode,
        String toWarehouseName,
        String status,
        String priority,
        String requestedBy,
        String approvedBy,
        String requestedAt,
        String approvedAt,
        long totalAmount,
        String note,
        List<LineResponse> lines
) {
    public record LineResponse(
            int lineNo,
            String sku,
            String nameSnapshot,
            int unitPriceSnapshot,
            int quantity
    ){

    }

    public static SalesOrderRelayResponse from(SalesOrderResult so) {
        return new SalesOrderRelayResponse(
                so.soNumber(),
                so.fromWarehouseCode(),
                so.fromWarehouseName(),
                so.toWarehouseCode(),
                so.toWarehouseName(),
                so.status(),
                so.priority(),
                so.requestedBy(),
                so.approvedBy(),
                so.requestedAt(),
                so.approvedAt(),
                so.totalAmount(),
                so.note(),
                so.lines().stream()
                        .map(line -> new LineResponse(
                                line.lineNo(),
                                line.sku(),
                                line.nameSnapshot(),
                                line.unitPriceSnapshot(),
                                line.quantity()
                        ))
                        .toList()
        );
    }
}
