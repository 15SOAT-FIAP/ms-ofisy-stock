package br.com.ofisy.ms_ofisy_stock.application.stock.consume;

import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.InsufficientStockException;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase.RegisterStockMovementCommand;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumeStockServiceTest {

    private static final UUID STOCK_ID = UUID.randomUUID();

    @Mock
    private StockRepository repository;

    @Mock
    private RegisterStockMovementUseCase registerStockMovementUseCase;

    @InjectMocks
    private ConsumeStockService consumeService;

    @Nested
    class Execute {

        @Test
        void shouldConsumeQuantityAndRegisterOutMovement() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = consumeService.execute(new ConsumeStockCommand(STOCK_ID, 4));

            assertThat(result.getQuantity()).isEqualTo(6);
            verify(registerStockMovementUseCase).execute(
                    new RegisterStockMovementCommand(STOCK_ID, MovementType.OUT, 4, 10, 6));
            verify(repository).save(result);
        }

        @Test
        void shouldAllowConsumingAllAvailableQuantity() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = consumeService.execute(new ConsumeStockCommand(STOCK_ID, 10));

            assertThat(result.getQuantity()).isZero();
        }

        @Test
        void shouldThrowInsufficientStockExceptionWhenQuantityIsGreaterThanAvailable() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));

            assertThatThrownBy(() -> consumeService.execute(new ConsumeStockCommand(STOCK_ID, 11)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Estoque insuficiente: ID " + STOCK_ID);

            verify(registerStockMovementUseCase, never()).execute(any());
            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidStockQuantityExceptionWhenQuantityIsNull() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));

            assertThatThrownBy(() -> consumeService.execute(new ConsumeStockCommand(STOCK_ID, null)))
                    .isInstanceOf(InvalidStockQuantityException.class);

            verify(registerStockMovementUseCase, never()).execute(any());
        }

        @Test
        void shouldThrowStockNotFoundExceptionWhenStockDoesNotExist() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consumeService.execute(new ConsumeStockCommand(STOCK_ID, 4)))
                    .isInstanceOf(StockNotFoundException.class);

            verify(registerStockMovementUseCase, never()).execute(any());
            verify(repository, never()).save(any());
        }
    }
}
