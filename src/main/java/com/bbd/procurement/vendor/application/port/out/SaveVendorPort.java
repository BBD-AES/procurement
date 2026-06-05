package com.bbd.procurement.vendor.application.port.out;

import com.bbd.procurement.vendor.domain.Vendor;

public interface SaveVendorPort {
    Vendor save(Vendor vendor);
}
