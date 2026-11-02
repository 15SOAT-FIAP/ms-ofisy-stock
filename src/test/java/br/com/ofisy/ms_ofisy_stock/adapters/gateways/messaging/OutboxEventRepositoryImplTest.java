package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryImplTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private JpaOutboxEventRepository jpa;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> entityCaptor;

    private OutboxEventRepositoryImpl repository;

    record StockConsumedPayload(UUID quoteId) {}

    @BeforeEach
    void setUp() {
        repository = new OutboxEventRepositoryImpl(jpa, jsonMapper);
    }

    @Test
    void shouldSaveOutboxEventWithPayloadSerializedAsJson() {
        var quoteId = UUID.randomUUID();
        var outboxEvent = OutboxEvent.create("StockConsumed", UUID.randomUUID(), new StockConsumedPayload(quoteId));

        repository.save(outboxEvent);

        verify(jpa).save(entityCaptor.capture());
        var entity = entityCaptor.getValue();
        assertThat(entity.getEventId()).isEqualTo(outboxEvent.getEventId());
        assertThat(entity.getPayload()).isEqualTo("{\"quoteId\":\"%s\"}".formatted(quoteId));
    }
}
