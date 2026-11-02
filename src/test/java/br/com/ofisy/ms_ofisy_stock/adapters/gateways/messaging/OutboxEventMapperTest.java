package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventMapperTest {

    @Test
    void shouldConvertDomainToEntity() {
        var outboxEvent = OutboxEvent.create("StockConsumed", UUID.randomUUID(), Map.of());
        var payloadJson = "{\"quoteId\":\"%s\"}".formatted(UUID.randomUUID());

        var result = OutboxEventMapper.toEntity(outboxEvent, payloadJson);

        assertThat(result.getId()).isEqualTo(outboxEvent.getEventId());
        assertThat(result.getEventType()).isEqualTo(outboxEvent.getEventType());
        assertThat(result.getServiceOrderId()).isEqualTo(outboxEvent.getServiceOrderId());
        assertThat(result.getMessageGroupId()).isEqualTo(outboxEvent.getMessageGroupId());
        assertThat(result.getPayload()).isEqualTo(payloadJson);
        assertThat(result.getOccurredAt()).isEqualTo(outboxEvent.getOccurredAt());
        assertThat(result.getPublishedAt()).isNull();
        assertThat(result.isNew()).isTrue();
    }
}
