package com.bbd.procurement.vendor.adapter.out.persistence;

import com.bbd.procurement.vendor.application.port.out.LoadVendorPort;
import com.bbd.procurement.vendor.application.port.out.SaveVendorPort;
import com.bbd.procurement.vendor.domain.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VendorPersistenceAdapter implements SaveVendorPort, LoadVendorPort {

    private final VendorJpaRepository vendorJpaRepository;

    @Override
    public Vendor save(Vendor vendor) {
        return  vendorJpaRepository.save(vendor);
    }

    @Override
    public Optional<Vendor> findByCode(String code) {
        return vendorJpaRepository.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return vendorJpaRepository.existsByCode(code);
    }
}
