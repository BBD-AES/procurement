package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemClientAdapter implements LoadItemPort {

    private static final int MAX_BATCH = 50;
    private final ItemHttpService itemHttpService;

    @Override
    public ItemResult findBySku(String sku) {
        ItemResponse response = getItem(sku);
        return new ItemResult(response.sku(), response.partName(), response.unitPrice(), response.sourcingType(),
                response.category(), response.unit(), response.safetyStock(), response.active());
    }

    @Override
    public List<ItemResult> findBySkus(List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return List.of();
        }

        List<ItemResult> results = new ArrayList<>();
        for (int i =0;i<skus.size();i+=MAX_BATCH) {
            List<String> chunk = skus.subList(i, Math.min(i+MAX_BATCH, skus.size()));
            for (ItemResponse r : getItems(chunk)) {
                results.add(new ItemResult(r.sku(), r.partName(), r.unitPrice(), r.sourcingType(),
                        r.category(), r.unit(), r.safetyStock(), r.active()));
            }
        }
        return results;
    }

    private ItemResponse getItem(String sku) {
        try {
            return itemHttpService.getItem(sku);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(ErrorCode.ITEM_NOT_FOUND);
        } catch (Exception e) {
            log.error("Item 서비스 호출 실패 sku={}", sku, e);
            log.info( "⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️ {} ⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️", e.getMessage());
            throw new ApiException(ErrorCode.ITEM_SERVICE_ERROR);
        }
    }

    private List<ItemResponse> getItems(List<String> skus) {
        try {
            return itemHttpService.getItems(skus);
        } catch (Exception e) {
            log.error("Item 서비스 다건 호출 실패 skus={}", skus, e);
            throw new ApiException(ErrorCode.ITEM_SERVICE_ERROR);
        }
    }
}
