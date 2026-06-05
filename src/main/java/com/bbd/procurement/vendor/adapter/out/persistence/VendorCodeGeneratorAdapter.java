package com.bbd.procurement.vendor.adapter.out.persistence;

import com.bbd.procurement.vendor.application.port.out.VendorCodeGeneratorPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VendorCodeGeneratorAdapter implements VendorCodeGeneratorPort {

    private static final String NEXT_VAL_SQL = "SELECT nextval('vendor_code_seq')";
    private static final String CODE_FORMAT = "V%06d";

    private final EntityManager entityManager;

    @Override
    public String generate() {
        Number seq = (Number) entityManager
                .createNativeQuery(NEXT_VAL_SQL)
                .getSingleResult();

        return String.format(CODE_FORMAT, seq.longValue());
    }

}
