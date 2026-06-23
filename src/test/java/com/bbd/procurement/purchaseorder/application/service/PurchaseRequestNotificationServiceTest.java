package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.SourcingType;
import com.bbd.procurement.shared.inbox.application.port.out.ProcessedEventPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PurchaseRequestNotificationService 멱등 인박스 단위테스트 (이슈 #64).
 *
 * 인박스 멱등 처리(existsByEventId)가 핵심:
 *  - 이미 처리한 eventId면 알림 저장 없이 즉시 skip
 *  - 새 eventId면 알림 저장 + ProcessedEvent 동시 저장
 * 외부 의존(포트/리졸버)은 모두 목으로 격리한다.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseRequestNotificationServiceTest {

    @Mock ProcessedEventPort processedEventPort;
    @Mock SavePurchaseRequestNotificationPort savePurchaseRequestNotificationPort;
    @Mock LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;
    @Mock SaveWorkOrderRequestNotificationPort saveWorkOrderRequestNotificationPort;
    @Mock SourcingResolver sourcingResolver;
    // 실제 직렬화기를 사용해 readValue/writeValueAsString이 자연스럽게 동작하도록 한다.
    @Spy ObjectMapper objectMapper = JsonMapper.builder().build();

    @InjectMocks PurchaseRequestNotificationService sut;

    private static final String EVENT_ID = "evt-001";

    private PurchaseRequested.Line line;
    private String message;

    @BeforeEach
    void setUp() {
        line = new PurchaseRequested.Line("SKU-1", 5, "BUY");
        PurchaseRequested event = new PurchaseRequested(
                EVENT_ID, "sales", "PURCHASE_REQUESTED", "2026-06-22T00:00:00Z",
                "SO-1", "WH-HQ-001",
                List.of(line)
        );
        message = objectMapper.writeValueAsString(event);
    }

    @Test
    @DisplayName("이미 처리된 eventId면 알림을 저장하지 않고 skip한다")
    void 중복_이벤트면_skip() {
        when(processedEventPort.existsByEventId(EVENT_ID)).thenReturn(true);

        sut.handle(message);

        verify(savePurchaseRequestNotificationPort, never()).save(any());
        verify(saveWorkOrderRequestNotificationPort, never()).save(any());
        verify(processedEventPort, never()).save(anyString());
    }

    @Test
    @DisplayName("새 eventId(BUY 라인)면 구매 알림 저장 + ProcessedEvent를 함께 저장한다")
    void 신규_이벤트면_알림과_처리이력_저장() {
        when(processedEventPort.existsByEventId(EVENT_ID)).thenReturn(false);
        when(sourcingResolver.resolveAll(any()))
                .thenReturn(Map.of(SourcingType.BUY, List.of(line)));

        sut.handle(message);

        verify(savePurchaseRequestNotificationPort, times(1)).save(any(PurchaseRequestNotification.class));
        verify(saveWorkOrderRequestNotificationPort, never()).save(any());
        verify(processedEventPort, times(1)).save(EVENT_ID);
    }

    @Test
    @DisplayName("MAKE 라인이면 작업지시 알림으로 라우팅되고 구매 알림은 저장하지 않는다")
    void MAKE_라인이면_작업지시로_라우팅() {
        when(processedEventPort.existsByEventId(EVENT_ID)).thenReturn(false);
        when(sourcingResolver.resolveAll(any()))
                .thenReturn(Map.of(SourcingType.MAKE, List.of(line)));

        sut.handle(message);

        verify(saveWorkOrderRequestNotificationPort, times(1)).save(any(WorkOrderRequestNotification.class));
        verify(savePurchaseRequestNotificationPort, never()).save(any());
        verify(processedEventPort, times(1)).save(EVENT_ID);
    }
}
