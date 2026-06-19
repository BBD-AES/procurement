package com.bbd.procurement.purchaseorder.adapter.in.web.request;

import com.bbd.procurement.purchaseorder.application.port.in.command.PurchaseOrderLineItem;
import com.bbd.procurement.purchaseorder.application.port.in.command.RegisterPurchaseOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RegisterPurchaseOrderRequest(

        @NotBlank(message = "vendorCode는 필수입니다.")
        @Size(max = 10, message = "vendorCode는 10자 이내여야 합니다.")
        String vendorCode,

        @NotBlank(message = "warehouseCode는 필수입니다.")
        @Size(max = 20, message = "warehouseCode는 20자 이내여야 합니다.")
        String warehouseCode,

        @Size(max = 30, message = "soNumber는 30자 이내여야 합니다.")
        String soNumber,

        LocalDate expectedArrival,

        String note,

        @Valid
        List<PurchaseOrderLineItemRequest> lines
) {
    public RegisterPurchaseOrderCommand toCommand(Long createdBy) {
        List<PurchaseOrderLineItem> items = lines == null
                ? List.of()
                :
                lines.stream().map(PurchaseOrderLineItemRequest::toCommandItem).toList();
        return new RegisterPurchaseOrderCommand(
                vendorCode, warehouseCode, soNumber, expectedArrival, note, items, createdBy
        );
    }
}
