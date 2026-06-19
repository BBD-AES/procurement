package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrderChangeType;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderHistory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public record PurchaseOrderHistoryResponse(
        PurchaseOrderChangeType changeType,
        Long changedBy,
        LocalDateTime changedAt,
        JsonNode before,
        JsonNode after
) {
    public static PurchaseOrderHistoryResponse from(PurchaseOrderHistory history, ObjectMapper objectMapper) {
        return new PurchaseOrderHistoryResponse(
                history.getChangeType(),
                history.getChangedBy(),
                history.getChangedAt(),
                history.getBeforePayload() == null
                ? null
                        : objectMapper.readTree(history.getBeforePayload()),
                objectMapper.readTree(history.getAfterPayload())
        );
    }
}
