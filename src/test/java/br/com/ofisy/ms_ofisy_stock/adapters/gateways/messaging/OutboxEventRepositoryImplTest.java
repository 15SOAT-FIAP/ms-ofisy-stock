package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.adapters.tracing.TraceContextPropagator;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryImplTest {

    private static final String TRACEPARENT = "00-6af793d30000000025cb7731d653ab37-6d0c2e20f86d8721-01";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private JpaOutboxEventRepository jpa;

    @Mock
    private TraceContextPropagator traceContextPropagator;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> entityCaptor;

    private OutboxEventRepositoryImpl repository;

    record StockConsumedPayload(UUID quoteId) {}

    @BeforeEach
    void setUp() {
        repository = new OutboxEventRepositoryImpl(jpa, jsonMapper, traceContextPropagator);
    }

    @Test
    void shouldSaveOutboxEventWithPayloadSerializedAsJson() {
        var quoteId = UUID.randomUUID();
        var outboxEvent = OutboxEvent.create("StockConsumed", UUID.randomUUID(), new StockConsumedPayload(quoteId));
        when(traceContextPropagator.captureCurrent()).thenReturn(Map.of());

        repository.save(outboxEvent);

        verify(jpa).save(entityCaptor.capture());
        var entity = entityCaptor.getValue();
        assertThat(entity.getEventId()).isEqualTo(outboxEvent.getEventId());
        assertThat(entity.getPayload()).isEqualTo("{\"quoteId\":\"%s\"}".formatted(quoteId));
    }

    @Test
    void shouldSaveCurrentTraceContextSerializedAsJson() {
        var outboxEvent = OutboxEvent.create("StockConsumed", UUID.randomUUID(), Map.of());
        when(traceContextPropagator.captureCurrent()).thenReturn(Map.of("traceparent", TRACEPARENT));

        repository.save(outboxEvent);

        verify(jpa).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getTraceContext()).isEqualTo("{\"traceparent\":\"%s\"}".formatted(TRACEPARENT));
    }

    @Test
    void shouldSaveWithoutTraceContextWhenThereIsNoActiveTrace() {
        var outboxEvent = OutboxEvent.create("StockConsumed", UUID.randomUUID(), Map.of());
        when(traceContextPropagator.captureCurrent()).thenReturn(Map.of());

        repository.save(outboxEvent);

        verify(jpa).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getTraceContext()).isNull();
    }

    @Test
    void shouldCheckWhetherServiceOrderHasEventOfType() {
        var serviceOrderId = UUID.randomUUID();
        when(jpa.existsByServiceOrderIdAndEventType(serviceOrderId, "StockRefunded")).thenReturn(true);

        assertThat(repository.existsByServiceOrderIdAndEventType(serviceOrderId, "StockRefunded")).isTrue();
    }
}
