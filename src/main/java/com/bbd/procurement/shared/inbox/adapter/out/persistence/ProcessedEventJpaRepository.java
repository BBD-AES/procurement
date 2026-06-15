package com.bbd.procurement.shared.inbox.adapter.out.persistence;

import com.bbd.procurement.shared.inbox.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventId(String eventId);

}
