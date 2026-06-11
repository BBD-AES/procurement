package com.bbd.procurement.purchaseorder.adapter.in.web.request;

import com.bbd.procurement.purchaseorder.application.port.in.command.UpdatePurchaseOrderHeaderCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePurchaseOrderHeaderRequest(

        @NotBlank(message = "vendorCode는 필수입니다.")
        @Size(max = 10)
        String vendorCode,

        @NotBlank(message = "warehouseCode는 필수입니다.")
        @Size(max = 20)
        String warehouseCode,

        @Size(max = 30)
        String soNumber,

        LocalDate expectedArrival,

        String note
) {
    public UpdatePurchaseOrderHeaderCommand toCommand(String poNumber) {
        return new UpdatePurchaseOrderHeaderCommand(
                poNumber, vendorCode, warehouseCode, soNumber, expectedArrival, note
        );
    }
}
