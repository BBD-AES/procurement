package com.bbd.procurement.workorder.adapter.in.web.request;

import com.bbd.procurement.workorder.application.port.in.command.WorkOrderLineItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record WorkOrderLineItemRequest(
        @Positive(message = "lineOrder는 1 이상이어야 합니다.")
        int lineOrder,

        @NotBlank(message = "sku는 필수입니다.")
        @Size(max = 50, message = "sku는 50자 이내여야 합니다.")
        String sku,

        @Positive(message = "quantity는 1 이상이어야 합니다.")
        int quantity
) {
    public WorkOrderLineItem toCommandItem() {
        return new WorkOrderLineItem(lineOrder, sku, quantity);
    }
}
