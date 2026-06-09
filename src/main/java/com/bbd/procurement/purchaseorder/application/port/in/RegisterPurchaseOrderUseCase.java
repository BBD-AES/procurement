package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.in.command.RegisterPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface RegisterPurchaseOrderUseCase {

    PurchaseOrder register(RegisterPurchaseOrderCommand command);

}
