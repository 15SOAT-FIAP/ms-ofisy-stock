package br.com.ofisy.ms_ofisy_stock.domain.stock;

import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    private Stock newStock(Integer quantity, Integer minThreshold) {
        return Stock.create("Filtro de óleo", "Filtro de óleo do motor", quantity,
                new BigDecimal("25.00"), "Motor", minThreshold);
    }

    private Stock persistedStock() {
        return Stock.reconstruct(UUID.randomUUID(), "Filtro de óleo", "Filtro de óleo do motor", 40,
                new BigDecimal("25.00"), "Motor", 10,
                LocalDateTime.of(2026, 1, 10, 8, 0), LocalDateTime.of(2026, 1, 10, 8, 0));
    }

    @Nested
    class CreateStock {
        @Test
        void shouldCreateStockWithValidData() {
            var result = Stock.create("Filtro de óleo", "Filtro de óleo do motor", 40,
                    new BigDecimal("25.00"), "Motor", 10);

            assertThat(result.getId()).isNull();
            assertThat(result.getProductName()).isEqualTo("Filtro de óleo");
            assertThat(result.getDescription()).isEqualTo("Filtro de óleo do motor");
            assertThat(result.getQuantity()).isEqualTo(40);
            assertThat(result.getUnitPrice()).isEqualByComparingTo("25.00");
            assertThat(result.getCategory()).isEqualTo("Motor");
            assertThat(result.getMinThreshold()).isEqualTo(10);
        }

        @Test
        void shouldSetCreatedAtAndUpdatedAt() {
            var result = newStock(40, 10);

            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    class ReconstructStock {
        @Test
        void shouldRestoreAllFieldsFromPersistedData() {
            var id = UUID.randomUUID();
            var createdAt = LocalDateTime.of(2026, 1, 10, 8, 0);
            var updatedAt = LocalDateTime.of(2026, 1, 12, 15, 30);

            var result = Stock.reconstruct(id, "Bateria automotiva", "Bateria 60Ah", 6,
                    new BigDecimal("450.00"), "Elétrica", 2, createdAt, updatedAt);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getProductName()).isEqualTo("Bateria automotiva");
            assertThat(result.getDescription()).isEqualTo("Bateria 60Ah");
            assertThat(result.getQuantity()).isEqualTo(6);
            assertThat(result.getUnitPrice()).isEqualByComparingTo("450.00");
            assertThat(result.getCategory()).isEqualTo("Elétrica");
            assertThat(result.getMinThreshold()).isEqualTo(2);
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
            assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    class UpdateStock {
        @Test
        void shouldUpdateAllInformedFields() {
            var stock = persistedStock();
            var previousUpdatedAt = stock.getUpdatedAt();

            stock.update("Filtro de óleo premium", "Filtro de óleo sintético", new BigDecimal("40.00"),
                    "Filtros", 5);

            assertThat(stock.getProductName()).isEqualTo("Filtro de óleo premium");
            assertThat(stock.getDescription()).isEqualTo("Filtro de óleo sintético");
            assertThat(stock.getUnitPrice()).isEqualByComparingTo("40.00");
            assertThat(stock.getCategory()).isEqualTo("Filtros");
            assertThat(stock.getMinThreshold()).isEqualTo(5);
            assertThat(stock.getUpdatedAt()).isAfter(previousUpdatedAt);
        }

        @Test
        void shouldKeepCurrentValuesWhenFieldsAreNull() {
            var stock = persistedStock();

            stock.update(null, null, null, null, null);

            assertThat(stock.getProductName()).isEqualTo("Filtro de óleo");
            assertThat(stock.getDescription()).isEqualTo("Filtro de óleo do motor");
            assertThat(stock.getUnitPrice()).isEqualByComparingTo("25.00");
            assertThat(stock.getCategory()).isEqualTo("Motor");
            assertThat(stock.getMinThreshold()).isEqualTo(10);
        }

        @Test
        void shouldIgnoreUnitPriceThatIsNotPositive() {
            var stock = persistedStock();

            stock.update(null, null, BigDecimal.ZERO, null, null);

            assertThat(stock.getUnitPrice()).isEqualByComparingTo("25.00");
        }

        @Test
        void shouldIgnoreNegativeMinThreshold() {
            var stock = persistedStock();

            stock.update(null, null, null, null, -1);

            assertThat(stock.getMinThreshold()).isEqualTo(10);
        }

        @Test
        void shouldAcceptZeroAsMinThreshold() {
            var stock = persistedStock();

            stock.update(null, null, null, null, 0);

            assertThat(stock.getMinThreshold()).isZero();
        }
    }

    @Nested
    class AddQuantity {
        @Test
        void shouldAddQuantityToStock() {
            var stock = persistedStock();
            var previousUpdatedAt = stock.getUpdatedAt();

            stock.addQuantity(10);

            assertThat(stock.getQuantity()).isEqualTo(50);
            assertThat(stock.getUpdatedAt()).isAfter(previousUpdatedAt);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -5})
        void shouldRejectInvalidQuantity(Integer quantity) {
            var stock = persistedStock();

            assertThatThrownBy(() -> stock.addQuantity(quantity))
                    .isInstanceOf(InvalidStockQuantityException.class)
                    .hasMessage("Quantidade informada é inválida: " + quantity + ". Deve ser maior que zero.");
            assertThat(stock.getQuantity()).isEqualTo(40);
        }
    }

    @Nested
    class ConsumeQuantity {
        @Test
        void shouldConsumeQuantityFromStock() {
            var stock = persistedStock();
            var previousUpdatedAt = stock.getUpdatedAt();

            stock.consumeQuantity(10);

            assertThat(stock.getQuantity()).isEqualTo(30);
            assertThat(stock.getUpdatedAt()).isAfter(previousUpdatedAt);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -5})
        void shouldRejectInvalidQuantity(Integer quantity) {
            var stock = persistedStock();

            assertThatThrownBy(() -> stock.consumeQuantity(quantity))
                    .isInstanceOf(InvalidStockQuantityException.class);
            assertThat(stock.getQuantity()).isEqualTo(40);
        }
    }

    @Nested
    class LowStock {
        @Test
        void shouldBeLowStockWhenQuantityIsBelowMinThreshold() {
            assertThat(newStock(5, 10).isLowStock()).isTrue();
        }

        @Test
        void shouldBeLowStockWhenQuantityEqualsMinThreshold() {
            assertThat(newStock(10, 10).isLowStock()).isTrue();
        }

        @Test
        void shouldNotBeLowStockWhenQuantityIsAboveMinThreshold() {
            assertThat(newStock(11, 10).isLowStock()).isFalse();
        }

        @Test
        void shouldNotBeLowStockWhenMinThresholdIsNull() {
            assertThat(newStock(0, null).isLowStock()).isFalse();
        }

        @Test
        void shouldBecomeLowStockAfterConsumingQuantity() {
            var stock = persistedStock();

            stock.consumeQuantity(30);

            assertThat(stock.isLowStock()).isTrue();
        }
    }
}
