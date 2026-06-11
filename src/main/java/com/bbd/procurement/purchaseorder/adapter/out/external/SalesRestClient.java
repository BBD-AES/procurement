package com.bbd.procurement.purchaseorder.adapter.out.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SalesRestClient {

    private final RestClient restClient;

    public SalesRestClient(@Value("{sales.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public SalesOrderResponse getSalesOrder(String soNumber) {
        return restClient.get()
                .uri("/api/v1/sales-orders/{soNumber}", soNumber)
                .retrieve()
                .body(SalesOrderResponse.class);
    }
}
