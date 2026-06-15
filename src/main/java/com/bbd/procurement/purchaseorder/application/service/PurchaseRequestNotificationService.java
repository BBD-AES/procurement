package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.application.port.in.GetPurchaseRequestNotificationQuery;
import com.bbd.procurement.purchaseorder.application.port.in.HandlePurchaseRequestedUseCase;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.shared.inbox.application.port.out.ProcessedEventPort;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(String message) {
        PurchaseRequested event = objectMapper.readValue(message, PurchaseRequested.class);

        if (processedEventPort.existsByEventId(event.eventId())) {
            log.info("Skip duplicate purchase-requested eventId={}", event.eventId());
            return;
        }

        PurchaseRequestNotification notification = PurchaseRequestNotification.create(
                event.eventId(),
                event.soNumber(),
                event.warehouseCode(),
                message,
                LocalDateTime.now()
        );
        savePurchaseRequestNotificationPort.save(notification);

        processedEventPort.save(event.eventId());

        log.info("Saved purchase-request notification eventId={} soNumber={}", event.eventId(), event.soNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestNotification> list() {
        return loadPurchaseRequestNotificationPort.findAllOrderByReceivedAtDesc();
    }
}
