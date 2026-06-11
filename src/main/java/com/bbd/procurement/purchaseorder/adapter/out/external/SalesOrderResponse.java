package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SalesOrderResponse(
        @JsonProperty("so_number") String soNumber,
        @JsonProperty("from_warehouse_code") String fromWarehouseCode,
        @JsonProperty("from_warehouse_name") String fromWarehouseName,
        @JsonProperty("to_wareohouse_code") String toWarehouseCode,
        @JsonProperty("to_warehouse_name") String toWarehouseName,
        String status,
        String priority,
        @JsonProperty("requested_by") String requestedBy,
        @JsonProperty("approved_by") String approvedBy,
        @JsonProperty("received_by") String receivedBy,
        @JsonProperty("canceled_by") String canceledBy,
        @JsonProperty("requested_at") String requestedAt,
        @JsonProperty("approved_at") String approvedAt,
        @JsonProperty("canceled_at") String canceledAt,
        @JsonProperty("received_at") String receivedAt,
        @JsonProperty("total_amount") String totalAmount,
        String note,
        List<Line> lines
) {
    public record Line(
            @JsonProperty("line_no") int lineNo,
            String sku,
            @JsonProperty("name_snapshot") String nameSnapshot,
            @JsonProperty("unit_price_snapshot") int unitPriceSnapshot,
            int quantity
    ) {
    }
}
