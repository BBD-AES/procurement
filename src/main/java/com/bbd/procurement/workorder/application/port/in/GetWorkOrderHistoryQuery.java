package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.domain.WorkOrderHistory;

import java.util.List;

public interface GetWorkOrderHistoryQuery {

    List<WorkOrderHistory> getHistory(String workOrderNumber);

}
