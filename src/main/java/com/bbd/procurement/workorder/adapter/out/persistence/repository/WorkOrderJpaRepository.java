package com.bbd.procurement.workorder.adapter.out.persistence.repository;

import com.bbd.procurement.purchaseorder.adapter.out.persistence.StatusCount;
import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WorkOrderJpaRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByWorkOrderNumber(String workOrderNumber);

    Optional<WorkOrder> findByRequestId(String requestId);

    /** 특정 SO 연계 작업지시 목록(최신순). 생산요청 알림 상세에서 "이 주문으로 뭘 지시했나" 역조회용. */
    List<WorkOrder> findBySoNumberOrderByCreatedAtDesc(String soNumber);

    @Query("select wo.status as status, count(wo) as count from WorkOrder wo group by wo.status")
    List<StatusCount<WorkOrderStatus>> countGroupByStatus();

}
