package br.com.ofisy.ms_ofisy_stock.application.stock.consume;

import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockService.StockLowPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.InsufficientStockException;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase.RegisterStockMovementCommand;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static br.com.ofisy.ms_ofisy_stock.application.stock.StockFixture.persistedStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumeStockServiceTest {

    private static final UUID STOCK_ID = UUID.randomUUID();

    @Mock
    private StockRepository repository;

    @Mock
    private RegisterStockMovementUseCase registerStockMovementUseCase;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ConsumeStockService consumeService;

    @Nested
    class StockLow {

        @Test
        void shouldRecordStockLowWhenBalanceReachesMinThreshold() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumeService.execute(new ConsumeStockCommand(STOCK_ID, 5, UUID.randomUUID()));

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());
            OutboxEvent event = captor.getValue();
            assertThat(event.getEventType()).isEqualTo(ConsumeStockService.STOCK_LOW);
            assertThat(event.getServiceOrderId()).isNull();
            assertThat(event.getMessageGroupId()).isEqualTo(STOCK_ID);
            assertThat(event.getPayload()).isEqualTo(new StockLowPayload(STOCK_ID, "Radiador", 5, 5));
        }

        @Test
        void shouldRecordStockLowOnEveryConsumptionWhileBalanceIsBelowMinThreshold() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 3)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumeService.execute(new ConsumeStockCommand(STOCK_ID, 1));

            verify(outboxEventRepository).save(any());
        }

        @Test
        void shouldNotRecordStockLowWhenBalanceStaysAboveMinThreshold() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumeService.execute(new ConsumeStockCommand(STOCK_ID, 4));

            verifyNoInteractions(outboxEventRepository);
        }

        @Test
        void shouldNotRecordStockLowWhenConsumptionFails() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 2)));

            assertThatThrownBy(() -> consumeService.execute(new ConsumeStockCommand(STOCK_ID, 3)))
                    .isInstanceOf(InsufficientStockException.class);

            verifyNoInteractions(outboxEventRepository);
        }
    }

    @Nested
    class Execute {

        @Test
        void shouldConsumeQuantityAndRegisterOutMovement() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = consumeService.execute(new ConsumeStockCommand(STOCK_ID, 4));

            assertThat(result.getQuantity()).isEqualTo(6);
            verify(registerStockMovementUseCase).execute(
                    new RegisterStockMovementCommand(STOCK_ID, MovementType.OUT, 4, 10, 6));
            verify(repository).save(result);
        }

        @Test
        void shouldRegisterOutMovementLinkedToServiceOrder() {
            var serviceOrderId = UUID.randomUUID();
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumeService.execute(new ConsumeStockCommand(STOCK_ID, 4, serviceOrderId));

            verify(registerStockMovementUseCase).execute(
                    new RegisterStockMovementCommand(STOCK_ID, MovementType.OUT, 4, 10, 6, serviceOrderId));
        }

        @Test
        void shouldAllowConsumingAllAvailableQuantity() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = consumeService.execute(new ConsumeStockCommand(STOCK_ID, 10));

            assertThat(result.getQuantity()).isZero();
        }

        @Test
        void shouldThrowInsufficientStockExceptionWhenQuantityIsGreaterThanAvailable() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));

            assertThatThrownBy(() -> consumeService.execute(new ConsumeStockCommand(STOCK_ID, 11)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Estoque insuficiente: ID " + STOCK_ID);

            verify(registerStockMovementUseCase, never()).execute(any());
            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidStockQuantityExceptionWhenQuantityIsNull() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));

            assertThatThrownBy(() -> consumeService.execute(new ConsumeStockCommand(STOCK_ID, null)))
                    .isInstanceOf(InvalidStockQuantityException.class);

            verify(registerStockMovementUseCase, never()).execute(any());
        }

        @Test
        void shouldThrowStockNotFoundExceptionWhenStockDoesNotExist() {
            when(repository.lockById(STOCK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consumeService.execute(new ConsumeStockCommand(STOCK_ID, 4)))
                    .isInstanceOf(StockNotFoundException.class);

            verify(registerStockMovementUseCase, never()).execute(any());
            verify(repository, never()).save(any());
        }
    }
}
