package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;

import java.util.List;

public interface LoadItemPort {

    ItemResult findBySku(String sku);

    List<ItemResult> findBySkus(List<String> skus);

}
