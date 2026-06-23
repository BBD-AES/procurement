package com.bbd.procurement.vendor.adapter.out.persistence;

import com.bbd.procurement.vendor.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface VendorJpaRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByCode(String code);

    Optional<Vendor> findByRequestId(String requestId);
}
