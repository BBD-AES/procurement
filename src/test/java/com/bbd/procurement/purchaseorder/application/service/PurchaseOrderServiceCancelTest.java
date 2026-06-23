package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.application.port.in.command.CancelPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.PurchaseOrderNumberGeneratorPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderHistoryPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.vendor.application.port.out.LoadVendorPort;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 취소 멱등 호출 시 변경이력 중복 적재 방지 검증 (이슈 (7)).
 *
 * PurchaseOrder.cancel()이 실제 전이 여부를 boolean으로 반환하고,
 * 서비스는 전이된 경우에만 recordHistory를 수행하므로
 * cancel을 반복 호출해도 CANCELED 이력은 정확히 1건만 쌓인다.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceCancelTest {

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

    @InjectMocks PurchaseOrderService sut;

    private static final String PO_NUMBER = "PO-2026-000001";

    private CancelPurchaseOrderCommand command() {
        return new CancelPurchaseOrderCommand(PO_NUMBER, 1L);
    }

    @Test
    @DisplayName("cancel을 2회 호출해도 실제 전이는 1회뿐이므로 CANCELED 이력은 1건만 저장된다")
    void cancel_2회_호출시_이력은_1회만_저장된다() {
        PurchaseOrder po = PurchaseOrder.create(
                PO_NUMBER, "V001", "WH-HQ-001", null, null, "note", List.of(), 1L, null);
        when(loadPurchaseOrderPort.findByPoNumber(PO_NUMBER)).thenReturn(Optional.of(po));

        sut.cancel(command());
        sut.cancel(command());

        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        verify(savePurchaseOrderHistoryPort, times(1)).save(any());
    }

    @Test
    @DisplayName("최초 cancel은 실제 전이되므로 CANCELED 이력 1건을 기록한다")
    void 최초_cancel은_이력_1건_기록() {
        PurchaseOrder po = PurchaseOrder.create(
                PO_NUMBER, "V001", "WH-HQ-001", null, null, "note", List.of(), 1L, null);
        when(loadPurchaseOrderPort.findByPoNumber(PO_NUMBER)).thenReturn(Optional.of(po));

        sut.cancel(command());

        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        verify(savePurchaseOrderHistoryPort, times(1)).save(any());
    }
}
