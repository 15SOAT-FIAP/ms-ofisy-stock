package br.com.ofisy.ms_ofisy_stock.application.stock.release;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.util.UUID;

public interface ReleaseStockUseCase {
    Stock execute(ReleaseStockCommand cmd);

    record ReleaseStockCommand(
            UUID stockId,
            Integer quantity
    ) {
    }
}
