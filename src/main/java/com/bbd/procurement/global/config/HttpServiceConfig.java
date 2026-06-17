package com.bbd.procurement.global.config;

import com.bbd.procurement.purchaseorder.adapter.out.external.ItemHttpService;
import com.bbd.procurement.purchaseorder.adapter.out.external.SalesHttpService;
import org.springframework.web.service.registry.ImportHttpServices;


@ImportHttpServices(
        group = "bbd-item-service",
        types = ItemHttpService.class
)

@ImportHttpServices(
        group = "bbd-sales-service",
        types = SalesHttpService.class
)
public class HttpServiceConfig {

}
