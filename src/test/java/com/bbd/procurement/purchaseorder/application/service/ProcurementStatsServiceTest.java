package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.application.port.in.result.ProcurementStats;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ProcurementStatsService 단위테스트 (이슈 #90).
 *  - PO/WO 상태별 카운트를 enum 전 상태 0 포함으로 채우는지
 *  - 대기 발주요청/생산요청 수를 그대로 전달하는지
 */
@ExtendWith(MockitoExtension.class)
class ProcurementStatsServiceTest {

    @Mock LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock LoadWorkOrderPort loadWorkOrderPort;
    @Mock LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;
    @Mock LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;

    @InjectMocks ProcurementStatsService sut;

    @Test
    @DisplayName("상태별 카운트를 모든 enum 상태(0 포함)로 채우고 대기 수를 함께 반환한다")
    void getStats_zeroFills_and_returns_pending() {
        when(loadPurchaseOrderPort.countByStatus())
                .thenReturn(Map.of(PurchaseOrderStatus.DRAFT, 3L, PurchaseOrderStatus.RECEIVED, 2L));
        when(loadWorkOrderPort.countByStatus())
                .thenReturn(Map.of(WorkOrderStatus.IN_PRODUCTION, 1L));
        when(loadPurchaseRequestNotificationPort.countPending()).thenReturn(5L);
        when(loadWorkOrderRequestNotificationPort.countPending()).thenReturn(7L);

        ProcurementStats stats = sut.getStats();

        // PO: 누락된 CANCELED 는 0 으로 채워져 모든 상태가 키로 존재
        assertThat(stats.poByStatus())
                .containsAllEntriesOf(Map.of(
                        PurchaseOrderStatus.DRAFT, 3L,
                        PurchaseOrderStatus.RECEIVED, 2L,
                        PurchaseOrderStatus.CANCELED, 0L))
                .hasSize(PurchaseOrderStatus.values().length);

        // WO: 보고되지 않은 PLANNED/COMPLETED/CANCELED 는 0
        assertThat(stats.woByStatus())
                .containsEntry(WorkOrderStatus.IN_PRODUCTION, 1L)
                .containsEntry(WorkOrderStatus.PLANNED, 0L)
                .containsEntry(WorkOrderStatus.COMPLETED, 0L)
                .containsEntry(WorkOrderStatus.CANCELED, 0L)
                .hasSize(WorkOrderStatus.values().length);

        assertThat(stats.pendingPurchaseRequests()).isEqualTo(5L);
        assertThat(stats.pendingWorkOrderRequests()).isEqualTo(7L);
    }
}
