package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.in.command.CancelPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface CancelPurchaseOrderUseCase {

    PurchaseOrder cancel(CancelPurchaseOrderCommand command);

}
