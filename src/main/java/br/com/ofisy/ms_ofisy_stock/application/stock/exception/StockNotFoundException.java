package br.com.ofisy.ms_ofisy_stock.application.stock.exception;

import java.util.UUID;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(UUID id) {
        super("Estoque não encontrado: ID " + id);
    }

    public StockNotFoundException(String productName) {
        super("Estoque não encontrado: Produto " + productName);
    }
}
