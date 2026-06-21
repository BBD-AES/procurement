package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.in.*;
import com.bbd.procurement.purchaseorder.application.port.in.command.*;
import com.bbd.procurement.purchaseorder.application.port.out.*;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.*;
import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;

    @Override
    @Transactional
    public PurchaseOrder register(RegisterPurchaseOrderCommand command) {
        // 멱등 사전 조회: 동일 requestId로 이미 만든 PO가 있으면 새로 만들지 않고 그대로 반환(replay).
        // (시간차 더블클릭/재시도를 여기서 흡수한다. requestId 미전송 레거시 요청은 건너뛰고 기존대로 생성.)
        if (StringUtils.hasText(command.requestId())) {
            Optional<PurchaseOrder> existing =
                    loadPurchaseOrderPort.findByRequestId(command.requestId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        try {
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
                    command.createdBy(),
                    command.requestId()
            );
            PurchaseOrder saved = savePurchaseOrderPort.save(po);
            markRequestNotificationDone(saved.getSoNumber());
            recordHistory(saved, PurchaseOrderChangeType.CREATED, null, command.createdBy());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 들어온 요청들이 사전 조회를 모두 통과한 경우(TOCTOU):
            // DB의 uq_purchase_order_request UNIQUE 제약이 두 번째 INSERT를 거부한다.
            // 이미 롤백 표시된 트랜잭션 안에서 재조회하지 않고 409로 응답한다.
            throw new ApiException(ErrorCode.PO_DUPLICATE_REQUEST);
        }
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
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<String> skus = items.stream()
                .map(PurchaseOrderLineItem::sku)
                .distinct()
                .toList();

        Map<String, ItemResult> itemMap = loadItemPort.findBySkus(skus).stream()
                .collect(Collectors.toMap(ItemResult::sku, Function.identity(), (a,b) -> a));

        return items.stream()
                .map(item -> {
                    ItemResult itemInfo = itemMap.get(item.sku());
                    if (itemInfo == null) {
                        throw new ApiException(ErrorCode.ITEM_NOT_FOUND);
                    }
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
                               Long changedBy) {
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

    private void markRequestNotificationDone(String soNumber) {
        if (!StringUtils.hasText(soNumber)) {
            return;
        }

        loadPurchaseRequestNotificationPort.findPendingBySoNumber(soNumber)
                .forEach(PurchaseRequestNotification::markDone);
    }
}

