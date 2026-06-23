package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.workorder.application.port.in.command.CancelWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderHistoryPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.WorkOrderNumberGeneratorPort;
import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderLine;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WO 취소(cancel) 단위 검증 (이슈 #94).
 *
 * - PLANNED / IN_PRODUCTION → CANCELED 전이 시 변경이력 1건 기록
 * - COMPLETED → WORK_ORDER_INVALID_STATE_TRANSITION
 * - 이미 CANCELED 재취소 → 멱등 no-op(예외 없음), 이력 추가 기록 없음((7) boolean-전이 패턴)
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceCancelTest {

    @Mock SaveWorkOrderPort saveWorkOrderPort;
    @Mock LoadWorkOrderPort loadWorkOrderPort;
    @Mock WorkOrderNumberGeneratorPort workOrderNumberGeneratorPort;
    @Mock LoadItemPort loadItemPort;
    @Mock SaveOutboxEventPort saveOutboxEventPort;
    @Spy ObjectMapper objectMapper = JsonMapper.builder().build();
    @Mock LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;
    @Mock SaveWorkOrderHistoryPort saveWorkOrderHistoryPort;

    @InjectMocks WorkOrderService sut;

    private static final String WO_NUMBER = "WO-2026-000001";

    private CancelWorkOrderCommand command() {
        return new CancelWorkOrderCommand(WO_NUMBER, 1L);
    }

    private WorkOrder planned() {
        return WorkOrder.create(WO_NUMBER, "SO-1", "WH-HQ-001",
                List.of(WorkOrderLine.create(1, "SKU-1", "부품1", new BigDecimal("100"), 2, "C", "EA", 0, true, "MAKE")), 1L, null);
    }

    @Test
    @DisplayName("PLANNED 작업지시는 취소되고 CANCELED 이력 1건을 기록한다")
    void PLANNED_취소() {
        WorkOrder wo = planned();
        when(loadWorkOrderPort.findByWorkOrderNumber(WO_NUMBER)).thenReturn(Optional.of(wo));

        WorkOrder result = sut.cancel(command());

        assertThat(result.getStatus()).isEqualTo(WorkOrderStatus.CANCELED);
        verify(saveWorkOrderHistoryPort, times(1)).save(any());
    }

    @Test
    @DisplayName("IN_PRODUCTION 작업지시도 취소되고 CANCELED 이력 1건을 기록한다")
    void IN_PRODUCTION_취소() {
        WorkOrder wo = planned();
        wo.start();
        when(loadWorkOrderPort.findByWorkOrderNumber(WO_NUMBER)).thenReturn(Optional.of(wo));

        WorkOrder result = sut.cancel(command());

        assertThat(result.getStatus()).isEqualTo(WorkOrderStatus.CANCELED);
        verify(saveWorkOrderHistoryPort, times(1)).save(any());
    }

    @Test
    @DisplayName("COMPLETED 작업지시는 취소할 수 없다 (WORK_ORDER_INVALID_STATE_TRANSITION)")
    void COMPLETED_취소불가() {
        WorkOrder wo = planned();
        wo.start();
        wo.markCompleted(1L);
        when(loadWorkOrderPort.findByWorkOrderNumber(WO_NUMBER)).thenReturn(Optional.of(wo));

        assertThatThrownBy(() -> sut.cancel(command()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);

        verify(saveWorkOrderHistoryPort, never()).save(any());
    }

    @Test
    @DisplayName("cancel을 2회 호출해도 실제 전이는 1회뿐이므로 CANCELED 이력은 1건만 저장된다")
    void 재취소_멱등_noop() {
        WorkOrder wo = planned();
        when(loadWorkOrderPort.findByWorkOrderNumber(WO_NUMBER)).thenReturn(Optional.of(wo));

        sut.cancel(command());
        sut.cancel(command());

        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.CANCELED);
        verify(saveWorkOrderHistoryPort, times(1)).save(any());
    }
}
