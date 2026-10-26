package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stock;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockMapperTest {

    @Test
    void shouldConvertEntityToDomain() {
        var id = UUID.randomUUID();
        var createdAt = LocalDateTime.of(2026, 1, 10, 8, 0);
        var updatedAt = LocalDateTime.of(2026, 1, 12, 15, 30);

        var entity = StockEntity.builder()
                .id(id)
                .productName("Radiador")
                .description("Radiador de água com reservatório")
                .quantity(10)
                .unitPrice(new BigDecimal("350.00"))
                .category("Arrefecimento")
                .minThreshold(2)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        var result = StockMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getProductName()).isEqualTo("Radiador");
        assertThat(result.getDescription()).isEqualTo("Radiador de água com reservatório");
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getUnitPrice()).isEqualByComparingTo("350.00");
        assertThat(result.getCategory()).isEqualTo("Arrefecimento");
        assertThat(result.getMinThreshold()).isEqualTo(2);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldConvertDomainToEntity() {
        var stock = Stock.create("Radiador", "Radiador de água com reservatório", 10,
                new BigDecimal("350.00"), "Arrefecimento", 2);

        var result = StockMapper.toEntity(stock);

        assertThat(result.getId()).isNull();
        assertThat(result.getProductName()).isEqualTo(stock.getProductName());
        assertThat(result.getDescription()).isEqualTo(stock.getDescription());
        assertThat(result.getQuantity()).isEqualTo(stock.getQuantity());
        assertThat(result.getUnitPrice()).isEqualByComparingTo(stock.getUnitPrice());
        assertThat(result.getCategory()).isEqualTo(stock.getCategory());
        assertThat(result.getMinThreshold()).isEqualTo(stock.getMinThreshold());
        assertThat(result.getCreatedAt()).isEqualTo(stock.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(stock.getUpdatedAt());
    }

    @Test
    void shouldKeepIdWhenConvertingPersistedDomainToEntity() {
        var id = UUID.randomUUID();
        var stock = Stock.reconstruct(id, "Radiador", null, 10, new BigDecimal("350.00"), null, 2,
                LocalDateTime.of(2026, 1, 10, 8, 0), LocalDateTime.of(2026, 1, 10, 8, 0));

        var result = StockMapper.toEntity(stock);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getDescription()).isNull();
        assertThat(result.getCategory()).isNull();
    }
}
