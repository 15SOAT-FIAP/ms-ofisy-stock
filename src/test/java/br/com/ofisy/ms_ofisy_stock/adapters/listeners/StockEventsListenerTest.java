package br.com.ofisy.ms_ofisy_stock.adapters.listeners;

import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockEventsListenerTest {

    @Mock
    private EventHandler quoteCreatedHandler;

    @Mock
    private EventHandler otherHandler;

    private StockEventsListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(quoteCreatedHandler.type()).thenReturn("QuoteCreated");
        lenient().when(otherHandler.type()).thenReturn("AllExecutionsCancelled");
        listener = new StockEventsListener(List.of(quoteCreatedHandler, otherHandler));
    }

    private EventEnvelope envelope(String type) {
        return new EventEnvelope(UUID.randomUUID(), type, UUID.randomUUID(), Instant.parse("2026-10-13T10:00:00Z"), null);
    }

    @Nested
    class Constructor {

        @Test
        void shouldFailWhenTwoHandlersDeclareTheSameType() {
            when(otherHandler.type()).thenReturn("QuoteCreated");

            assertThatThrownBy(() -> new StockEventsListener(List.of(quoteCreatedHandler, otherHandler)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void shouldAcceptNoHandlers() {
            StockEventsListener emptyListener = new StockEventsListener(List.of());

            assertThatCode(() -> emptyListener.onMessage(envelope("QuoteCreated"))).doesNotThrowAnyException();
        }
    }

    @Nested
    class OnMessage {

        @Test
        void shouldDispatchEventToHandlerOfItsType() {
            EventEnvelope envelope = envelope("QuoteCreated");

            listener.onMessage(envelope);

            verify(quoteCreatedHandler).handle(envelope);
            verify(otherHandler, never()).handle(any());
        }

        @Test
        void shouldIgnoreEventWithoutHandler() {
            listener.onMessage(envelope("QuoteApproved"));

            verify(quoteCreatedHandler, never()).handle(any());
            verify(otherHandler, never()).handle(any());
        }

        @Test
        void shouldPropagateHandlerFailure() {
            EventEnvelope envelope = envelope("QuoteCreated");
            doThrow(new IllegalStateException("boom")).when(quoteCreatedHandler).handle(envelope);

            assertThatThrownBy(() -> listener.onMessage(envelope))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");
        }
    }
}
