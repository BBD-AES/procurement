package com.bbd.procurement.shared.outbox.adapter.out.persistence;

import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPersistenceAdapter implements SaveOutboxEventPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public void save(OutboxEvent event) {
        outboxEventJpaRepository.save(event);
    }
}
