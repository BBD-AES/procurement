package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PurchaseOrderResponseAssembler {

    private final ObjectMapper objectMapper;

    public PurchaseOrderHistoryResponse toHistoryResponse(PurchaseOrderHistory history) {
        try {
            JsonNode before = history.getBeforePayload() == null
                    ? null
                    : objectMapper.readTree(history.getBeforePayload());
            JsonNode after = objectMapper.readTree(history.getAfterPayload());
            return new PurchaseOrderHistoryResponse(
                    history.getChangeType(),
                    history.getChangedBy(),
                    history.getChangedAt(),
                    before,
                    after
            );
        } catch (JacksonException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
