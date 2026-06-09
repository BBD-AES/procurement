package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.in.command.UpdatePurchaseOrderHeaderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface UpdatePurchaseOrderHeaderUseCase {

    PurchaseOrder updateHeader(UpdatePurchaseOrderHeaderCommand command);

}
