package com.bbd.procurement.vendor.application.port.in;

import com.bbd.procurement.vendor.domain.Vendor;

import java.util.List;

public interface ListVendorQuery {
    List<Vendor> list();
}
