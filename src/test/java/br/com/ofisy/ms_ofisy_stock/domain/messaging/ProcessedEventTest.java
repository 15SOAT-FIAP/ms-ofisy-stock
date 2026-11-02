package br.com.ofisy.ms_ofisy_stock.domain.messaging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventTest {

    @Nested
    class CreateProcessedEvent {
        @Test
        void shouldCreateProcessedEventWithProcessedAt() {
            var eventId = UUID.randomUUID();

            var result = ProcessedEvent.create(eventId, "QuoteCreated");

            assertThat(result.getEventId()).isEqualTo(eventId);
            assertThat(result.getEventType()).isEqualTo("QuoteCreated");
            assertThat(result.getProcessedAt()).isNotNull();
        }
    }

    @Nested
    class ReconstructProcessedEvent {
        @Test
        void shouldKeepAllFields() {
            var eventId = UUID.randomUUID();
            var processedAt = LocalDateTime.of(2026, 10, 13, 10, 0);

            var result = ProcessedEvent.reconstruct(eventId, "QuoteCreated", processedAt);

            assertThat(result.getEventId()).isEqualTo(eventId);
            assertThat(result.getEventType()).isEqualTo("QuoteCreated");
            assertThat(result.getProcessedAt()).isEqualTo(processedAt);
        }
    }
}
