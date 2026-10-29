package br.com.ofisy.ms_ofisy_stock.application.stock.identifybyproduct;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

public interface IdentifyByProductStockUseCase {
    Stock execute(String productName);
}
