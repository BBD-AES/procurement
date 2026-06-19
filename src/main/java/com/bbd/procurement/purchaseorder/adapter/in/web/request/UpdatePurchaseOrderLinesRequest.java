package com.bbd.procurement.purchaseorder.adapter.in.web.request;

import com.bbd.procurement.purchaseorder.application.port.in.command.PurchaseOrderLineItem;
import com.bbd.procurement.purchaseorder.application.port.in.command.UpdatePurchaseOrderLinesCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdatePurchaseOrderLinesRequest(

        @Valid
        @NotEmpty(message = "lines는 최소 1개 이상이여야 합니다.")
        List<PurchaseOrderLineItemRequest> lines
) {
    public UpdatePurchaseOrderLinesCommand toCommand(String poNumber, Long updatedBy) {
        List<PurchaseOrderLineItem> items = lines.stream()
                .map(PurchaseOrderLineItemRequest::toCommandItem)
                .toList();
        return new UpdatePurchaseOrderLinesCommand(poNumber, items, updatedBy);
    }
}
