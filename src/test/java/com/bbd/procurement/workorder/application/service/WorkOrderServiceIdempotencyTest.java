package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.workorder.application.port.in.command.CreateWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.WorkOrderNumberGeneratorPort;
import com.bbd.procurement.workorder.domain.WorkOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WO 생성(create)의 멱등 동작 단위 검증 (PO #79 패턴 대칭 적용).
 *
 * 멱등키(request_id) 부재로 인한 중복 WO → StockInRequested 중복 발행 → 재고 이중입고 방지.
 * 영속/외부 의존은 모두 목으로 대체하므로 DB 없이 로직만 빠르게 검증한다.
 * (라인 없는 커맨드를 사용해 LoadItemPort 경로는 타지 않는다.)
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceIdempotencyTest {

    @Mock SaveWorkOrderPort saveWorkOrderPort;
    @Mock LoadWorkOrderPort loadWorkOrderPort;
    @Mock WorkOrderNumberGeneratorPort workOrderNumberGeneratorPort;
    @Mock LoadItemPort loadItemPort;
    @Mock SaveOutboxEventPort saveOutboxEventPort;
    @Spy ObjectMapper objectMapper = JsonMapper.builder().build();
    @Mock LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;

    @InjectMocks WorkOrderService sut;

    private static final String REQUEST_ID = "11111111-1111-1111-1111-111111111111";

    private CreateWorkOrderCommand command(String requestId) {
        return new CreateWorkOrderCommand(
                "SO-1",          // soNumber
                "WH-HQ-001",     // warehouseCode
                List.of(),       // lines (비어있어 아이템 조회 경로 미사용)
                1L,              // createdBy
                requestId
        );
    }

    @Test
    @DisplayName("동일 requestId면 기존 WO를 반환하고 새로 생성하지 않는다")
    void 동일_requestId면_기존_WO를_반환한다() {
        WorkOrder existing = WorkOrder.create(
                "WO-2026-000010", "SO-1", "WH-HQ-001", List.of(), 1L, REQUEST_ID);
        when(loadWorkOrderPort.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        WorkOrder result = sut.create(command(REQUEST_ID));

        assertThat(result).isSameAs(existing);
        verify(saveWorkOrderPort, never()).save(any());
        verify(workOrderNumberGeneratorPort, never()).generate();
    }

    @Test
    @DisplayName("requestId가 있고 기존 WO가 없으면 새로 생성한다")
    void requestId가_있고_기존_WO가_없으면_새로_생성한다() {
        when(loadWorkOrderPort.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(workOrderNumberGeneratorPort.generate()).thenReturn("WO-2026-000011");
        when(saveWorkOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkOrder result = sut.create(command(REQUEST_ID));

        assertThat(result.getWorkOrderNumber()).isEqualTo("WO-2026-000011");
        assertThat(result.getRequestId()).isEqualTo(REQUEST_ID);
        verify(saveWorkOrderPort, times(1)).save(any(WorkOrder.class));
    }

    @Test
    @DisplayName("requestId가 없으면 사전조회 없이 기존대로 생성한다 (레거시 호환)")
    void requestId가_없으면_사전조회_없이_생성한다() {
        when(workOrderNumberGeneratorPort.generate()).thenReturn("WO-2026-000012");
        when(saveWorkOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkOrder result = sut.create(command(null));

        assertThat(result.getWorkOrderNumber()).isEqualTo("WO-2026-000012");
        verify(loadWorkOrderPort, never()).findByRequestId(anyString());
        verify(saveWorkOrderPort, times(1)).save(any(WorkOrder.class));
    }

    @Test
    @DisplayName("동시 경합으로 UNIQUE 위반 시 409(WORK_ORDER_DUPLICATE_REQUEST)로 응답한다")
    void 동시경합으로_UNIQUE_위반시_409로_응답한다() {
        when(loadWorkOrderPort.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(workOrderNumberGeneratorPort.generate()).thenReturn("WO-2026-000013");
        when(saveWorkOrderPort.save(any()))
                .thenThrow(new DataIntegrityViolationException("uq_work_order_request"));

        assertThatThrownBy(() -> sut.create(command(REQUEST_ID)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.WORK_ORDER_DUPLICATE_REQUEST);
    }
}
