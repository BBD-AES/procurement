package com.bbd.procurement.purchaseorder.application.port.in.command;

import com.bbd.procurement.global.auth.Role;

public record CancelPurchaseOrderCommand(
        String poNumber,
        String requesterId,
        Role requesterRole
) {
}
