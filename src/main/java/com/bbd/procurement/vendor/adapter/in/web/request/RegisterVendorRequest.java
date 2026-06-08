package com.bbd.procurement.vendor.adapter.in.web.request;

import com.bbd.procurement.vendor.application.port.in.command.RegisterVendorCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "공급사 등록 요청")
public record RegisterVendorRequest(
        @NotBlank(message = "공급사명은 필수입니다.")
        @Size(max = 100, message = "공급사명은 100자 이내여야 합니다.")
        String name,

        @Size(max = 100, message = "연락처는 100자 이내여야 합니다.")
        String contact,

        String terms
) {
   public RegisterVendorCommand toCommand() {
       return new RegisterVendorCommand(name, contact, terms);
   }
}
