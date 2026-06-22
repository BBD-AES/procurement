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
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.vendor.application.port.out.LoadVendorPort;
import com.bbd.procurement.vendor.domain.Vendor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PO 생성 시 공급사(Vendor) 통제 단위 검증.
 *
 * 발주 생성 시점 통제 누락 이슈: 미존재/비활성 공급사로 PO가 생성되던 결함을 차단한다.
 * requestId=null 커맨드를 사용해 멱등 사전조회 경로를 건너뛰고, 공급사 검증 분기만 검증한다.
 * 검증 실패 시 PO 채번·저장 경로에 진입하지 않는 것도 함께 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceVendorValidationTest {

    @Mock SavePurchaseOrderPort savePurchaseOrderPort;
    @Mock LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock PurchaseOrderNumberGeneratorPort purchaseOrderNumberGeneratorPort;
    @Mock SaveOutboxEventPort saveOutboxEventPort;
    @Mock ObjectMapper objectMapper;
    @Mock LoadItemPort loadItemPort;
    @Mock SavePurchaseOrderHistoryPort savePurchaseOrderHistoryPort;
    @Mock LoadPurchaseOrderHistoryPort loadPurchaseOrderHistoryPort;
    @Mock LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;
    @Mock LoadVendorPort loadVendorPort;

    @InjectMocks PurchaseOrderService sut;

    private RegisterPurchaseOrderCommand command() {
        return new RegisterPurchaseOrderCommand(
                "V001",          // vendorCode
                "WH-HQ-001",     // warehouseCode
                null,            // soNumber
                null,            // expectedArrival
                "note",          // note
                List.of(),       // lines
                1L,              // createdBy
                null             // requestId (멱등 사전조회 스킵)
        );
    }

    @Test
    @DisplayName("존재하지 않는 공급사로 생성 시 VENDOR_NOT_FOUND로 차단된다")
    void 미존재_공급사면_VENDOR_NOT_FOUND() {
        when(loadVendorPort.findByCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.register(command()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENDOR_NOT_FOUND);

        verify(purchaseOrderNumberGeneratorPort, never()).generate();
        verify(savePurchaseOrderPort, never()).save(any());
    }

    @Test
    @DisplayName("비활성(거래중지) 공급사로 생성 시 VENDOR_INACTIVE로 차단된다")
    void 비활성_공급사면_VENDOR_INACTIVE() {
        Vendor inactive = Vendor.create("V000001", "거래중지공급사", null, null);
        inactive.deactivate();
        when(loadVendorPort.findByCode(any())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> sut.register(command()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VENDOR_INACTIVE);

        verify(purchaseOrderNumberGeneratorPort, never()).generate();
        verify(savePurchaseOrderPort, never()).save(any());
    }
}
