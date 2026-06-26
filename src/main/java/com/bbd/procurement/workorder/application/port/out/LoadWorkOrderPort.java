package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadWorkOrderPort {

    Optional<WorkOrder> findByWorkOrderNumber(String workOrderNumber);

    Optional<WorkOrder> findByRequestId(String requestId);

    List<WorkOrder> findAll();

    /** 특정 SO 연계 작업지시 목록(최신순). 생산요청 알림 상세 역조회용. */
    List<WorkOrder> findBySoNumber(String soNumber);

    /** 작업지시(WO) 상태별 건수. 건수가 0인 상태는 포함되지 않을 수 있다. */
    Map<WorkOrderStatus, Long> countByStatus();
}
