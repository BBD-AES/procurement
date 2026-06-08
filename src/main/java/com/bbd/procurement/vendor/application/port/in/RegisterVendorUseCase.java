package com.bbd.procurement.vendor.application.port.in;

import com.bbd.procurement.vendor.application.port.in.command.RegisterVendorCommand;
import com.bbd.procurement.vendor.domain.Vendor;

public interface RegisterVendorUseCase {
    Vendor register(RegisterVendorCommand commnad);
}
