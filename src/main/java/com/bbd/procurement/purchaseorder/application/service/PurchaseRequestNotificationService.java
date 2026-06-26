package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.application.port.in.ClaimPurchaseRequestNotificationUseCase;
import com.bbd.procurement.purchaseorder.application.port.in.GetPurchaseRequestNotificationQuery;
import com.bbd.procurement.purchaseorder.application.port.in.HandlePurchaseRequestedUseCase;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotificationLine;
import com.bbd.procurement.purchaseorder.domain.SourcingType;
import com.bbd.procurement.shared.inbox.application.port.out.ProcessedEventPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotificationLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRequestNotificationService implements HandlePurchaseRequestedUseCase, GetPurchaseRequestNotificationQuery, ClaimPurchaseRequestNotificationUseCase {

    /** 이 시간보다 오래된 클레임은 만료로 보고 다른 담당자가 takeover 할 수 있다. */
    private static final Duration CLAIM_TTL = Duration.ofMinutes(30);

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

        Map<SourcingType, List<PurchaseRequested.Line>> routed = sourcingResolver.resolveAll(event.lines());
        List<PurchaseRequested.Line> buyLines = routed.getOrDefault(SourcingType.BUY, List.of());
        List<PurchaseRequested.Line> makeLines = routed.getOrDefault(SourcingType.MAKE, List.of());

        LocalDateTime receivedAt = LocalDateTime.now();

        if (!buyLines.isEmpty()) {
            List<PurchaseRequestNotificationLine> notificationLines = buyLines.stream()
                    .map(line -> PurchaseRequestNotificationLine.create(line.sku(), line.quantity()))
                    .toList();
            savePurchaseRequestNotificationPort.save(PurchaseRequestNotification.create(
                    event.eventId(), event.soNumber(), event.warehouseCode(),
                    serializeWith(event, buyLines), receivedAt, notificationLines));
        }

        if (!makeLines.isEmpty()) {
            List<WorkOrderRequestNotificationLine> notificationLines = makeLines.stream()
                    .map(line -> WorkOrderRequestNotificationLine.create(line.sku(), line.quantity()))
                    .toList();
            saveWorkOrderRequestNotificationPort.save(WorkOrderRequestNotification.create(
                    event.eventId(), event.soNumber(), event.warehouseCode(),
                    serializeWith(event, makeLines), receivedAt, notificationLines));
        }

        processedEventPort.save(event.eventId());

        log.info("Routed purchase-requested eventId={} soNumber={} buy={} make={}",
                event.eventId(), event.soNumber(), buyLines.size(), makeLines.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestNotification> list() {
        return loadPurchaseRequestNotificationPort.findActiveOrderByReceivedAtDesc();
    }

    @Override
    @Transactional
    public PurchaseRequestNotification claim(String eventId, Long userId, String userName) {
        PurchaseRequestNotification notification = loadPurchaseRequestNotificationPort.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOTIFICATION_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        notification.claim(userId, userName, now, now.minus(CLAIM_TTL));
        savePurchaseRequestNotificationPort.save(notification);
        return notification;
    }

    @Override
    @Transactional
    public PurchaseRequestNotification release(String eventId, Long userId) {
        PurchaseRequestNotification notification = loadPurchaseRequestNotificationPort.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOTIFICATION_NOT_FOUND));
        notification.releaseClaim(userId);
        savePurchaseRequestNotificationPort.save(notification);
        return notification;
    }

    private String serializeWith(PurchaseRequested event, List<PurchaseRequested.Line> lines) {
        PurchaseRequested filtered = new PurchaseRequested(
                event.eventId(), event.source(), event.eventType(), event.occurredAt(), event.soNumber(), event.warehouseCode(), lines
        );
        return objectMapper.writeValueAsString(filtered);
    }
}
