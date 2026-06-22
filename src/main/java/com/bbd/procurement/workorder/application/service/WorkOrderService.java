package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import com.bbd.procurement.workorder.application.port.in.CompleteWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.CreateWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderQuery;
import com.bbd.procurement.workorder.application.port.in.StartWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.command.CompleteWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.in.command.CreateWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.in.command.WorkOrderLineItem;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderPort;
import com.bbd.procurement.workorder.application.port.out.WorkOrderNumberGeneratorPort;
import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderLine;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderService implements CreateWorkOrderUseCase, StartWorkOrderUseCase, CompleteWorkOrderUseCase, GetWorkOrderQuery {

    private final SaveWorkOrderPort saveWorkOrderPort;
    private final LoadWorkOrderPort loadWorkOrderPort;
    private final WorkOrderNumberGeneratorPort workOrderNumberGeneratorPort;
    private final LoadItemPort loadItemPort;
    private final SaveOutboxEventPort saveOutboxEventPort;
    private final ObjectMapper objectMapper;
    private final LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;

    @Override
    @Transactional
    public WorkOrder create(CreateWorkOrderCommand command) {
        String number = workOrderNumberGeneratorPort.generate();
        List<WorkOrderLine> lines = toLines(command.lines());

        WorkOrder workOrder = WorkOrder.create(
                number, command.soNumber(), command.warehouseCode(), lines, command.createdBy()
        );
        WorkOrder saved = saveWorkOrderPort.save(workOrder);
        return saved;
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
    public WorkOrder getByWorkOrderNumber(String workOrderNumber) {
        return findOrThrow(workOrderNumber);
    }

    @Override
    public List<WorkOrder> list() {
        return loadWorkOrderPort.findAll();
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
                            item.quantity()
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
                        line.getQuantity(),
                        workOrder.getWarehouseCode(),
                        line.getUnitPrice().intValueExact()
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
