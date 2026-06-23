package com.bbd.procurement.purchaseorder.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockInRequested(
        UUID eventId,
        String source,
        String eventType,
        Instant occurredAt,
        String poNumber,
        String soNumber,
        List<Line> lines
) {
    public static final String SOURCE = "procurement";
    public static final String EVENT_TYPE = "STOCK_IN_REQUESTED";
    public static final String TOPIC = "procurement.stock-in-requested";

    public record Line(
            String sku,
            String name,
            String category,
            String unit,
            int safetyStock,
            int quantity,
            String warehouseCode,
            int unitPrice,
            boolean active,
            String sourcingType
    ) {
    }

    public static StockInRequested of (UUID eventId,
                                       Instant occurredAt,
                                       String poNumber,
                                       String soNumber,
                                       List<Line> lines) {
        return new StockInRequested(
                eventId,
                SOURCE,
                EVENT_TYPE,
                occurredAt,
                poNumber,
                soNumber,
                lines
        );
    }
}
