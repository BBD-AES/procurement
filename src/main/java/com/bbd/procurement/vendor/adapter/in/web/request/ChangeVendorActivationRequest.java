package com.bbd.procurement.vendor.adapter.in.web.request;

import com.bbd.procurement.vendor.application.port.in.command.ChangeVendorActivationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "공급사 활성/비활성 전환 요청")
public record ChangeVendorActivationRequest(
        @NotNull(message = "활성 여부는 필수입니다.")
        Boolean active
) {
    public ChangeVendorActivationCommand toCommand(String code) {
        return new ChangeVendorActivationCommand(code, active);
    }
}
