package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.in.command.UpdatePurchaseOrderLinesCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface UpdatePurchaseOrderLinesUseCase {

    PurchaseOrder updateLines(UpdatePurchaseOrderLinesCommand command);

}
