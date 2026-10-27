package br.com.ofisy.ms_ofisy_stock.application.stockmovement.register;

import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase.RegisterStockMovementCommand;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStockMovementServiceTest {

    @Mock
    private StockMovementRepository repository;

    @InjectMocks
    private RegisterStockMovementService registerService;

    @Nested
    class Execute {

        @Test
        void shouldCreateAndSaveMovementWithCommandData() {
            var stockId = UUID.randomUUID();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockMovement result = registerService.execute(
                    new RegisterStockMovementCommand(stockId, MovementType.OUT, 4, 10, 6));

            assertThat(result.getStockId()).isEqualTo(stockId);
            assertThat(result.getMovementType()).isEqualTo(MovementType.OUT);
            assertThat(result.getQuantity()).isEqualTo(4);
            assertThat(result.getPreviousQuantity()).isEqualTo(10);
            assertThat(result.getNewQuantity()).isEqualTo(6);
            assertThat(result.getCreatedAt()).isNotNull();
            verify(repository).save(result);
        }
    }
}
