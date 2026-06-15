package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.adapter.in.messaging.event.PurchaseRequested;
import com.bbd.procurement.purchaseorder.adapter.out.persistence.PurchaseRequestNotificationJpaRepository;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.shared.inbox.adapter.out.persistence.ProcessedEventJpaRepository;
import com.bbd.procurement.shared.inbox.domain.ProcessedEvent;
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
public class PurchaseRequestNotificationService {

    private final ProcessedEventJpaRepository processedEventJpaRepository;
    private final PurchaseRequestNotificationJpaRepository purchaseRequestNotificationJpaRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(String message) {
        PurchaseRequested event = objectMapper.readValue(message, PurchaseRequested.class);

        if (processedEventJpaRepository.existsByEventId(event.eventId())) {
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
        purchaseRequestNotificationJpaRepository.save(notification);

        processedEventJpaRepository.save(ProcessedEvent.of(event.eventId()));

        log.info("Saved purchase-request notification eventId={} soNumber={}", event.eventId(), event.soNumber());
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestNotification> list() {
        return purchaseRequestNotificationJpaRepository.findAllByOrderByReceivedAtDesc();
    }
}
