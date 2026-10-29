package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stock;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StockMapper {

    public static Stock toDomain(StockEntity entity) {
        return Stock.reconstruct(
                entity.getId(),
                entity.getProductName(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getCategory(),
                entity.getMinThreshold(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static StockEntity toEntity(Stock domain) {
        return StockEntity.builder()
                .id(domain.getId())
                .productName(domain.getProductName())
                .description(domain.getDescription())
                .quantity(domain.getQuantity())
                .unitPrice(domain.getUnitPrice())
                .category(domain.getCategory())
                .minThreshold(domain.getMinThreshold())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
