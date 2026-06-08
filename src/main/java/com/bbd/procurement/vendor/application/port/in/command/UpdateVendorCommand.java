package com.bbd.procurement.vendor.application.port.in.command;

public record UpdateVendorCommand(
        String code,
        String name,
        String contact,
        String terms
) {
}
