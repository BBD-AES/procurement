package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrder;

import java.util.List;
import java.util.Optional;

public interface LoadWorkOrderPort {

    Optional<WorkOrder> findByWorkOrderNumber(String workOrderNumber);

    List<WorkOrder> findAll();
}
