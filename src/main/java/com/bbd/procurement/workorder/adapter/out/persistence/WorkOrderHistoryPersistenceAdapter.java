package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.adapter.out.persistence.repository.WorkOrderHistoryJpaRepository;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderHistoryPort;
import com.bbd.procurement.workorder.domain.WorkOrderHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkOrderHistoryPersistenceAdapter implements SaveWorkOrderHistoryPort {

    private final WorkOrderHistoryJpaRepository workOrderHistoryJpaRepository;

    @Override
    public void save(WorkOrderHistory history) {
        workOrderHistoryJpaRepository.save(history);
    }
}
