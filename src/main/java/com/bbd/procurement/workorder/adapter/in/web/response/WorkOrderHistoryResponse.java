package com.bbd.procurement.workorder.adapter.in.web.response;

import com.bbd.procurement.workorder.domain.WorkOrderChangeType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record WorkOrderHistoryResponse(
        WorkOrderChangeType changeType,
        Long changedBy,
        String changedByName,
        LocalDateTime changedAt,
        JsonNode before,
        JsonNode after
) {
}
