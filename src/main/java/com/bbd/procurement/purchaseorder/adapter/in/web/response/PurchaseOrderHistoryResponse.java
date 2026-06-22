package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrderChangeType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record PurchaseOrderHistoryResponse(
        PurchaseOrderChangeType changeType,
        Long changedBy,
        LocalDateTime changedAt,
        JsonNode before,
        JsonNode after
) {
}
