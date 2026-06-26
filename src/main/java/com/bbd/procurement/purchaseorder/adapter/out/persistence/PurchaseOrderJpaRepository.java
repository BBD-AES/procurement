package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface PurchaseOrderJpaRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    Optional<PurchaseOrder> findByRequestId(String requestId);

    /** 특정 SO 연계 PO 목록(최신순). 요청 알림 상세에서 "이 주문으로 뭘 발주했나" 역조회용. */
    List<PurchaseOrder> findBySoNumberOrderByCreatedAtDesc(String soNumber);

    @Query("select po.status as status, count(po) as count from PurchaseOrder po group by po.status")
    List<StatusCount<PurchaseOrderStatus>> countGroupByStatus();

}
