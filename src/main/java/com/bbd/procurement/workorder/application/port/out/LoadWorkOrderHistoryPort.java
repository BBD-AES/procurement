package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrderHistory;

import java.util.List;

public interface LoadWorkOrderHistoryPort {

    List<WorkOrderHistory> findByWorkOrderNumber(String workOrderNumber);

}
