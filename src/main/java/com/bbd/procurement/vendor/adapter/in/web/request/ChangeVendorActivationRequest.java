package com.bbd.procurement.vendor.adapter.in.web.request;

import com.bbd.procurement.vendor.application.port.in.command.ChangeVendorActivationCommand;
import jakarta.validation.constraints.NotNull;

public record ChangeVendorActivationRequest(
        @NotNull(message = "활성 여부는 필수입니다.")
        Boolean active
) {
    public ChangeVendorActivationCommand toCommand(String code) {
        return new ChangeVendorActivationCommand(code, active);
    }
}
