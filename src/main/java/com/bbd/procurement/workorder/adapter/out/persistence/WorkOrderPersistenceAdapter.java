package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderPort;
import com.bbd.procurement.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WorkOrderPersistenceAdapter implements SaveWorkOrderPort, LoadWorkOrderPort {

    private final WorkOrderJpaRepository workOrderJpaRepository;

    @Override
    public WorkOrder save(WorkOrder workOrder) {
        return workOrderJpaRepository.save(workOrder);
    }

    @Override
    public Optional<WorkOrder> findByWorkOrderNumber(String workOrderNumber) {
        return workOrderJpaRepository.findByWorkOrderNumber(workOrderNumber);
    }

    @Override
    public List<WorkOrder> findAll() {
        return workOrderJpaRepository.findAll();
    }

}
