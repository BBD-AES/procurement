package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.global.util.JsonUtil;
import com.bbd.procurement.purchaseorder.application.port.in.*;
import com.bbd.procurement.purchaseorder.application.port.in.command.*;
import com.bbd.procurement.purchaseorder.application.port.out.*;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.*;
import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import com.bbd.procurement.vendor.application.port.out.LoadVendorPort;
import com.bbd.procurement.vendor.domain.Vendor;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService implements
        RegisterPurchaseOrderUseCase,
        UpdatePurchaseOrderHeaderUseCase,
        UpdatePurchaseOrderLinesUseCase,
        OrderPurchaseOrderUseCase,
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
    private final LoadVendorPort loadVendorPort;
    private final GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;

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

        validateVendor(command.vendorCode());

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
        if (StringUtils.hasText(command.vendorCode())) {
            validateVendor(command.vendorCode());
        }
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

    /**
     * 주문 전이(DRAFT → ORDERED). 공급사에 발주 확정. 재고 반영(StockInRequested)은 아직 하지 않는다.
     * 실제 재고 증가/백오더 충당은 입고완료(complete) 시점에 일어난다.
     */
    @Override
    @Transactional
    public PurchaseOrder order(OrderPurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        String before = snapshot(po);
        po.markOrdered(command.orderedBy());
        recordHistory(po, PurchaseOrderChangeType.ORDERED, before, command.orderedBy());
        return po;
    }

    /**
     * 입고완료 전이(ORDERED → RECEIVED). 이 시점에 재고 증가(StockInRequested) outbox 발행 +
     * 같은 soNumber 백오더 충당 + 이력(COMPLETED)을 한 트랜잭션으로 처리한다.
     */
    @Override
    @Transactional
    public PurchaseOrder complete(CompletePurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        String before = snapshot(po);
        po.markReceived(command.receivedBy());
        publishStockInRequested(po);
        applyRequestFulfillment(po);
        recordHistory(po, PurchaseOrderChangeType.COMPLETED, before, command.receivedBy());
        return po;
    }

    @Override
    @Transactional
    public PurchaseOrder cancel(CancelPurchaseOrderCommand command) {
        PurchaseOrder po = findPurchaseOrderOrThrow(command.poNumber());
        String before = snapshot(po);
        boolean transitioned = po.cancel();
        if (transitioned) {
            recordHistory(po, PurchaseOrderChangeType.CANCELED, before, command.requesterId());
        }
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

    /**
     * 발주 생성/헤더수정 시 공급사 통제. 존재하지 않으면 VENDOR_NOT_FOUND(V001),
     * 거래중지(active=false) 공급사면 VENDOR_INACTIVE(V003)로 차단한다.
     */
    private void validateVendor(String vendorCode) {
        Vendor vendor = loadVendorPort.findByCode(vendorCode)
                .orElseThrow(() -> new ApiException(ErrorCode.VENDOR_NOT_FOUND));
        if (!vendor.isActive()) {
            throw new ApiException(ErrorCode.VENDOR_INACTIVE);
        }
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
                            item.quantity(),
                            itemInfo.category(),
                            itemInfo.unit(),
                            itemInfo.safetyStock(),
                            itemInfo.active(),
                            itemInfo.sourcingType()
                    );
                })
                .toList();
    }

    private String snapshot(PurchaseOrder po) {
        return JsonUtil.toJson(objectMapper, PurchaseOrderSnapshot.from(po),
                "PurchaseOrderSnapshot for PO " + po.getPoNumber());
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
                changedBy,
                resolveChangedByName()
        );
        savePurchaseOrderHistoryPort.save(history);
    }

    /**
     * 이력 변경자 이름 스냅샷. changedBy(userId)는 항상 컨트롤러가 현재 사용자 스냅샷에서 주입하므로,
     * 같은 스냅샷의 displayName을 사용한다(요청 스레드 내 보안 컨텍스트 기준). 미확인 시 null → 조회측 #id 폴백.
     */
    private String resolveChangedByName() {
        return getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().displayName();
    }

    private void publishStockInRequested(PurchaseOrder po) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        StockInRequested event = po.toStockInRequested(eventId, occurredAt);
        String payload = JsonUtil.toJson(objectMapper, event,
                "StockInRequested for PO " + po.getPoNumber());

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

    /**
     * 완료(RECEIVED)된 PO 라인을 sku별 수량으로 집계해, 같은 soNumber의 활성(PENDING/PARTIAL)
     * 요청 알림 라인에 FIFO(receivedAt 오름차순)로 충당한다. 부분 충족은 PARTIAL로 남는다.
     */
    private void applyRequestFulfillment(PurchaseOrder po) {
        if (!StringUtils.hasText(po.getSoNumber())) {
            return;
        }

        Map<String, Integer> orderedBySku = po.getLines().stream()
                .collect(Collectors.groupingBy(
                        PurchaseOrderLine::getSku,
                        Collectors.summingInt(PurchaseOrderLine::getQuantity)));

        List<PurchaseRequestNotification> active =
                loadPurchaseRequestNotificationPort.findActiveBySoNumber(po.getSoNumber());

        orderedBySku.forEach((sku, qty) -> {
            int remaining = qty;
            for (PurchaseRequestNotification notification : active) {
                if (remaining <= 0) {
                    break;
                }
                remaining -= notification.applyFulfillment(sku, remaining);
            }
        });
    }
}

