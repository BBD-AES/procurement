package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.application.port.out.PurchaseOrderNumberGeneratorPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PurchaseOrderNumberGeneratorAdapter implements PurchaseOrderNumberGeneratorPort {

    private static final String NEXT_VAL_SQL =  "SELECT nextval('po_number_seq')";
    private static final String NUMBER_FORMAT = "PO-%04d-%06d";
    private final EntityManager entityManager;

    @Override
    public String generate() {
        Number seq = (Number) entityManager
                .createNativeQuery(NEXT_VAL_SQL)
                .getSingleResult();
        int year = LocalDate.now().getYear();
        return String.format(NUMBER_FORMAT, year, seq.longValue());
    }
}
