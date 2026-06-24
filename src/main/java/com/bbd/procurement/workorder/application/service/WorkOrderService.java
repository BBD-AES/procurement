package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.global.util.JsonUtil;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import com.bbd.procurement.workorder.application.port.in.CancelWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.CompleteWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.CreateWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderHistoryQuery;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderQuery;
import com.bbd.procurement.workorder.application.port.in.StartWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.UpdateWorkOrderHeaderUseCase;
import com.bbd.procurement.workorder.application.port.in.UpdateWorkOrderLinesUseCase;
import com.bbd.procurement.workorder.application.port.in.command.CancelWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.in.command.CompleteWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.in.command.CreateWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderHeaderCommand;
import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderLinesCommand;
import com.bbd.procurement.workorder.application.port.in.command.WorkOrderLineItem;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderHistoryPort;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderHistoryPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.WorkOrderNumberGeneratorPort;
import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderChangeType;
import com.bbd.procurement.workorder.domain.WorkOrderHistory;
import com.bbd.procurement.workorder.domain.WorkOrderLine;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
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
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderService implements CreateWorkOrderUseCase, StartWorkOrderUseCase, CompleteWorkOrderUseCase, CancelWorkOrderUseCase, UpdateWorkOrderHeaderUseCase, UpdateWorkOrderLinesUseCase, GetWorkOrderQuery, GetWorkOrderHistoryQuery {

    private final SaveWorkOrderPort saveWorkOrderPort;
    private final LoadWorkOrderPort loadWorkOrderPort;
    private final WorkOrderNumberGeneratorPort workOrderNumberGeneratorPort;
    private final LoadItemPort loadItemPort;
    private final SaveOutboxEventPort saveOutboxEventPort;
    private final ObjectMapper objectMapper;
    private final LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;
    private final SaveWorkOrderHistoryPort saveWorkOrderHistoryPort;
    private final LoadWorkOrderHistoryPort loadWorkOrderHistoryPort;
    private final GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;

    @Override
    @Transactional
    public WorkOrder create(CreateWorkOrderCommand command) {
        // 멱등 사전 조회: 동일 requestId로 이미 만든 WO가 있으면 새로 만들지 않고 그대로 반환(replay).
        // (시간차 더블클릭/재시도를 여기서 흡수한다. requestId 미전송 레거시 요청은 건너뛰고 기존대로 생성.)
        if (StringUtils.hasText(command.requestId())) {
            Optional<WorkOrder> existing = loadWorkOrderPort.findByRequestId(command.requestId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        try {
            String number = workOrderNumberGeneratorPort.generate();
            List<WorkOrderLine> lines = toLines(command.lines());

            WorkOrder workOrder = WorkOrder.create(
                    number, command.soNumber(), command.warehouseCode(), lines, command.createdBy(), command.requestId()
            );
            return saveWorkOrderPort.save(workOrder);
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 들어온 요청들이 사전 조회를 모두 통과한 경우(TOCTOU):
            // DB의 uq_work_order_request UNIQUE 제약이 두 번째 INSERT를 거부한다 → 409로 응답.
            throw new ApiException(ErrorCode.WORK_ORDER_DUPLICATE_REQUEST);
        }
    }

    @Override
    @Transactional
    public WorkOrder start(String workOrderNumber) {
        WorkOrder workOrder = findOrThrow(workOrderNumber);
        workOrder.start();
        return workOrder;
    }

    @Override
    @Transactional
    public WorkOrder complete(CompleteWorkOrderCommand command) {
        WorkOrder workOrder = findOrThrow(command.workOrderNumber());
        workOrder.markCompleted(command.completedBy());
        publishStockInRequested(workOrder);
        applyRequestFulfillment(workOrder);
        return workOrder;
    }

    @Override
    @Transactional
    public WorkOrder cancel(CancelWorkOrderCommand command) {
        WorkOrder workOrder = findOrThrow(command.workOrderNumber());
        String before = snapshot(workOrder);
        boolean transitioned = workOrder.cancel();
        if (transitioned) {
            recordHistory(workOrder, WorkOrderChangeType.CANCELED, before, command.requesterId());
        }
        return workOrder;
    }

    @Override
    @Transactional
    public WorkOrder updateHeader(UpdateWorkOrderHeaderCommand command) {
        WorkOrder workOrder = findOrThrow(command.workOrderNumber());
        String before = snapshot(workOrder);
        workOrder.updateHeader(command.warehouseCode(), command.soNumber());
        recordHistory(workOrder, WorkOrderChangeType.HEADER_UPDATED, before, command.updatedBy());
        return workOrder;
    }

    @Override
    @Transactional
    public WorkOrder updateLines(UpdateWorkOrderLinesCommand command) {
        WorkOrder workOrder = findOrThrow(command.workOrderNumber());
        String before = snapshot(workOrder);
        workOrder.replaceLines(toLines(command.lines()));
        recordHistory(workOrder, WorkOrderChangeType.LINES_REPLACED, before, command.updatedBy());
        return workOrder;
    }

    @Override
    public WorkOrder getByWorkOrderNumber(String workOrderNumber) {
        return findOrThrow(workOrderNumber);
    }

    @Override
    public List<WorkOrder> list() {
        return loadWorkOrderPort.findAll();
    }

    @Override
    public List<WorkOrderHistory> getHistory(String workOrderNumber) {
        findOrThrow(workOrderNumber);
        return loadWorkOrderHistoryPort.findByWorkOrderNumber(workOrderNumber);
    }

    private String snapshot(WorkOrder workOrder) {
        return JsonUtil.toJson(objectMapper, WorkOrderSnapshot.from(workOrder),
                "WorkOrderSnapshot for WO " + workOrder.getWorkOrderNumber());
    }

    private void recordHistory(WorkOrder workOrder,
                               WorkOrderChangeType changeType,
                               String beforePayload,
                               Long changedBy) {
        WorkOrderHistory history = WorkOrderHistory.create(
                workOrder.getWorkOrderNumber(),
                changeType,
                beforePayload,
                snapshot(workOrder),
                changedBy,
                resolveChangedByName()
        );
        saveWorkOrderHistoryPort.save(history);
    }

    /**
     * 이력 변경자 이름 스냅샷. changedBy(userId)는 항상 컨트롤러가 현재 사용자 스냅샷에서 주입하므로,
     * 같은 스냅샷의 displayName을 사용한다(요청 스레드 내 보안 컨텍스트 기준). 미확인 시 null → 조회측 #id 폴백.
     */
    private String resolveChangedByName() {
        return getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().displayName();
    }

    private WorkOrder findOrThrow(String workOrderNumber) {
        return loadWorkOrderPort.findByWorkOrderNumber(workOrderNumber)
                .orElseThrow(() -> new ApiException(ErrorCode.WORK_ORDER_NOT_FOUND));
    }

    private List<WorkOrderLine> toLines(List<WorkOrderLineItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> {
                    ItemResult itemInfo = loadItemPort.findBySku(item.sku());
                    return WorkOrderLine.create(
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

    private void publishStockInRequested(WorkOrder workOrder) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        List<StockInRequested.Line> lines = workOrder.getLines().stream()
                .map(line -> new StockInRequested.Line(
                        line.getSku(),
                        line.getPartName(),
                        line.getCategory(),
                        line.getUnit(),
                        line.getSafetyStock(),
                        line.getQuantity(),
                        workOrder.getWarehouseCode(),
                        line.getUnitPrice().intValueExact(),
                        line.isActive(),
                        line.getSourcingType()
                ))
                .toList();

        StockInRequested event = StockInRequested.of(
                eventId, occurredAt, workOrder.getWorkOrderNumber(), workOrder.getSoNumber(), lines
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to serialize StockInRequested for WorkOrder" + workOrder.getWorkOrderNumber(), e);
        }

        OutboxEvent outboxEvent = OutboxEvent.create(
                StockInRequested.TOPIC,
                eventId,
                "WorkOrder",
                workOrder.getWorkOrderNumber(),
                StockInRequested.EVENT_TYPE,
                payload,
                LocalDateTime.now()
        );
        saveOutboxEventPort.save(outboxEvent);
    }

    /**
     * 완료(COMPLETED)된 작업지시 라인을 sku별 수량으로 집계해, 같은 soNumber의 활성(PENDING/PARTIAL)
     * 생산요청 알림 라인에 FIFO로 충당한다. 부분 충족은 PARTIAL로 남는다.
     */
    private void applyRequestFulfillment(WorkOrder workOrder) {
        if (!StringUtils.hasText(workOrder.getSoNumber())) {
            return;
        }

        Map<String, Integer> producedBySku = workOrder.getLines().stream()
                .collect(Collectors.groupingBy(
                        WorkOrderLine::getSku,
                        Collectors.summingInt(WorkOrderLine::getQuantity)));

        List<WorkOrderRequestNotification> active =
                loadWorkOrderRequestNotificationPort.findActiveBySoNumber(workOrder.getSoNumber());

        producedBySku.forEach((sku, qty) -> {
            int remaining = qty;
            for (WorkOrderRequestNotification notification : active) {
                if (remaining <= 0) {
                    break;
                }
                remaining -= notification.applyFulfillment(sku, remaining);
            }
        });
    }
}
