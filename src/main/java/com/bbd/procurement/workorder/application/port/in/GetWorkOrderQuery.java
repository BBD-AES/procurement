package com.bbd.procurement.workorder.application.port.in;

import com.bbd.procurement.workorder.domain.WorkOrder;

import java.util.List;

public interface GetWorkOrderQuery {

    WorkOrder getByWorkOrderNumber(String workOrderNumber);

    List<WorkOrder> list();

    /** 특정 SO 연계 작업지시 목록(최신순). 생산요청 알림 상세 역조회용. */
    List<WorkOrder> listBySoNumber(String soNumber);
}
