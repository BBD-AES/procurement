package com.bbd.procurement.shared.inbox.application.port.out;

public interface ProcessedEventPort {

    boolean existsByEventId(String eventId);

    void save(String eventId);

}
