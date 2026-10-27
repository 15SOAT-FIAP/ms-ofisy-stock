package br.com.ofisy.ms_ofisy_stock.application.stock.add;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.util.UUID;

public interface AddStockUseCase {
    Stock execute(AddStockCommand cmd);

    record AddStockCommand(
            UUID stockId,
            Integer quantity
    ) {
    }
}
