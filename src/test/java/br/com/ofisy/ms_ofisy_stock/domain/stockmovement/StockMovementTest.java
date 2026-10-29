package br.com.ofisy.ms_ofisy_stock.domain.stockmovement;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockMovementTest {

    @Nested
    class CreateStockMovement {
        @Test
        void shouldCreateInMovement() {
            var stockId = UUID.randomUUID();

            var result = StockMovement.create(stockId, MovementType.IN, 10, 90, 100);

            assertThat(result.getId()).isNull();
            assertThat(result.getStockId()).isEqualTo(stockId);
            assertThat(result.getMovementType()).isEqualTo(MovementType.IN);
            assertThat(result.getQuantity()).isEqualTo(10);
            assertThat(result.getPreviousQuantity()).isEqualTo(90);
            assertThat(result.getNewQuantity()).isEqualTo(100);
        }

        @Test
        void shouldCreateOutMovement() {
            var stockId = UUID.randomUUID();

            var result = StockMovement.create(stockId, MovementType.OUT, 10, 100, 90);

            assertThat(result.getStockId()).isEqualTo(stockId);
            assertThat(result.getMovementType()).isEqualTo(MovementType.OUT);
            assertThat(result.getQuantity()).isEqualTo(10);
            assertThat(result.getPreviousQuantity()).isEqualTo(100);
            assertThat(result.getNewQuantity()).isEqualTo(90);
        }

        @Test
        void shouldSetCreatedAtAndUpdatedAt() {
            var result = StockMovement.create(UUID.randomUUID(), MovementType.IN, 10, 90, 100);

            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    class ReconstructStockMovement {
        @Test
        void shouldRestoreAllFieldsFromPersistedData() {
            var id = UUID.randomUUID();
            var stockId = UUID.randomUUID();
            var createdAt = LocalDateTime.of(2026, 1, 10, 8, 0);
            var updatedAt = LocalDateTime.of(2026, 1, 10, 8, 0);

            var result = StockMovement.reconstruct(id, stockId, MovementType.OUT, 5, 20, 15, createdAt, updatedAt);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getStockId()).isEqualTo(stockId);
            assertThat(result.getMovementType()).isEqualTo(MovementType.OUT);
            assertThat(result.getQuantity()).isEqualTo(5);
            assertThat(result.getPreviousQuantity()).isEqualTo(20);
            assertThat(result.getNewQuantity()).isEqualTo(15);
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
            assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }
}
