package com.bbd.procurement.vendor.application.port.in.command;

public record RegisterVendorCommand(
        String name,
        String contact,
        String terms,
        String requestId
) {
}
// code는 포함 안 함 -> 등록 시 VendorCodeGeneratorPort가 채번 → Service가 받아서 Vendor.create에 주입