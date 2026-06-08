package com.bbd.procurement.shared.outbox.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    String aggregateType();

    String aggregateId();

    LocalDateTime occurredAt();

    default String eventType() {
        return getClass().getSimpleName();
    }

}
