package br.com.ofisy.ms_ofisy_stock.application.stock.identifybyid;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentifyByIdStockServiceTest {

    private static final UUID STOCK_ID = UUID.randomUUID();

    @Mock
    private StockRepository repository;

    @InjectMocks
    private IdentifyByIdStockService identifyByIdService;

    @Nested
    class Execute {

        @Test
        void shouldReturnStockWhenIdExists() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.of(persistedStock(STOCK_ID, "Radiador", 10)));

            Stock result = identifyByIdService.execute(STOCK_ID);

            assertThat(result.getId()).isEqualTo(STOCK_ID);
            assertThat(result.getProductName()).isEqualTo("Radiador");
        }

        @Test
        void shouldThrowStockNotFoundExceptionWhenIdDoesNotExist() {
            when(repository.findById(STOCK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> identifyByIdService.execute(STOCK_ID))
                    .isInstanceOf(StockNotFoundException.class)
                    .hasMessage("Estoque não encontrado: ID " + STOCK_ID);
        }
    }
}
