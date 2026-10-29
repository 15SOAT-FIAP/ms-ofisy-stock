package br.com.ofisy.ms_ofisy_stock.domain.exception;

public class InvalidStockQuantityException extends RuntimeException {

    public InvalidStockQuantityException(Integer quantity) {
        super("Quantidade informada é inválida: " + quantity + ". Deve ser maior que zero.");
    }
}
