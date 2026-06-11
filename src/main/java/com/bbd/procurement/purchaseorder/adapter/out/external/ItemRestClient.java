package com.bbd.procurement.purchaseorder.adapter.out.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

public class ItemRestClient {

    private final RestClient restClient;

    public ItemRestClient(@Value("${item.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ItemResponse getItem(String sku) {
        return restClient.get()
                .uri("/api/v1/items/{sku}", sku)
                .retrieve()
                .body(ItemResponse.class);
    }
}
