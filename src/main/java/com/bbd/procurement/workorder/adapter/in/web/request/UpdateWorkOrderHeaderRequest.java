package com.bbd.procurement.workorder.adapter.in.web.request;

import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderHeaderCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkOrderHeaderRequest(

        @NotBlank(message = "warehouseCode는 필수입니다.")
        @Size(max = 20)
        String warehouseCode,

        @NotBlank(message = "soNumber는 필수입니다.")
        @Size(max = 30)
        String soNumber
) {
    public UpdateWorkOrderHeaderCommand toCommand(String workOrderNumber, Long updatedBy) {
        return new UpdateWorkOrderHeaderCommand(workOrderNumber, warehouseCode, soNumber, updatedBy);
    }
}
