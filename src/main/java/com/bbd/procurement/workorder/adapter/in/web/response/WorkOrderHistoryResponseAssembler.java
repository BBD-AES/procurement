package com.bbd.procurement.workorder.adapter.in.web.response;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.workorder.domain.WorkOrderHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class WorkOrderHistoryResponseAssembler {

    private final ObjectMapper objectMapper;

    public WorkOrderHistoryResponse toHistoryResponse(WorkOrderHistory history) {
        try {
            JsonNode before = history.getBeforePayload() == null
                    ? null
                    : objectMapper.readTree(history.getBeforePayload());
            JsonNode after = objectMapper.readTree(history.getAfterPayload());
            return new WorkOrderHistoryResponse(
                    history.getChangeType(),
                    history.getChangedBy(),
                    history.getChangedByName(),
                    history.getChangedAt(),
                    before,
                    after
            );
        } catch (JacksonException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
