package com.bbd.procurement.shared.outbox.application.port;

import com.bbd.procurement.shared.outbox.domain.OutboxEvent;

public interface SaveOutboxEventPort {

    void save(OutboxEvent event);

}
