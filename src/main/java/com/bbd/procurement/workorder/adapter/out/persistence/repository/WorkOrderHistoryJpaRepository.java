package com.bbd.procurement.workorder.adapter.out.persistence.repository;

import com.bbd.procurement.workorder.domain.WorkOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderHistoryJpaRepository extends JpaRepository<WorkOrderHistory, Long> {

    List<WorkOrderHistory> findByWorkOrderNumberOrderByChangedAtAsc(String workOrderNumber);
}
