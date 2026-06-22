package com.bbd.procurement.purchaseorder.adapter.in.web.response;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;

import java.util.Map;

/**
 * 발주 대시보드 집계 응답.
 *
 * @param poByStatus               발주(PO) 상태별 카운트(모든 상태 0 포함)
 * @param woByStatus               작업지시(WO) 상태별 카운트(모든 상태 0 포함)
 * @param pendingPurchaseRequests  아직 발주(PO)로 전환되지 않은 대기 발주요청 수
 * @param pendingWorkOrderRequests 아직 작업지시(WO)로 전환되지 않은 대기 생산요청 수
 */
public record PurchaseOrderStatsResponse(
        Map<PurchaseOrderStatus, Long> poByStatus,
        Map<WorkOrderStatus, Long> woByStatus,
        long pendingPurchaseRequests,
        long pendingWorkOrderRequests
) {
}
