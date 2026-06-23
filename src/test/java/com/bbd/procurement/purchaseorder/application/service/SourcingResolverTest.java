package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.SourcingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SourcingResolver 배치 판정 단위테스트 (이슈 #63 N+1 재발 방지).
 *
 * - hint 없는 N개 라인 → Item 다건 조회(findBySkus) 1회 / 단건(findBySku) 0회
 * - 라우팅(BUY/MAKE) 결과는 단건 호출 시절과 불변
 * - sku 미존재는 ITEM_NOT_FOUND로 실패, sourcingType 불명은 BUY로 degrade(구분 처리)
 */
@ExtendWith(MockitoExtension.class)
class SourcingResolverTest {

    @Mock LoadItemPort loadItemPort;

    @InjectMocks SourcingResolver sut;

    private static PurchaseRequested.Line line(String sku, String hint) {
        return new PurchaseRequested.Line(sku, 1, hint);
    }

    @Test
    @DisplayName("hint 없는 N개 라인이면 findBySkus 1회만 호출하고 findBySku는 호출하지 않는다")
    void hint없는_N라인이면_다건조회_1회() {
        List<PurchaseRequested.Line> lines = List.of(
                line("SKU-1", null), line("SKU-2", null), line("SKU-3", null));
        when(loadItemPort.findBySkus(anyList())).thenReturn(List.of(
                new ItemResult("SKU-1", "p1", 100, "BUY", "CAT", "EA", 0, true),
                new ItemResult("SKU-2", "p2", 100, "MAKE", "CAT", "EA", 0, true),
                new ItemResult("SKU-3", "p3", 100, "BUY", "CAT", "EA", 0, true)));

        Map<SourcingType, List<PurchaseRequested.Line>> result = sut.resolveAll(lines);

        verify(loadItemPort, times(1)).findBySkus(anyList());
        verify(loadItemPort, never()).findBySku(anyString());
        assertThat(result.get(SourcingType.BUY)).extracting(PurchaseRequested.Line::sku)
                .containsExactlyInAnyOrder("SKU-1", "SKU-3");
        assertThat(result.get(SourcingType.MAKE)).extracting(PurchaseRequested.Line::sku)
                .containsExactly("SKU-2");
    }

    @Test
    @DisplayName("hint가 있는 라인은 Item 조회 없이 hint로 판정한다")
    void hint있으면_아이템조회_없음() {
        List<PurchaseRequested.Line> lines = List.of(line("SKU-1", "BUY"), line("SKU-2", "MAKE"));

        Map<SourcingType, List<PurchaseRequested.Line>> result = sut.resolveAll(lines);

        verify(loadItemPort, never()).findBySkus(anyList());
        verify(loadItemPort, never()).findBySku(anyString());
        assertThat(result.get(SourcingType.BUY)).extracting(PurchaseRequested.Line::sku).containsExactly("SKU-1");
        assertThat(result.get(SourcingType.MAKE)).extracting(PurchaseRequested.Line::sku).containsExactly("SKU-2");
    }

    @Test
    @DisplayName("hint 있는 라인과 없는 라인이 섞이면 hint 없는 sku만 모아 다건 조회한다")
    void 혼합이면_불명sku만_다건조회() {
        List<PurchaseRequested.Line> lines = List.of(line("SKU-1", "BUY"), line("SKU-2", null));
        when(loadItemPort.findBySkus(anyList())).thenReturn(List.of(
                new ItemResult("SKU-2", "p2", 100, "MAKE", "CAT", "EA", 0, true)));

        sut.resolveAll(lines);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(loadItemPort, times(1)).findBySkus(captor.capture());
        assertThat(captor.getValue()).containsExactly("SKU-2");
    }

    @Test
    @DisplayName("sku가 마스터에 없으면 ITEM_NOT_FOUND로 실패한다")
    void 미존재sku면_ITEM_NOT_FOUND() {
        List<PurchaseRequested.Line> lines = List.of(line("SKU-X", null));
        when(loadItemPort.findBySkus(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> sut.resolveAll(lines))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("sku는 있으나 sourcingType이 불명이면 BUY로 degrade한다")
    void 불명sourcingType이면_BUY_degrade() {
        List<PurchaseRequested.Line> lines = List.of(line("SKU-1", null));
        when(loadItemPort.findBySkus(anyList())).thenReturn(List.of(
                new ItemResult("SKU-1", "p1", 100, null, "CAT", "EA", 0, true)));

        Map<SourcingType, List<PurchaseRequested.Line>> result = sut.resolveAll(lines);

        assertThat(result.get(SourcingType.BUY)).extracting(PurchaseRequested.Line::sku).containsExactly("SKU-1");
    }

    @Test
    @DisplayName("빈 라인이면 조회 없이 빈 맵을 반환한다")
    void 빈라인이면_빈맵() {
        assertThat(sut.resolveAll(List.of())).isEmpty();
        verify(loadItemPort, never()).findBySkus(any());
    }
}
