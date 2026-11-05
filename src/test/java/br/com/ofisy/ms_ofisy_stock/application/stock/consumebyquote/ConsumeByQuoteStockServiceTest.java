package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote;

import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.StockConsumedPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.StockConsumptionFailedPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockUseCase.ConsumeByQuoteStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockResult;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEventRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumeByQuoteStockServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID STOCK_ID = UUID.randomUUID();
    private static final List<StockItem> ITEMS = List.of(new StockItem(STOCK_ID, 2));
    private static final ConsumeByQuoteStockCommand COMMAND =
            new ConsumeByQuoteStockCommand(EVENT_ID, "QuoteCreated", SERVICE_ORDER_ID, QUOTE_ID, ITEMS);
    private static final ConsumeByServiceOrderStockCommand CONSUME_COMMAND =
            new ConsumeByServiceOrderStockCommand(SERVICE_ORDER_ID, ITEMS);

    @Mock
    private ConsumeByServiceOrderStockUseCase consumeByServiceOrderStockUseCase;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ConsumeByQuoteStockService consumeByQuoteService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    @Nested
    class Execute {

        @Test
        void shouldRecordStockConsumedWhenEveryItemWasConsumed() {
            when(consumeByServiceOrderStockUseCase.execute(CONSUME_COMMAND))
                    .thenReturn(new ConsumeByServiceOrderStockResult(List.of(), List.of()));

            consumeByQuoteService.execute(COMMAND);

            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            OutboxEvent event = outboxEventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo(ConsumeByQuoteStockService.STOCK_CONSUMED);
            assertThat(event.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getMessageGroupId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getPayload()).isEqualTo(new StockConsumedPayload(QUOTE_ID, ITEMS));
        }

        @Test
        void shouldRecordStockConsumptionFailedWithMissingItems() {
            List<MissingStockItem> missingItems = List.of(new MissingStockItem(STOCK_ID, 2, 1));
            when(consumeByServiceOrderStockUseCase.execute(CONSUME_COMMAND))
                    .thenReturn(new ConsumeByServiceOrderStockResult(List.of(), missingItems));

            consumeByQuoteService.execute(COMMAND);

            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            OutboxEvent event = outboxEventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo(ConsumeByQuoteStockService.STOCK_CONSUMPTION_FAILED);
            assertThat(event.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getPayload()).isEqualTo(new StockConsumptionFailedPayload(QUOTE_ID, missingItems));
        }

        @Test
        void shouldRecordProcessedEvent() {
            when(consumeByServiceOrderStockUseCase.execute(CONSUME_COMMAND))
                    .thenReturn(new ConsumeByServiceOrderStockResult(List.of(), List.of()));

            consumeByQuoteService.execute(COMMAND);

            verify(processedEventRepository).save(processedEventCaptor.capture());
            assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(processedEventCaptor.getValue().getEventType()).isEqualTo("QuoteCreated");
        }

        @Test
        void shouldSkipWhenEventWasAlreadyProcessed() {
            when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

            consumeByQuoteService.execute(COMMAND);

            verifyNoInteractions(consumeByServiceOrderStockUseCase);
            verifyNoInteractions(outboxEventRepository);
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldNotRecordAnythingWhenConsumptionFails() {
            when(consumeByServiceOrderStockUseCase.execute(CONSUME_COMMAND)).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> consumeByQuoteService.execute(COMMAND)).isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(outboxEventRepository);
            verify(processedEventRepository, never()).save(any());
        }
    }
}
