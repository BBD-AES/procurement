package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.SourcingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourcingResolver {

    private final LoadItemPort loadItemPort;

    public Map<SourcingType, List<PurchaseRequested.Line>> resolveAll(List<PurchaseRequested.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return Map.of();
        }

        // item 서비스에 조달 방식을 물어봐야하는 sku 목록
        List<String> masterSkus = lines.stream()
                .filter(line -> SourcingType.from(line.sourcingType()) == null)
                .map(PurchaseRequested.Line::sku)
                .distinct()
                .toList();

        Map<String, ItemResult> masterBySku = masterSkus.isEmpty()
                ? Map.of()
                : loadItemPort.findBySkus(masterSkus).stream()
                        .collect(Collectors.toMap(ItemResult::sku, Function.identity(), (a, b) -> a));

        return lines.stream()
                .collect(Collectors.groupingBy(line -> resolveLine(line, masterBySku)));
    }

    private SourcingType resolveLine(PurchaseRequested.Line line, Map<String, ItemResult> masterBySku) {
        SourcingType fromHint = SourcingType.from(line.sourcingType());
        if (fromHint != null) {
            return fromHint;
        }

        ItemResult item = masterBySku.get(line.sku());
        if (item == null) {
            throw new ApiException(ErrorCode.ITEM_NOT_FOUND);
        }

        SourcingType fromMaster = SourcingType.from(item.sourcingType());
        if (fromMaster != null) {
            return fromMaster;
        }

        log.warn("sourcingType 불명 sku={} master={} -> BUY로 처리", line.sku(), item.sourcingType());
        return SourcingType.BUY;
    }
}
