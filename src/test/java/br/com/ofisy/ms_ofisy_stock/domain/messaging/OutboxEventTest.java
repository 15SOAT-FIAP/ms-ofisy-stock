package br.com.ofisy.ms_ofisy_stock.domain.messaging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Nested
    class CreateOutboxEvent {
        @Test
        void shouldCreateEventGroupedByServiceOrder() {
            var serviceOrderId = UUID.randomUUID();
            var payload = Map.of("quoteId", UUID.randomUUID());

            var result = OutboxEvent.create("StockConsumed", serviceOrderId, payload);

            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getEventType()).isEqualTo("StockConsumed");
            assertThat(result.getServiceOrderId()).isEqualTo(serviceOrderId);
            assertThat(result.getMessageGroupId()).isEqualTo(serviceOrderId);
            assertThat(result.getPayload()).isEqualTo(payload);
            assertThat(result.getOccurredAt()).isNotNull();
        }

        @Test
        void shouldGenerateDifferentIdsForEachEvent() {
            var serviceOrderId = UUID.randomUUID();

            var first = OutboxEvent.create("StockConsumed", serviceOrderId, Map.of());
            var second = OutboxEvent.create("StockConsumed", serviceOrderId, Map.of());

            assertThat(first.getEventId()).isNotEqualTo(second.getEventId());
        }
    }

    @Nested
    class CreateOutboxEventWithoutServiceOrder {
        @Test
        void shouldCreateEventGroupedByInformedIdAndWithoutServiceOrder() {
            var stockId = UUID.randomUUID();

            var result = OutboxEvent.createWithoutServiceOrder("StockLow", stockId, Map.of("stockId", stockId));

            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getEventType()).isEqualTo("StockLow");
            assertThat(result.getServiceOrderId()).isNull();
            assertThat(result.getMessageGroupId()).isEqualTo(stockId);
            assertThat(result.getOccurredAt()).isNotNull();
        }
    }
}
