package com.bbd.procurement.workorder.adapter.out.persistence;

import com.bbd.procurement.workorder.adapter.out.persistence.repository.WorkOrderJpaRepository;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderPort;
import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    public Optional<WorkOrder> findByRequestId(String requestId) {
        return workOrderJpaRepository.findByRequestId(requestId);
    }

    @Override
    public List<WorkOrder> findAll() {
        return workOrderJpaRepository.findAll();
    }

    @Override
    public List<WorkOrder> findBySoNumber(String soNumber) {
        return workOrderJpaRepository.findBySoNumberOrderByCreatedAtDesc(soNumber);
    }

    @Override
    public Map<WorkOrderStatus, Long> countByStatus() {
        Map<WorkOrderStatus, Long> counts = new EnumMap<>(WorkOrderStatus.class);
        workOrderJpaRepository.countGroupByStatus()
                .forEach(row -> counts.put(row.getStatus(), row.getCount()));
        return counts;
    }

}
