package com.bbd.procurement.vendor.application.port.in;

import com.bbd.procurement.vendor.application.port.in.command.UpdateVendorCommand;
import com.bbd.procurement.vendor.domain.Vendor;

public interface UpdateVendorUseCase {
    Vendor update(UpdateVendorCommand command);
}
