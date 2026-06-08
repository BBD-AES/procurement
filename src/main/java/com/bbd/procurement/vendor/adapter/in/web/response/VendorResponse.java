package com.bbd.procurement.vendor.adapter.in.web.response;

import com.bbd.procurement.vendor.domain.Vendor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공급사 상세 응답")
public record VendorResponse(
        String code,
        String name,
        String contact,
        String terms,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getCode(),
                vendor.getName(),
                vendor.getContact(),
                vendor.getTerms(),
                vendor.isActive(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt()
        );
    }
}
