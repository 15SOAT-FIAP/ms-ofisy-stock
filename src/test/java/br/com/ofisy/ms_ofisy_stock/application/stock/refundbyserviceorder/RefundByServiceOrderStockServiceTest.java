package br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockService.StockRefundedPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockUseCase.RefundByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase.ReleaseByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase.ReleasedStockItem;
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
class RefundByServiceOrderStockServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID STOCK_ID = UUID.randomUUID();
    private static final RefundByServiceOrderStockCommand COMMAND =
            new RefundByServiceOrderStockCommand(EVENT_ID, "AllExecutionsCancelled", SERVICE_ORDER_ID);
    private static final ReleaseByServiceOrderStockCommand RELEASE_COMMAND =
            new ReleaseByServiceOrderStockCommand(SERVICE_ORDER_ID);

    @Mock
    private ReleaseByServiceOrderStockUseCase releaseByServiceOrderStockUseCase;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private RefundByServiceOrderStockService refundService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    @Nested
    class Execute {

        @Test
        void shouldReleaseStockOfServiceOrder() {
            when(releaseByServiceOrderStockUseCase.execute(RELEASE_COMMAND)).thenReturn(List.of());

            refundService.execute(COMMAND);

            verify(releaseByServiceOrderStockUseCase).execute(RELEASE_COMMAND);
        }

        @Test
        void shouldRecordStockRefundedWithReleasedItems() {
            List<ReleasedStockItem> releasedItems = List.of(new ReleasedStockItem(STOCK_ID, 2));
            when(releaseByServiceOrderStockUseCase.execute(RELEASE_COMMAND)).thenReturn(releasedItems);

            refundService.execute(COMMAND);

            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            OutboxEvent event = outboxEventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo(RefundByServiceOrderStockService.STOCK_REFUNDED);
            assertThat(event.getServiceOrderId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getMessageGroupId()).isEqualTo(SERVICE_ORDER_ID);
            assertThat(event.getPayload()).isEqualTo(new StockRefundedPayload(releasedItems));
        }

        @Test
        void shouldRecordStockRefundedWithEmptyItemsWhenNothingWasReleased() {
            when(releaseByServiceOrderStockUseCase.execute(RELEASE_COMMAND)).thenReturn(List.of());

            refundService.execute(COMMAND);

            verify(outboxEventRepository).save(outboxEventCaptor.capture());
            assertThat(outboxEventCaptor.getValue().getEventType()).isEqualTo(RefundByServiceOrderStockService.STOCK_REFUNDED);
            assertThat(outboxEventCaptor.getValue().getPayload()).isEqualTo(new StockRefundedPayload(List.of()));
        }

        @Test
        void shouldRecordProcessedEvent() {
            when(releaseByServiceOrderStockUseCase.execute(RELEASE_COMMAND)).thenReturn(List.of());

            refundService.execute(COMMAND);

            verify(processedEventRepository).save(processedEventCaptor.capture());
            assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(processedEventCaptor.getValue().getEventType()).isEqualTo("AllExecutionsCancelled");
        }

        @Test
        void shouldSkipWhenEventWasAlreadyProcessed() {
            when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

            refundService.execute(COMMAND);

            verifyNoInteractions(releaseByServiceOrderStockUseCase);
            verifyNoInteractions(outboxEventRepository);
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldNotRecordAnythingWhenReleaseFails() {
            when(releaseByServiceOrderStockUseCase.execute(RELEASE_COMMAND)).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> refundService.execute(COMMAND)).isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(outboxEventRepository);
            verify(processedEventRepository, never()).save(any());
        }
    }
}
