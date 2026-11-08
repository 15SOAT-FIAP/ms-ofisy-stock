package br.com.ofisy.ms_ofisy_stock.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote.AdjustByQuoteStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote.AdjustByQuoteStockUseCase.AdjustByQuoteStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QuoteUpdatedHandlerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID STOCK_ID = UUID.randomUUID();
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Mock
    private AdjustByQuoteStockUseCase adjustByQuoteStockUseCase;

    private QuoteUpdatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new QuoteUpdatedHandler(adjustByQuoteStockUseCase, JSON_MAPPER);
    }

    private EventEnvelope envelope(UUID serviceOrderId, String payload) {
        return new EventEnvelope(EVENT_ID, QuoteUpdatedHandler.TYPE, serviceOrderId,
                Instant.parse("2026-11-02T10:00:00Z"), payload == null ? null : JSON_MAPPER.readTree(payload));
    }

    @Test
    void shouldHandleQuoteUpdatedType() {
        assertThat(handler.type()).isEqualTo("QuoteUpdated");
    }

    @Nested
    class Handle {

        @Test
        void shouldMapEnvelopeAndPayloadToAdjustCommand() {
            handler.handle(envelope(SERVICE_ORDER_ID, """
                    { "quoteId": "%s", "totalPrice": 150.00,
                      "stockItems": [ { "stockId": "%s", "quantity": 2, "unitPrice": 50.00 } ] }
                    """.formatted(QUOTE_ID, STOCK_ID)));

            verify(adjustByQuoteStockUseCase).execute(new AdjustByQuoteStockCommand(EVENT_ID,
                    QuoteUpdatedHandler.TYPE, SERVICE_ORDER_ID, QUOTE_ID, List.of(new StockItem(STOCK_ID, 2))));
        }

        @Test
        void shouldAcceptEmptyStockItemsWhenEveryPartLeftTheQuote() {
            handler.handle(envelope(SERVICE_ORDER_ID, """
                    { "quoteId": "%s", "stockItems": [] }
                    """.formatted(QUOTE_ID)));

            verify(adjustByQuoteStockUseCase).execute(new AdjustByQuoteStockCommand(EVENT_ID,
                    QuoteUpdatedHandler.TYPE, SERVICE_ORDER_ID, QUOTE_ID, List.of()));
        }

        @Test
        void shouldRejectEventWithoutServiceOrder() {
            assertThatThrownBy(() -> handler.handle(envelope(null, """
                    { "quoteId": "%s", "stockItems": [] }
                    """.formatted(QUOTE_ID)))).isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(adjustByQuoteStockUseCase);
        }

        @Test
        void shouldRejectEventWithoutPayload() {
            assertThatThrownBy(() -> handler.handle(envelope(SERVICE_ORDER_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(adjustByQuoteStockUseCase);
        }

        @Test
        void shouldRejectEventWithoutQuoteId() {
            assertThatThrownBy(() -> handler.handle(envelope(SERVICE_ORDER_ID, """
                    { "stockItems": [] }
                    """))).isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(adjustByQuoteStockUseCase);
        }

        @Test
        void shouldRejectEventWithoutStockItems() {
            assertThatThrownBy(() -> handler.handle(envelope(SERVICE_ORDER_ID, """
                    { "quoteId": "%s" }
                    """.formatted(QUOTE_ID)))).isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(adjustByQuoteStockUseCase);
        }
    }
}
