package com.bbd.procurement.shared.inbox.adapter.out.persistence;

import com.bbd.procurement.shared.inbox.application.port.out.ProcessedEventPort;
import com.bbd.procurement.shared.inbox.domain.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessedEventPersistenceAdapter implements ProcessedEventPort {

    private final ProcessedEventJpaRepository processedEventJpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return processedEventJpaRepository.existsByEventId(eventId);
    }

    @Override
    public void save(String eventId) {
        processedEventJpaRepository.save(ProcessedEvent.of(eventId));
    }
}
