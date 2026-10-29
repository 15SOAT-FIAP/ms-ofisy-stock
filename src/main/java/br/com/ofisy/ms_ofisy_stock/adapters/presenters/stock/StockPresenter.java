package br.com.ofisy.ms_ofisy_stock.adapters.presenters.stock;

import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto.StockResponseDTO;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StockPresenter {

    public static StockResponseDTO present(Stock stock) {
        return new StockResponseDTO(
                stock.getId(),
                stock.getProductName(),
                stock.getDescription(),
                stock.getQuantity(),
                stock.getUnitPrice(),
                stock.getCategory(),
                stock.getCreatedAt(),
                stock.getUpdatedAt(),
                stock.getMinThreshold(),
                stock.isLowStock()
        );
    }
}
