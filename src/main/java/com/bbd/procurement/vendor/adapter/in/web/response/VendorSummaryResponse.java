package com.bbd.procurement.vendor.adapter.in.web.response;

import com.bbd.procurement.vendor.domain.Vendor;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공급사 목록 응답 - 요약")
public record VendorSummaryResponse(
        String code,
        String name,
        String contact,
        String terms,
        boolean active
) {
    public static VendorSummaryResponse from(Vendor vendor) {
        return new VendorSummaryResponse(
                vendor.getCode(),
                vendor.getName(),
                vendor.getContact(),
                vendor.getTerms(),
                vendor.isActive()
        );
    }
}
