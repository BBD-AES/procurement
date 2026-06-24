package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.adapter.out.persistence.repository.WorkOrderHistoryJpaRepository;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderHistoryPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderHistoryPort;
import com.bbd.procurement.workorder.domain.WorkOrderHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkOrderHistoryPersistenceAdapter implements SaveWorkOrderHistoryPort, LoadWorkOrderHistoryPort {

    private final WorkOrderHistoryJpaRepository workOrderHistoryJpaRepository;

    @Override
    public void save(WorkOrderHistory history) {
        workOrderHistoryJpaRepository.save(history);
    }

    @Override
    public List<WorkOrderHistory> findByWorkOrderNumber(String workOrderNumber) {
        return workOrderHistoryJpaRepository.findByWorkOrderNumberOrderByChangedAtAsc(workOrderNumber);
    }
}
