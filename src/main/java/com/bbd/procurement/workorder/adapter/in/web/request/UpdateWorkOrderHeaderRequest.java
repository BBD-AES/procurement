package com.bbd.procurement.workorder.adapter.in.web.request;

import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderHeaderCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkOrderHeaderRequest(

        @NotBlank(message = "warehouseCode는 필수입니다.")
        @Size(max = 20)
        String warehouseCode,

        // soNumber는 선택 — 비우면 기존 값 유지(도메인 updateHeader가 hasText일 때만 갱신).
        @Size(max = 30)
        String soNumber
) {
    public UpdateWorkOrderHeaderCommand toCommand(String workOrderNumber, Long updatedBy) {
        return new UpdateWorkOrderHeaderCommand(workOrderNumber, warehouseCode, soNumber, updatedBy);
    }
}
