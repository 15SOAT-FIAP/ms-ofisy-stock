package br.com.ofisy.ms_ofisy_stock.application.stockmovement.register;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;

import java.util.UUID;

public interface RegisterStockMovementUseCase {
    StockMovement execute(RegisterStockMovementCommand cmd);

    record RegisterStockMovementCommand(
            UUID stockId,
            MovementType type,
            Integer quantity,
            Integer previousQuantity,
            Integer newQuantity
    ) {
    }
}
