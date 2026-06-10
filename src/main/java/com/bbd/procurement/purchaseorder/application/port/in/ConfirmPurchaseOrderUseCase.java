package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.in.command.ConfirmPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface ConfirmPurchaseOrderUseCase {

    PurchaseOrder confirm(ConfirmPurchaseOrderCommand command);

}
