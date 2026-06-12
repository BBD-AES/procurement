package com.bbd.procurement.shared.outbox.adapter.out.messaging;

import com.bbd.procurement.shared.outbox.adapter.out.persistence.OutboxEventJpaRepository;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelay {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxEventJpaRepository
                .findByProcessedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, BATCH_SIZE));

        if (batch.isEmpty()) {
            return;
        }

        log.info("Relaying {} outbox events", batch.size());

        for (OutboxEvent event : batch) {
            try {
                publish(event);
                event.markProcessed();
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={} eventId={}",
                        event.getId(), event.getEventId(), e);
            }
        }
    }

    private void publish(OutboxEvent event) {
        kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload());
        log.info("Publishing event: eventId={} topic={} key={}",
                event.getEventId(),
                event.getTopic(),
                event.getAggregateId());
    }
}
