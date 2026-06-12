package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class ItemRestClient implements LoadItemPort {

    private final RestClient restClient;

    public ItemRestClient(@Value("${item.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public ItemResult findBySku(String sku) {
        ItemResponse response = getItem(sku);
        return new ItemResult(response.sku(), response.partName(), response.unitPrice());
    }

    private ItemResponse getItem(String sku) {
        try {
            return restClient.get()
                    .uri("/api/v1/items/{sku}", sku)
                    .retrieve()
                    .body(ItemResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(ErrorCode.ITEM_NOT_FOUND);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.ITEM_SERVICE_ERROR);
        }
    }
}
