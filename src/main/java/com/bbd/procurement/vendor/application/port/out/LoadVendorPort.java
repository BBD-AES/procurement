package com.bbd.procurement.vendor.application.port.out;

import com.bbd.procurement.vendor.domain.Vendor;

import java.util.Optional;

public interface LoadVendorPort {

    Optional<Vendor> findByCode(String code);

    boolean existsByCode(String code);

}
