package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.in.command.OrderPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

public interface OrderPurchaseOrderUseCase {

    PurchaseOrder order(OrderPurchaseOrderCommand command);

}
