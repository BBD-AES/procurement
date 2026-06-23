package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrderHistory;

public interface SaveWorkOrderHistoryPort {

    void save(WorkOrderHistory history);

}
