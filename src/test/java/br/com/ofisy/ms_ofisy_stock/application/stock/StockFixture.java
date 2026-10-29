package br.com.ofisy.ms_ofisy_stock.application.stock;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class StockFixture {

    private StockFixture() {
    }

    public static Stock persistedStock(UUID id, String productName, Integer quantity) {
        return Stock.reconstruct(id, productName, "Descrição de " + productName, quantity,
                new BigDecimal("100.00"), "Motor", 5,
                LocalDateTime.of(2026, 1, 10, 8, 0), LocalDateTime.of(2026, 1, 10, 8, 0));
    }
}
