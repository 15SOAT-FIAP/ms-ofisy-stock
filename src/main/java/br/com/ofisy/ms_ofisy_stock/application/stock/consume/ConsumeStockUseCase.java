package br.com.ofisy.ms_ofisy_stock.application.stock.consume;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.util.UUID;

public interface ConsumeStockUseCase {
    Stock execute(ConsumeStockCommand cmd);

    record ConsumeStockCommand(
            UUID stockId,
            Integer quantity
    ) {
    }
}
