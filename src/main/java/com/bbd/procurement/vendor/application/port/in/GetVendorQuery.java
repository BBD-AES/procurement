package com.bbd.procurement.vendor.application.port.in;

import com.bbd.procurement.vendor.domain.Vendor;

public interface GetVendorQuery {
    Vendor getByCode(String code);
}
