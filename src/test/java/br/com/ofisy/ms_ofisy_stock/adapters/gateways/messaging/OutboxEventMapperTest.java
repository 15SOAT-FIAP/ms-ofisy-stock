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

        var traceContextJson = "{\"traceparent\":\"00-6af793d30000000025cb7731d653ab37-6d0c2e20f86d8721-01\"}";

        var result = OutboxEventMapper.toEntity(outboxEvent, payloadJson, traceContextJson);

        assertThat(result.getId()).isEqualTo(outboxEvent.getEventId());
        assertThat(result.getEventType()).isEqualTo(outboxEvent.getEventType());
        assertThat(result.getServiceOrderId()).isEqualTo(outboxEvent.getServiceOrderId());
        assertThat(result.getMessageGroupId()).isEqualTo(outboxEvent.getMessageGroupId());
        assertThat(result.getPayload()).isEqualTo(payloadJson);
        assertThat(result.getOccurredAt()).isEqualTo(outboxEvent.getOccurredAt());
        assertThat(result.getTraceContext()).isEqualTo(traceContextJson);
        assertThat(result.getPublishedAt()).isNull();
        assertThat(result.isNew()).isTrue();
    }
}
