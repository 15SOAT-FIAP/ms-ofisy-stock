package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stockmovement;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockMovementMapperTest {

    @Test
    void shouldConvertEntityToDomain() {
        var id = UUID.randomUUID();
        var stockId = UUID.randomUUID();
        var createdAt = LocalDateTime.of(2026, 1, 10, 8, 0);
        var updatedAt = LocalDateTime.of(2026, 1, 10, 8, 0);

        var entity = StockMovementEntity.builder()
                .id(id)
                .stockId(stockId)
                .movementType(MovementType.OUT)
                .quantity(5)
                .previousQuantity(20)
                .newQuantity(15)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        var result = StockMovementMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStockId()).isEqualTo(stockId);
        assertThat(result.getMovementType()).isEqualTo(MovementType.OUT);
        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getPreviousQuantity()).isEqualTo(20);
        assertThat(result.getNewQuantity()).isEqualTo(15);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldConvertDomainToEntity() {
        var movement = StockMovement.create(UUID.randomUUID(), MovementType.IN, 10, 90, 100);

        var result = StockMovementMapper.toEntity(movement);

        assertThat(result.getId()).isNull();
        assertThat(result.getStockId()).isEqualTo(movement.getStockId());
        assertThat(result.getMovementType()).isEqualTo(MovementType.IN);
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getPreviousQuantity()).isEqualTo(90);
        assertThat(result.getNewQuantity()).isEqualTo(100);
        assertThat(result.getCreatedAt()).isEqualTo(movement.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(movement.getUpdatedAt());
    }
}
