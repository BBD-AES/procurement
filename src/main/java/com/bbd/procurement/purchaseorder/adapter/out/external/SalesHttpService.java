package com.bbd.procurement.purchaseorder.adapter.out.external;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@Component
@HttpExchange("/api/v1/sales-orders")
public interface SalesHttpService {

    @GetExchange("/{soNumber}")
    SalesOrderResponse getSalesOrder(@PathVariable String soNumber);
}
