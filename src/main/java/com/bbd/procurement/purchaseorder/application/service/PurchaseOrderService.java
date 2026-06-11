package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.in.*;
import com.bbd.procurement.purchaseorder.application.port.in.command.*;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.PurchaseOrderNumberGeneratorPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseOrderPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.adapter.out.persistence.OutboxEventJpaRepository;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService implements
        RegisterPurchaseOrderUseCase,
        UpdatePurchaseOrderHeaderUseCase,
        UpdatePurchaseOrderLinesUseCase,
        CompletePurchaseOrderUseCase,
        CancelPurchaseOrderUseCase,
        GetPurchaseOrderQuery,
        ListPurchaseOrderQuery {

    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final PurchaseOrderNumberGeneratorPort purchaseOrderNumberGeneratorPort;
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PurchaseOrder register(RegisterPurchaseOrderCommand command) {
        String poNumber = purchaseOrderNumberGeneratorPort.generate();
        List<PurchaseOrderLine> lines = toLines(command.lines());

        PurchaseOrder po = PurchaseOrder.create(
                poNumber,
                command.vendorCode(),
                command.warehouseCode(),
                command.soNumber(),
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
                command.soNumber(),
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
    public PurchaseOrder complete(CompletePurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        po.markReceived(command.receivedBy());
        publishStockInRequested(po);
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder cancel(CancelPurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
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

    private void publishStockInRequested(PurchaseOrder po) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        List<StockInRequested.Line> lines = po.getLines().stream()
                .map(line -> new StockInRequested.Line(
                        line.getSku(),
                        line.getQuantity(),
                        po.getWarehouseCode(),
                        line.getUnitPrice().intValueExact()
                ))
                .toList();

        StockInRequested event = StockInRequested.of(
                eventId,
                occurredAt,
                po.getPoNumber(),
                po.getSoNumber(),
                lines
        );

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to serialize StockInRequested for PO" +
                            po.getPoNumber(), e
            );
        }

        OutboxEvent outboxEvent = OutboxEvent.create(
                StockInRequested.TOPIC,
                eventId,
                "PurchaseOrder",
                po.getPoNumber(),
                StockInRequested.EVENT_TYPE,
                payload,
                LocalDateTime.now()
        );

        outboxEventJpaRepository.save(outboxEvent);
    }
}

