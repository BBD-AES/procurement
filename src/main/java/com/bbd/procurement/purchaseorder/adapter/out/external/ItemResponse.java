package com.bbd.procurement.purchaseorder.adapter.out.external;

public record ItemResponse(
        String sku,
        String partName,
        int unitPrice,
        String sourcingType
) {
}
