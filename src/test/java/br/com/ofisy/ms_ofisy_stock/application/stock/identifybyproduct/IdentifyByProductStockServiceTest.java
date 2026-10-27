package br.com.ofisy.ms_ofisy_stock.application.stock.identifybyproduct;

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
class IdentifyByProductStockServiceTest {

    @Mock
    private StockRepository repository;

    @InjectMocks
    private IdentifyByProductStockService identifyByProductService;

    @Nested
    class Execute {

        @Test
        void shouldReturnStockWhenProductNameExists() {
            var id = UUID.randomUUID();
            when(repository.findByProductName("Radiador")).thenReturn(Optional.of(persistedStock(id, "Radiador", 10)));

            Stock result = identifyByProductService.execute("Radiador");

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getProductName()).isEqualTo("Radiador");
        }

        @Test
        void shouldThrowStockNotFoundExceptionWhenProductNameDoesNotExist() {
            when(repository.findByProductName("Inexistente")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> identifyByProductService.execute("Inexistente"))
                    .isInstanceOf(StockNotFoundException.class)
                    .hasMessage("Estoque não encontrado: Produto Inexistente");
        }
    }
}
