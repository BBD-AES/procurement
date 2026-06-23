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

        String terms,

        @Schema(description = "등록 멱등키(레거시 폴백). 헤더 Idempotency-Key 미전송 시 사용. 클릭당 1개 UUID, 재시도 시 동일 값.")
        @Size(max = 64, message = "requestId는 64자 이내여야 합니다.")
        String requestId
) {
   public RegisterVendorCommand toCommand() {
       return toCommand(null);
   }

   public RegisterVendorCommand toCommand(String idempotencyKey) {
       // 멱등 키: Idempotency-Key 헤더 우선(org 표준·게이트웨이가 강제), 없으면 본문 requestId(레거시 폴백).
       String idemKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : requestId;
       return new RegisterVendorCommand(name, contact, terms, idemKey);
   }
}
