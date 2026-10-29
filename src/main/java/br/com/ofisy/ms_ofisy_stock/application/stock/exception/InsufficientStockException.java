package br.com.ofisy.ms_ofisy_stock.application.stock.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID id) {
        super("Estoque insuficiente: ID " + id);
    }
}
