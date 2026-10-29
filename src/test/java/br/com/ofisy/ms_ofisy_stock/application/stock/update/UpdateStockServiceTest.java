package br.com.ofisy.ms_ofisy_stock.application.stock.update;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockAlreadyExistsException;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.application.stock.update.UpdateStockUseCase.UpdateStockCommand;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static br.com.ofisy.ms_ofisy_stock.application.stock.StockFixture.persistedStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateStockServiceTest {

    private static final UUID STOCK_ID = UUID.randomUUID();

    @Mock
    private StockRepository repository;

    @InjectMocks
    private UpdateStockService updateService;

    @Nested
    class Execute {

        @Test
        void shouldUpdateStockWithCommandData() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.findByProductName("Radiador novo")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = updateService.execute(new UpdateStockCommand(STOCK_ID, "Radiador novo",
                    "Radiador com tampa", new BigDecimal("400.00"), "Arrefecimento", 3));

            assertThat(result.getProductName()).isEqualTo("Radiador novo");
            assertThat(result.getDescription()).isEqualTo("Radiador com tampa");
            assertThat(result.getUnitPrice()).isEqualByComparingTo("400.00");
            assertThat(result.getCategory()).isEqualTo("Arrefecimento");
            assertThat(result.getMinThreshold()).isEqualTo(3);
            verify(repository).save(any());
        }

        @Test
        void shouldAllowKeepingTheSameProductName() {
            var stock = persistedStock(STOCK_ID, "Radiador", 10);
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(stock));
            when(repository.findByProductName("Radiador")).thenReturn(Optional.of(stock));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = updateService.execute(new UpdateStockCommand(STOCK_ID, "Radiador",
                    null, new BigDecimal("400.00"), null, null));

            assertThat(result.getProductName()).isEqualTo("Radiador");
            assertThat(result.getUnitPrice()).isEqualByComparingTo("400.00");
        }

        @Test
        void shouldNotLookUpProductNameWhenItIsNotInformed() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            updateService.execute(new UpdateStockCommand(STOCK_ID, null, null, null, "Motor", null));

            verify(repository, never()).findByProductName(anyString());
        }

        @Test
        void shouldThrowStockAlreadyExistsExceptionWhenNameBelongsToAnotherStock() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));
            when(repository.findByProductName("Alternador"))
                    .thenReturn(Optional.of(persistedStock(UUID.randomUUID(), "Alternador", 4)));

            assertThatThrownBy(() -> updateService.execute(
                    new UpdateStockCommand(STOCK_ID, "Alternador", null, null, null, null)))
                    .isInstanceOf(StockAlreadyExistsException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowStockNotFoundExceptionWhenStockDoesNotExist() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> updateService.execute(
                    new UpdateStockCommand(STOCK_ID, "Radiador", null, null, null, null)))
                    .isInstanceOf(StockNotFoundException.class)
                    .hasMessage("Estoque não encontrado: ID " + STOCK_ID);

            verify(repository, never()).save(any());
        }
    }
}
