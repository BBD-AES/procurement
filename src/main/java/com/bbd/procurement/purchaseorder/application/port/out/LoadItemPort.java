package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;

public interface LoadItemPort {

    ItemResult findBySku(String sku);

}
