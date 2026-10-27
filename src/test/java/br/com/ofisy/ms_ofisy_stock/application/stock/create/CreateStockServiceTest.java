package br.com.ofisy.ms_ofisy_stock.application.stock.create;

import br.com.ofisy.ms_ofisy_stock.application.stock.create.CreateStockUseCase.CreateStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockAlreadyExistsException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateStockServiceTest {

    @Mock
    private StockRepository repository;

    @InjectMocks
    private CreateStockService createService;

    private CreateStockCommand command() {
        return new CreateStockCommand("Radiador", "Radiador de água com reservatório", 10,
                new BigDecimal("350.00"), "Arrefecimento", 2);
    }

    @Nested
    class Execute {

        @Test
        void shouldCreateStockWithCommandData() {
            when(repository.findByProductName("Radiador")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Stock result = createService.execute(command());

            assertThat(result.getProductName()).isEqualTo("Radiador");
            assertThat(result.getDescription()).isEqualTo("Radiador de água com reservatório");
            assertThat(result.getQuantity()).isEqualTo(10);
            assertThat(result.getUnitPrice()).isEqualByComparingTo("350.00");
            assertThat(result.getCategory()).isEqualTo("Arrefecimento");
            assertThat(result.getMinThreshold()).isEqualTo(2);
            assertThat(result.getCreatedAt()).isNotNull();
            verify(repository).save(any());
        }

        @Test
        void shouldThrowStockAlreadyExistsExceptionWhenProductNameIsTaken() {
            when(repository.findByProductName("Radiador"))
                    .thenReturn(Optional.of(persistedStock(UUID.randomUUID(), "Radiador", 10)));

            assertThatThrownBy(() -> createService.execute(command()))
                    .isInstanceOf(StockAlreadyExistsException.class)
                    .hasMessage("Já existe um estoque cadastrado para o produto Radiador");

            verify(repository, never()).save(any());
        }
    }
}
