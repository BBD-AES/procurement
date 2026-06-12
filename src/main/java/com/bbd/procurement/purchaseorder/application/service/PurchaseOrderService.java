package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.in.*;
import com.bbd.procurement.purchaseorder.application.port.in.command.*;
import com.bbd.procurement.purchaseorder.application.port.out.*;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderChangeType;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderHistory;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrderLine;
import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
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
        ListPurchaseOrderQuery,
        GetPurchaseOrderHistoryQuery{

    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final PurchaseOrderNumberGeneratorPort purchaseOrderNumberGeneratorPort;
    private final SaveOutboxEventPort saveOutboxEventPort;
    private final ObjectMapper objectMapper;
    private final LoadItemPort loadItemPort;
    private final SavePurchaseOrderHistoryPort savePurchaseOrderHistoryPort;
    private final LoadPurchaseOrderHistoryPort loadPurchaseOrderHistoryPort;

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
        PurchaseOrder saved = savePurchaseOrderPort.save(po);
        recordHistory(saved, PurchaseOrderChangeType.CREATED, null, command.createdBy());
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder updateHeader(UpdatePurchaseOrderHeaderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        String before = snapshot(po);
        po.updateHeader(
                command.vendorCode(),
                command.warehouseCode(),
                command.soNumber(),
                command.expectedArrival(),
                command.note()
        );
        recordHistory(po, PurchaseOrderChangeType.HEADER_UPDATED, before, command.updatedBy());
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder updateLines(UpdatePurchaseOrderLinesCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        String before = snapshot(po);
        po.replaceLines(toLines(command.lines()));
        recordHistory(po, PurchaseOrderChangeType.LINES_REPLACED, before, command.updatedBy());
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder complete(CompletePurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        String before = snapshot(po);
        po.markReceived(command.receivedBy());
        publishStockInRequested(po);
        recordHistory(po, PurchaseOrderChangeType.COMPLETED, before, command.receivedBy());
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder cancel(CancelPurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        String before = snapshot(po);
        po.cancel();
        recordHistory(po, PurchaseOrderChangeType.CANCELED, before, command.requesterId());
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

    @Override
    public List<PurchaseOrderHistory> getHistory(String poNumber) {
        findPurchaseOrderOrThrow(poNumber);
        return loadPurchaseOrderHistoryPort.findByPoNumber(poNumber);
    }

    private List<PurchaseOrderLine> toLines(List<PurchaseOrderLineItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> {
                    ItemResult itemInfo = loadItemPort.findBySku(item.sku());
                    return PurchaseOrderLine.create(
                            item.lineOrder(),
                            item.sku(),
                            itemInfo.partName(),
                            new BigDecimal(itemInfo.unitPrice()),
                            item.quantity()
                    );
                })
                .toList();
    }

    private String snapshot(PurchaseOrder po) {
        try {
            return objectMapper.writeValueAsString(PurchaseOrderSnapshot.from(po));
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to serialize PurchaseOrderSnapshot for PO" + po.getPoNumber(), e
            );
        }
    }

    private void recordHistory(PurchaseOrder po,
                               PurchaseOrderChangeType changeType,
                               String beforePayload,
                               String changedBy) {
        PurchaseOrderHistory history = PurchaseOrderHistory.create(
                po.getPoNumber(),
                changeType,
                beforePayload,
                snapshot(po),
                changedBy
        );
        savePurchaseOrderHistoryPort.save(history);
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

        saveOutboxEventPort.save(outboxEvent);
    }
}

