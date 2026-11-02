package br.com.ofisy.ms_ofisy_stock.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockUseCase.RefundByServiceOrderStockCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServiceOrderCancelledHandlerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();

    @Mock
    private RefundByServiceOrderStockUseCase refundByServiceOrderStockUseCase;

    @InjectMocks
    private ServiceOrderCancelledHandler handler;

    @Test
    void shouldHandleServiceOrderCancelledType() {
        assertThat(handler.type()).isEqualTo("ServiceOrderCancelled");
    }

    @Nested
    class Handle {

        @Test
        void shouldMapEnvelopeToRefundCommand() {
            EventEnvelope envelope = new EventEnvelope(EVENT_ID, ServiceOrderCancelledHandler.TYPE, SERVICE_ORDER_ID,
                    Instant.parse("2026-10-20T10:00:00Z"), JsonMapper.builder().build().readTree("{}"));

            handler.handle(envelope);

            verify(refundByServiceOrderStockUseCase).execute(
                    new RefundByServiceOrderStockCommand(EVENT_ID, ServiceOrderCancelledHandler.TYPE, SERVICE_ORDER_ID));
        }
    }
}
