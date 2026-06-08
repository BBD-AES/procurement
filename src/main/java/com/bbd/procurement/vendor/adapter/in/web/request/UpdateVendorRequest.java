package com.bbd.procurement.vendor.adapter.in.web.request;

import com.bbd.procurement.vendor.application.port.in.command.UpdateVendorCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "공급사 수정 요청")
public record UpdateVendorRequest(
        @NotBlank(message = "공급사명은 필수입니다.")
        @Size(max = 100, message = "공급사명은 100자 이내여야 합니다.")
        String name,

        @Size(max = 100, message = "연락처는 100자 이내여야 합니다.")
        String contact,

        String terms
) {
    public UpdateVendorCommand toCommand(String code) {
        return new UpdateVendorCommand(code, name, contact, terms);
    }
}
