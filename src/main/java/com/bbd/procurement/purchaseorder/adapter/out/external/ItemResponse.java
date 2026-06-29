package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemResponse(
        String sku,
        @JsonProperty("name") String partName,
        int unitPrice,
        String sourcingType,
        String category,
        String unit,
        int safetyStock,
        boolean active
) {
}

