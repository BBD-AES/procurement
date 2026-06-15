package com.bbd.procurement.purchaseorder.application.port.out.result;

public record ItemResult(
        String sku,
        String partName,
        int unitPrice,
        String sourcingType
) {
}
