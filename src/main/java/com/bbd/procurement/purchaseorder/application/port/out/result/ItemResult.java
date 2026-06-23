package com.bbd.procurement.purchaseorder.application.port.out.result;

public record ItemResult(
        String sku,
        String partName,
        int unitPrice,
        String sourcingType,
        String category,
        String unit,
        int safetyStock,
        boolean active
) {
}
