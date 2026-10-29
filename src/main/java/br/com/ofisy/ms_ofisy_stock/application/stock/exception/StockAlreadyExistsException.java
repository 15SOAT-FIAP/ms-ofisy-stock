package br.com.ofisy.ms_ofisy_stock.application.stock.exception;

public class StockAlreadyExistsException extends RuntimeException {

    public StockAlreadyExistsException(String productName) {
        super("Já existe um estoque cadastrado para o produto " + productName);
    }
}
