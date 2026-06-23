package com.bbd.procurement.workorder.adapter.in.web.request;

import com.bbd.procurement.workorder.application.port.in.command.CreateWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.in.command.WorkOrderLineItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateWorkOrderRequest(
        @NotBlank(message = "soNumber는 필수입니다.")
        @Size(max = 30, message = "soNumber는 30자 이내여야 합니다.")
        String soNumber,

        @NotBlank(message = "warehouseCode는 필수입니다.")
        @Size(max = 20, message = "warehouseCode는 20자 이내여야 합니다.")
        String warehouseCode,

        @Size(max = 64, message = "requestId는 64자 이내여야 합니다.")
        String requestId,

        @Valid
        List<WorkOrderLineItemRequest> lines
) {
    public CreateWorkOrderCommand toCommand(Long createdBy) {
        return toCommand(createdBy, null);
    }

    public CreateWorkOrderCommand toCommand(Long createdBy, String idempotencyKey) {
        List<WorkOrderLineItem> items = lines == null
                ? List.of()
                :
                lines.stream().map(WorkOrderLineItemRequest::toCommandItem).toList();
        // 멱등 키: Idempotency-Key 헤더 우선(org 표준·게이트웨이가 강제), 없으면 본문 requestId(레거시 폴백).
        String idemKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : requestId;
        return new CreateWorkOrderCommand(soNumber, warehouseCode, items, createdBy, idemKey);
    }
}
