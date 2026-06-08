package com.bbd.procurement.vendor.application.port.in.command;

public record ChangeVendorActivationCommand(
        String code,
        boolean active
) {
}
