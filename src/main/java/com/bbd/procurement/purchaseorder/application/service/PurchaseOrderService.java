package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.auth.Role;
import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.in.*;
import com.bbd.procurement.purchaseorder.application.port.in.command.*;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.PurchaseOrderNumberGeneratorPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService implements
        RegisterPurchaseOrderUseCase,
        UpdatePurchaseOrderHeaderUseCase,
        UpdatePurchaseOrderLinesUseCase,
        ConfirmPurchaseOrderUseCase,
        CancelPurchaseOrderUseCase,
        GetPurchaseOrderQuery,
        ListPurchaseOrderQuery {

    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final PurchaseOrderNumberGeneratorPort purchaseOrderNumberGeneratorPort;

    @Override
    @Transactional
    public PurchaseOrder register(RegisterPurchaseOrderCommand command) {
        String poNumber = purchaseOrderNumberGeneratorPort.generate();
        List<PurchaseOrderLine> lines = toLines(command.lines());

        PurchaseOrder po = PurchaseOrder.create(
                poNumber,
                command.vendorCode(),
                command.warehouseCode(),
                command.soId(),
                command.expectedArrival(),
                command.note(),
                lines,
                command.createdBy()

        );
        return savePurchaseOrderPort.save(po);
    }

    @Override
    @Transactional
    public PurchaseOrder updateHeader(UpdatePurchaseOrderHeaderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        po.updateHeader(
                command.vendorCode(),
                command.warehouseCode(),
                command.soId(),
                command.expectedArrival(),
                command.note()
        );
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder updateLines(UpdatePurchaseOrderLinesCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        po.replaceLines(toLines(command.lines()));
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder confirm(ConfirmPurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        po.confirm(command.confirmedBy());
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder cancel(CancelPurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        ensureCancelAllowed(command.requesterRole(), po.getStatus());
        po.cancel();
        return po;
    }

    @Override
    public PurchaseOrder getByPoNumber(String poNumber) {
        return findPurchaseOrderOrThrow(poNumber);
    }

    @Override
    public List<PurchaseOrder> list() {
        return loadPurchaseOrderPort.findAll();
    }

    private PurchaseOrder findPurchaseOrderOrThrow(String poNumber) {
        return loadPurchaseOrderPort.findByPoNumber(poNumber)
                .orElseThrow(() -> new ApiException(ErrorCode.PO_NOT_FOUND));
    }

    private List<PurchaseOrderLine> toLines(List<PurchaseOrderLineItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> PurchaseOrderLine.create(
                        item.lineOrder(),
                        item.sku(),
                        item.partName(),
                        item.unitPrice(),
                        item.quantity()
                ))
                .toList();
    }

    private void ensureCancelAllowed(Role requesterRole, PurchaseOrderStatus currentStatus) {
        if (requesterRole == Role.HQ_STAFF && currentStatus != PurchaseOrderStatus.DRAFT) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }
}

