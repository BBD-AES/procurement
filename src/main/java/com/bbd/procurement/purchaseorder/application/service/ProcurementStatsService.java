package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.application.port.in.GetPurchaseOrderStatsQuery;
import com.bbd.procurement.purchaseorder.application.port.in.result.ProcurementStats;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/**
 * 발주 대시보드 집계 읽기 서비스(이슈 #90).
 * 발주(PO)/작업지시(WO) 상태별 카운트와 대기 요청 수를 한 번에 모은다.
 *
 * <p>"대기" 정의: 발주요청/생산요청 알림 중 아직 발주(PO)/작업지시(WO)로 완전히
 * 충당되지 않은 상태(PENDING + PARTIAL). 완료(DONE)는 제외한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcurementStatsService implements GetPurchaseOrderStatsQuery {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadWorkOrderPort loadWorkOrderPort;
    private final LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;
    private final LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;

    @Override
    public ProcurementStats getStats() {
        return new ProcurementStats(
                zeroFilled(loadPurchaseOrderPort.countByStatus(), PurchaseOrderStatus.class),
                zeroFilled(loadWorkOrderPort.countByStatus(), WorkOrderStatus.class),
                loadPurchaseRequestNotificationPort.countPending(),
                loadWorkOrderRequestNotificationPort.countPending()
        );
    }

    /** enum 의 모든 상태를 키로 보장하고, 누락된 상태는 0으로 채운다. */
    private <S extends Enum<S>> Map<S, Long> zeroFilled(Map<S, Long> counts, Class<S> type) {
        Map<S, Long> result = new EnumMap<>(type);
        for (S status : type.getEnumConstants()) {
            result.put(status, 0L);
        }
        result.putAll(counts);
        return result;
    }
}
