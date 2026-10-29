package br.com.ofisy.ms_ofisy_stock.application.stock.identifybyid;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.util.UUID;

public interface IdentifyByIdStockUseCase {
    Stock execute(UUID id);
}
