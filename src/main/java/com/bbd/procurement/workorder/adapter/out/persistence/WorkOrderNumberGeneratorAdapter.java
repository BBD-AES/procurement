package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.application.port.out.WorkOrderNumberGeneratorPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WorkOrderNumberGeneratorAdapter implements WorkOrderNumberGeneratorPort {

    private static final String NEXT_VAL_SQL = "SELECT nextval('work_order_number_seq')";
    private static final String NUMBER_FORMAT = "WO-%04d-%06d";
    private final EntityManager entityManager;

    @Override
    public String generate() {
        Number seq = (Number) entityManager
                .createNativeQuery(NEXT_VAL_SQL)
                .getSingleResult();
        int year = LocalDateTime.now().getYear();
        return String.format(NUMBER_FORMAT, year, seq.longValue());
    }
}
