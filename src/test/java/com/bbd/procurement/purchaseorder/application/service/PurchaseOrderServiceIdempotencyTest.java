package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.in.command.RegisterPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.PurchaseOrderNumberGeneratorPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderPort;
import com.bbd.procurement.vendor.application.port.out.LoadVendorPort;
import com.bbd.procurement.vendor.domain.Vendor;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.securitycore.application.model.CurrentUserSnapshotResult;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PO 생성(register)의 멱등 동작 단위 검증.
 *
 * 이슈 #79: 멱등키(request_id) 부재로 인한 중복 PO → 재고 이중입고 방지.
 * 영속/외부 의존은 모두 목으로 대체하므로 DB 없이 로직만 빠르게 검증한다.
 * (라인 없는 커맨드를 사용해 LoadItemPort 경로는 타지 않는다.)
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceIdempotencyTest {

    @Mock SavePurchaseOrderPort savePurchaseOrderPort;
    @Mock LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock PurchaseOrderNumberGeneratorPort purchaseOrderNumberGeneratorPort;
    @Mock SaveOutboxEventPort saveOutboxEventPort;
    // 실제 직렬화기를 써서 이력 스냅샷 직렬화가 자연스럽게 동작하도록 한다.
    @org.mockito.Spy ObjectMapper objectMapper = JsonMapper.builder().build();
    @Mock LoadItemPort loadItemPort;
    @Mock SavePurchaseOrderHistoryPort savePurchaseOrderHistoryPort;
    @Mock LoadPurchaseOrderHistoryPort loadPurchaseOrderHistoryPort;
    @Mock LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;
    @Mock LoadVendorPort loadVendorPort;
    @Mock GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;

    @InjectMocks PurchaseOrderService sut;

    @BeforeEach
    void stubCurrentUser() {
        // 이력 기록 시 변경자 이름 스냅샷용. 생성 없이 끝나는(replay/충돌) 테스트도 있어 lenient.
        CurrentUserSnapshotResult snapshot = mock(CurrentUserSnapshotResult.class);
        lenient().when(snapshot.displayName()).thenReturn("테스터");
        lenient().when(getCurrentUserSnapshotUseCase.getCurrentUserSnapshot()).thenReturn(snapshot);
    }

    private static final String REQUEST_ID = "11111111-1111-1111-1111-111111111111";

    private RegisterPurchaseOrderCommand command(String requestId) {
        return new RegisterPurchaseOrderCommand(
                "V001",          // vendorCode
                "WH-HQ-001",     // warehouseCode
                null,            // soNumber (null이면 알림 처리 스킵)
                null,            // expectedArrival
                "note",          // note
                List.of(),       // lines (비어있어 아이템 조회 경로 미사용)
                1L,              // createdBy
                requestId
        );
    }

    // 검증을 통과시키기 위한 활성 공급사 픽스처(코드 형식 ^V\\d{6}$ 충족)
    private Vendor activeVendor() {
        return Vendor.create("V000001", "테스트공급사", null, null);
    }

    @Test
    @DisplayName("동일 requestId면 기존 PO를 반환하고 새로 생성하지 않는다")
    void 동일_requestId면_기존_PO를_반환하고_새로_생성하지_않는다() {
        PurchaseOrder existing = PurchaseOrder.create(
                "PO-2026-000001", "V001", "WH-HQ-001", null, null, "note", List.of(), 1L, REQUEST_ID);
        when(loadPurchaseOrderPort.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        PurchaseOrder result = sut.register(command(REQUEST_ID));

        assertThat(result).isSameAs(existing);
        verify(savePurchaseOrderPort, never()).save(any());
        verify(purchaseOrderNumberGeneratorPort, never()).generate();
        verify(savePurchaseOrderHistoryPort, never()).save(any());
    }

    @Test
    @DisplayName("requestId가 있고 기존 PO가 없으면 새로 생성한다")
    void requestId가_있고_기존_PO가_없으면_새로_생성한다() {
        when(loadVendorPort.findByCode(any())).thenReturn(Optional.of(activeVendor()));
        when(loadPurchaseOrderPort.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(purchaseOrderNumberGeneratorPort.generate()).thenReturn("PO-2026-000002");
        when(savePurchaseOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = sut.register(command(REQUEST_ID));

        assertThat(result.getPoNumber()).isEqualTo("PO-2026-000002");
        assertThat(result.getRequestId()).isEqualTo(REQUEST_ID);
        verify(savePurchaseOrderPort, times(1)).save(any(PurchaseOrder.class));
        verify(savePurchaseOrderHistoryPort, times(1)).save(any());
    }

    @Test
    @DisplayName("requestId가 없으면 사전조회 없이 기존대로 생성한다 (레거시 호환)")
    void requestId가_없으면_사전조회_없이_기존대로_생성한다() {
        when(loadVendorPort.findByCode(any())).thenReturn(Optional.of(activeVendor()));
        when(purchaseOrderNumberGeneratorPort.generate()).thenReturn("PO-2026-000003");
        when(savePurchaseOrderPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = sut.register(command(null));

        assertThat(result.getPoNumber()).isEqualTo("PO-2026-000003");
        verify(loadPurchaseOrderPort, never()).findByRequestId(anyString());
        verify(savePurchaseOrderPort, times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("동시 경합으로 UNIQUE 위반 시 409(PO_DUPLICATE_REQUEST)로 응답한다")
    void 동시경합으로_UNIQUE_위반시_409로_응답한다() {
        when(loadVendorPort.findByCode(any())).thenReturn(Optional.of(activeVendor()));
        when(loadPurchaseOrderPort.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(purchaseOrderNumberGeneratorPort.generate()).thenReturn("PO-2026-000004");
        when(savePurchaseOrderPort.save(any()))
                .thenThrow(new DataIntegrityViolationException("uq_purchase_order_request"));

        assertThatThrownBy(() -> sut.register(command(REQUEST_ID)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.PO_DUPLICATE_REQUEST);
    }
}
