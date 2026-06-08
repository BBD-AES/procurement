package com.bbd.procurement.vendor.application.port.in;

import com.bbd.procurement.vendor.application.port.in.command.ChangeVendorActivationCommand;
import com.bbd.procurement.vendor.domain.Vendor;

public interface ChangeVendorActivationUseCase {
    Vendor changeActivation(ChangeVendorActivationCommand command);
}
