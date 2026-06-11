package com.bbd.procurement.purchaseorder.adapter.in.web.request;

import com.bbd.procurement.purchaseorder.application.port.in.command.PurchaseOrderLineItem;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PurchaseOrderLineItemRequest(
        @Positive(message = "lineOrder는 1 이상이어야 합니다.")
        int lineOrder,

        @NotBlank(message = "sku는 필수입니다.")
        @Size(max = 50, message = "sku는 50자 이내여야 합니다.")
        String sku,

        @Positive(message = "quantity는 1 이상이어야 합니다.")
        int quantity
) {
    public PurchaseOrderLineItem toCommandItem() {
        return new PurchaseOrderLineItem(lineOrder, sku, quantity);
    }
}
