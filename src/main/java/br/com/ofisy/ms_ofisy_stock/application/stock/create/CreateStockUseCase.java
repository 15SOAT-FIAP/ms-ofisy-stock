package br.com.ofisy.ms_ofisy_stock.application.stock.create;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.math.BigDecimal;

public interface CreateStockUseCase {
    Stock execute(CreateStockCommand cmd);

    record CreateStockCommand(
            String productName,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            String category,
            Integer minThreshold
    ) {
    }
}
