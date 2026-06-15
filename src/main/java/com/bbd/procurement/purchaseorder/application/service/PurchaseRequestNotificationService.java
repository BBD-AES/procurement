package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.application.port.in.GetPurchaseRequestNotificationQuery;
import com.bbd.procurement.purchaseorder.application.port.in.HandlePurchaseRequestedUseCase;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.SourcingType;
import com.bbd.procurement.shared.inbox.application.port.out.ProcessedEventPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRequestNotificationService implements HandlePurchaseRequestedUseCase, GetPurchaseRequestNotificationQuery {

    private final ProcessedEventPort processedEventPort;
    private final SavePurchaseRequestNotificationPort savePurchaseRequestNotificationPort;
    private final LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort;
    private final SaveWorkOrderRequestNotificationPort saveWorkOrderRequestNotificationPort;
    private final SourcingResolver sourcingResolver;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(String message) {
        PurchaseRequested event = objectMapper.readValue(message, PurchaseRequested.class);

        if (processedEventPort.existsByEventId(event.eventId())) {
            log.info("Skip duplicate purchase-requested eventId={}", event.eventId());
            return;
        }

        List<PurchaseRequested.Line> buyLines = event.lines().stream()
                .filter(line -> sourcingResolver.resolve(line.sku(), line.sourcingType()) == SourcingType.BUY)
                .toList();

        List<PurchaseRequested.Line> makeLines = event.lines().stream()
                .filter(line -> sourcingResolver.resolve(line.sku(), line.sourcingType()) == SourcingType.MAKE)
                .toList();

        LocalDateTime receivedAt = LocalDateTime.now();

        if (!buyLines.isEmpty()) {
            savePurchaseRequestNotificationPort.save(PurchaseRequestNotification.create(
                    event.eventId(), event.soNumber(), event.warehouseCode(), serializeWith(event, buyLines), receivedAt));
        }

        if (!makeLines.isEmpty()) {
            saveWorkOrderRequestNotificationPort.save(WorkOrderRequestNotification.create(
                    event.eventId(), event.soNumber(), event.warehouseCode(), serializeWith(event, makeLines), receivedAt
            ));
        }

        processedEventPort.save(event.eventId());

        log.info("Routed purchase-requested eventId={} soNumber={} buy={} make={}",
                event.eventId(), event.soNumber(), buyLines.size(), makeLines.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestNotification> list() {
        return loadPurchaseRequestNotificationPort.findAllOrderByReceivedAtDesc();
    }

    private String serializeWith(PurchaseRequested event, List<PurchaseRequested.Line> lines) {
        PurchaseRequested filtered = new PurchaseRequested(
                event.eventId(), event.source(), event.eventType(), event.occurredAt(), event.soNumber(), event.warehouseCode(), lines
        );
        return objectMapper.writeValueAsString(filtered);
    }
}
