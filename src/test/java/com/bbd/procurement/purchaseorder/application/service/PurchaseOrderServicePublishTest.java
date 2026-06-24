package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.application.port.in.command.CompletePurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.PurchaseOrderNumberGeneratorPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderPort;
import com.bbd.procurement.vendor.application.port.out.LoadVendorPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import com.bbd.securitycore.application.model.CurrentUserSnapshotResult;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * publishStockInRequested 단위테스트 (이슈 #64).
 *
 * complete(입고완료) 공개 경로를 통해 Outbox 발행을 검증한다.
 *  - StockInRequested 페이로드 조립 → 직렬화 → OutboxEvent.create → saveOutboxEventPort.save
 * 실제 ObjectMapper로 직렬화하고, 저장된 OutboxEvent를 캡처해 메타/페이로드를 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServicePublishTest {

    @Mock SavePurchaseOrderPort savePurchaseOrderPort;
    @Mock LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock PurchaseOrderNumberGeneratorPort purchaseOrderNumberGeneratorPort;
    @Mock SaveOutboxEventPort saveOutboxEventPort;
    @Spy ObjectMapper objectMapper = JsonMapper.builder().build();
    @Mock LoadItemPort loadItemPort;
    @Mock SavePurchaseOrderHistoryPort savePurchaseOrderHistoryPort;
    @Mock LoadPurchaseOrderHistoryPort loadPurchaseOrderHistoryPort;
    @Mock LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;
    @Mock LoadVendorPort loadVendorPort;
    @Mock GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;

    @InjectMocks PurchaseOrderService sut;

    @BeforeEach
    void stubCurrentUser() {
        // 완료(complete) 시 이력 기록의 변경자 이름 스냅샷용.
        CurrentUserSnapshotResult snapshot = mock(CurrentUserSnapshotResult.class);
        lenient().when(snapshot.displayName()).thenReturn("테스터");
        lenient().when(getCurrentUserSnapshotUseCase.getCurrentUserSnapshot()).thenReturn(snapshot);
    }

    private static final String PO_NUMBER = "PO-2026-000001";

    private PurchaseOrder draftPo() {
        PurchaseOrderLine line = PurchaseOrderLine.create(1, "SKU-1", "부품A", new BigDecimal("100"), 2, "카테고리A", "EA", 5, true, "BUY");
        return PurchaseOrder.create(
                PO_NUMBER, "V001", "WH-HQ-001", "SO-1", null, "note", List.of(line), 1L, null);
    }

    @Test
    @DisplayName("complete 시 StockInRequested가 OutboxEvent로 저장되고 메타/페이로드가 구성된다")
    void complete시_outbox에_저장된다() {
        PurchaseOrder po = draftPo();
        when(loadPurchaseOrderPort.findByPoNumber(PO_NUMBER)).thenReturn(Optional.of(po));

        sut.complete(new CompletePurchaseOrderCommand(PO_NUMBER, 99L));

        // 상태 전이도 함께 일어났는지 확인
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);

        // 저장된 OutboxEvent 캡처
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(saveOutboxEventPort, times(1)).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.getTopic()).isEqualTo(StockInRequested.TOPIC);
        assertThat(saved.getEventType()).isEqualTo(StockInRequested.EVENT_TYPE);
        assertThat(saved.getAggregateType()).isEqualTo("PurchaseOrder");
        assertThat(saved.getAggregateId()).isEqualTo(PO_NUMBER);
        assertThat(saved.getEventId()).isNotNull();
        // 페이로드에 라인/번호 정보가 직렬화되어 들어갔는지 확인
        assertThat(saved.getPayload())
                .contains(PO_NUMBER)
                .contains("SKU-1")
                .contains(StockInRequested.EVENT_TYPE);

        // 이력도 함께 저장되었는지
        verify(savePurchaseOrderHistoryPort, times(1)).save(any());
    }
}
