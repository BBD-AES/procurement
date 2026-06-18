package com.bbd.procurement.purchaseorder.adapter.out.external;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@Component
@HttpExchange("/api/v1/items")
public interface ItemHttpService {

    @GetExchange("/{sku}")
    ItemResponse getItem(@PathVariable String sku);

    @GetExchange
    List<ItemResponse> getItems(@RequestParam List<String> sku);

}
