package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.domain.WorkOrder;

import java.util.List;

public interface GetWorkOrderQuery {

    WorkOrder getByWorkOrderNumber(String workOrderNumber);

    List<WorkOrder> list();
}
