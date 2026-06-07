package com.bbd.procurement.vendor.adapter.in.web.response;

import com.bbd.procurement.vendor.domain.Vendor;

public record VendorSummaryResponse(
        String code,
        String name,
        boolean active
) {
    public static VendorSummaryResponse from(Vendor vendor) {
        return new VendorSummaryResponse(
                vendor.getCode(),
                vendor.getName(),
                vendor.isActive()
        );
    }
}
