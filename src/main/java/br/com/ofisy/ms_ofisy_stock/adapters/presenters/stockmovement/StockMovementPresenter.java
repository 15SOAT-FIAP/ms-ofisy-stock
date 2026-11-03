package br.com.ofisy.ms_ofisy_stock.adapters.presenters.stockmovement;

import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stockmovement.dto.StockMovementResponseDTO;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StockMovementPresenter {

    public static StockMovementResponseDTO present(StockMovement stockMovement) {
        return new StockMovementResponseDTO(
                stockMovement.getId(),
                stockMovement.getStockId(),
                stockMovement.getMovementType(),
                stockMovement.getQuantity(),
                stockMovement.getPreviousQuantity(),
                stockMovement.getNewQuantity(),
                stockMovement.getCreatedAt(),
                stockMovement.getUpdatedAt()
        );
    }
}
