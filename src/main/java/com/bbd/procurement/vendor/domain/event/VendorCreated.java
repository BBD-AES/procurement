package com.bbd.procurement.vendor.domain.event;

import com.bbd.procurement.shared.outbox.domain.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record VendorCreated(
        UUID eventId,
        String code,
        String name,
        String contact,
        String terms,
        LocalDateTime occurredAt
) implements DomainEvent {

    public static VendorCreated of(String code, String name, String contact, String terms) {
        return new VendorCreated(
                UUID.randomUUID(),
                code,
                name,
                contact,
                terms,
                LocalDateTime.now()
        );
    }

    @Override
    public String aggregateType() {
        return "Vendor";
    }

    @Override
    public String aggregateId() {
        return code;
    }
}

