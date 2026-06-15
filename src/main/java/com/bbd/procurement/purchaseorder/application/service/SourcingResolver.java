package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.SourcingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourcingResolver {

    private final LoadItemPort loadItemPort;

    /**
     * 라인의 조달유형 판정.
     * 1) 이벤트 힌트(hint) 우선 — BUY/MAKE면 그대로.
     * 2) null/미지정이면 item 마스터로 재해석.
     * 3) 마스터도 불명확하면 BUY로 degrade.
     */

    public SourcingType resolve(String sku, String hint) {
        SourcingType fromHint = SourcingType.from(hint);

        if (fromHint != null) {
            return fromHint;
        }

        ItemResult item = loadItemPort.findBySku(sku);
        SourcingType fromMaster = SourcingType.from(item.sourcingType());

        if (fromMaster != null) {
            return fromMaster;
        }

        log.warn("sourcingType 불명 sku={} master={] -> BUY로 처리", sku, hint, item.sourcingType());
        return SourcingType.BUY;
    }
}
