package br.com.ofisy.ms_ofisy_stock.domain.messaging;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class OutboxEvent {

    private final UUID eventId;
    private final String eventType;
    private final UUID serviceOrderId;
    private final UUID messageGroupId;
    private final Object payload;
    private final Instant occurredAt;

    private OutboxEvent(UUID eventId, String eventType, UUID serviceOrderId, UUID messageGroupId, Object payload,
                        Instant occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.serviceOrderId = serviceOrderId;
        this.messageGroupId = messageGroupId;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public static OutboxEvent create(String eventType, UUID serviceOrderId, Object payload) {
        return new OutboxEvent(UUID.randomUUID(), eventType, serviceOrderId, serviceOrderId, payload, Instant.now());
    }

    // Para eventos que não pertencem a uma OS, como o StockLow, que é agrupado pelo id do estoque
    public static OutboxEvent createWithoutServiceOrder(String eventType, UUID messageGroupId, Object payload) {
        return new OutboxEvent(UUID.randomUUID(), eventType, null, messageGroupId, payload, Instant.now());
    }
}
