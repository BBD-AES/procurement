package com.bbd.procurement.shared.outbox.adapter.out.persistence;

import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByProcessedAtIsNullOrderByOccurredAtAsc(Pageable pageable);
}
