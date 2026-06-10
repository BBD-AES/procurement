package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.in.command.CompletePurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface CompletePurchaseOrderUseCase {

    PurchaseOrder complete(CompletePurchaseOrderCommand command);

}
