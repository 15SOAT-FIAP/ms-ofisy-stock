package br.com.ofisy.ms_ofisy_stock.domain.messaging;

import java.util.UUID;

public interface OutboxEventRepository {

    void save(OutboxEvent outboxEvent);

    boolean existsByServiceOrderIdAndEventType(UUID serviceOrderId, String eventType);
}
