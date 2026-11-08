package br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote;

import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote.AdjustByQuoteStockUseCase.AdjustByQuoteStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase.AdjustByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase.AdjustByServiceOrderStockResult;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.StockConsumedPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.StockConsumptionFailedPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockService;
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
class AdjustByQuoteStockServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID QUOTE_ID = UUID.randomUUID();
    private static final UUID STOCK_ID = UUID.randomUUID();
    private static final List<StockItem> ITEMS = List.of(new StockItem(STOCK_ID, 3));
    private static final AdjustByQuoteStockCommand COMMAND =
            new AdjustByQuoteStockCommand(EVENT_ID, "QuoteUpdated", SERVICE_ORDER_ID, QUOTE_ID, ITEMS);
    private static final AdjustByServiceOrderStockCommand ADJUST_COMMAND =
            new AdjustByServiceOrderStockCommand(SERVICE_ORDER_ID, ITEMS);

    @Mock
    private AdjustByServiceOrderStockUseCase adjustByServiceOrderStockUseCase;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private AdjustByQuoteStockService adjustByQuoteService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    @Nested
    class Execute {

        @Test
        void shouldRecordStockConsumedWithFinalItemsWhenAdjustmentSucceeds() {
            when(adjustByServiceOrderStockUseCase.execute(ADJUST_COMMAND))
                    .thenReturn(new AdjustByServiceOrderStockResult(List.of()));

            adjustByQuoteService.execute(COMMAND);

            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            OutboxEvent event = outboxEventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo(ConsumeByQuoteStockService.STOCK_CONSUMED);
            assertThat(event.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getPayload()).isEqualTo(new StockConsumedPayload(QUOTE_ID, ITEMS));
        }

        @Test
        void shouldRecordStockConsumptionFailedWithMissingItems() {
            List<MissingStockItem> missingItems = List.of(new MissingStockItem(STOCK_ID, 1, 0));
            when(adjustByServiceOrderStockUseCase.execute(ADJUST_COMMAND))
                    .thenReturn(new AdjustByServiceOrderStockResult(missingItems));

            adjustByQuoteService.execute(COMMAND);

            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            OutboxEvent event = outboxEventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo(ConsumeByQuoteStockService.STOCK_CONSUMPTION_FAILED);
            assertThat(event.getPayload()).isEqualTo(new StockConsumptionFailedPayload(QUOTE_ID, missingItems));
        }

        @Test
        void shouldRecordProcessedEvent() {
            when(adjustByServiceOrderStockUseCase.execute(ADJUST_COMMAND))
                    .thenReturn(new AdjustByServiceOrderStockResult(List.of()));

            adjustByQuoteService.execute(COMMAND);

            verify(processedEventRepository).save(processedEventCaptor.capture());
            assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(processedEventCaptor.getValue().getEventType()).isEqualTo("QuoteUpdated");
        }

        @Test
        void shouldIgnoreEventWhenServiceOrderWasAlreadyRefunded() {
            when(outboxEventRepository.existsByServiceOrderIdAndEventType(
                    SERVICE_ORDER_ID, RefundByServiceOrderStockService.STOCK_REFUNDED)).thenReturn(true);

            adjustByQuoteService.execute(COMMAND);

            verifyNoInteractions(adjustByServiceOrderStockUseCase);
            verify(outboxEventRepository, never()).save(any());
            verify(processedEventRepository).save(processedEventCaptor.capture());
            assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
        }

        @Test
        void shouldSkipWhenEventWasAlreadyProcessed() {
            when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

            adjustByQuoteService.execute(COMMAND);

            verifyNoInteractions(adjustByServiceOrderStockUseCase);
            verifyNoInteractions(outboxEventRepository);
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldNotRecordAnythingWhenAdjustmentFails() {
            when(adjustByServiceOrderStockUseCase.execute(ADJUST_COMMAND)).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> adjustByQuoteService.execute(COMMAND)).isInstanceOf(IllegalStateException.class);

            verify(outboxEventRepository, never()).save(any());
            verify(processedEventRepository, never()).save(any());
        }
    }
}
