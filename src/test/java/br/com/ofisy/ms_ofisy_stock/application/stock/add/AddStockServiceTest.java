package br.com.ofisy.ms_ofisy_stock.application.stock.add;

import br.com.ofisy.ms_ofisy_stock.application.stock.add.AddStockUseCase.AddStockCommand;
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
class AddStockServiceTest {

    private static final UUID STOCK_ID = UUID.randomUUID();

    @Mock
    private StockRepository repository;

    @Mock
    private RegisterStockMovementUseCase registerStockMovementUseCase;

    @InjectMocks
    private AddStockService addService;

    @Nested
    class Execute {

        @Test
        void shouldAddQuantityAndRegisterInMovement() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = addService.execute(new AddStockCommand(STOCK_ID, 5));

            assertThat(result.getQuantity()).isEqualTo(15);
            verify(registerStockMovementUseCase).execute(
                    new RegisterStockMovementCommand(STOCK_ID, MovementType.IN, 5, 10, 15));
            verify(repository).save(result);
        }

        @Test
        void shouldThrowStockNotFoundExceptionWhenStockDoesNotExist() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addService.execute(new AddStockCommand(STOCK_ID, 5)))
                    .isInstanceOf(StockNotFoundException.class);

            verify(registerStockMovementUseCase, never()).execute(any());
            verify(repository, never()).save(any());
        }

        @Test
        void shouldNotRegisterMovementWhenQuantityIsInvalid() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));

            assertThatThrownBy(() -> addService.execute(new AddStockCommand(STOCK_ID, 0)))
                    .isInstanceOf(InvalidStockQuantityException.class);

            verify(registerStockMovementUseCase, never()).execute(any());
            verify(repository, never()).save(any());
        }
    }
}
