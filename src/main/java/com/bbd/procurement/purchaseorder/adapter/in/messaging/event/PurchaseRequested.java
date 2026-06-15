package com.bbd.procurement.purchaseorder.adapter.in.messaging.event;

import java.util.List;

public record PurchaseRequested(
        String eventId,
        String source,
        String eventType,
        String occurredAt,
        String soNumber,
        String warehouseCode,
        List<Line> lines
) {
    public record Line(
            String sku,
            int quantity
    ) {

    }
}
