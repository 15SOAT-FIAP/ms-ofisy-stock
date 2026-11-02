package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventMapperTest {

    @Test
    void shouldConvertEntityToDomain() {
        var eventId = UUID.randomUUID();
        var processedAt = LocalDateTime.of(2026, 10, 13, 10, 0);
        var entity = ProcessedEventEntity.builder()
                .eventId(eventId)
                .eventType("QuoteCreated")
                .processedAt(processedAt)
                .build();

        var result = ProcessedEventMapper.toDomain(entity);

        assertThat(result.getEventId()).isEqualTo(eventId);
        assertThat(result.getEventType()).isEqualTo("QuoteCreated");
        assertThat(result.getProcessedAt()).isEqualTo(processedAt);
    }

    @Test
    void shouldConvertDomainToEntity() {
        var processedEvent = ProcessedEvent.create(UUID.randomUUID(), "QuoteCreated");

        var result = ProcessedEventMapper.toEntity(processedEvent);

        assertThat(result.getId()).isEqualTo(processedEvent.getEventId());
        assertThat(result.getEventType()).isEqualTo(processedEvent.getEventType());
        assertThat(result.getProcessedAt()).isEqualTo(processedEvent.getProcessedAt());
        assertThat(result.isNew()).isTrue();
    }
}
