package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemClientAdapter implements LoadItemPort {

    private final ItemHttpService itemHttpService;

    @Override
    public ItemResult findBySku(String sku) {
        ItemResponse response = getItem(sku);
        return new ItemResult(response.sku(), response.partName(), response.unitPrice(), response.sourcingType());
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
}
