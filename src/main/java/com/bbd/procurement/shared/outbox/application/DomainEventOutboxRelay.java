package com.bbd.procurement.shared.outbox.application;

import com.bbd.procurement.purchaseorder.domain.event.StockInRequested;
import com.bbd.procurement.shared.outbox.adapter.out.persistence.OutboxEventJpaRepository;
import com.bbd.procurement.shared.outbox.domain.DomainEvent;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DomainEventOutboxRelay {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(DomainEvent event) {
        String payload = serialize(event);

        OutboxEvent outboxEvent = OutboxEvent.create(
                StockInRequested.TOPIC,
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                payload,
                event.occurredAt()
        );

        outboxEventJpaRepository.save(outboxEvent);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                    "Failed to serialize domain event: " +
                            event.getClass().getName(), e
            );
        }
    }
}
