package br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.util.List;
import java.util.UUID;

public interface ReleaseByServiceOrderStockUseCase {
    List<Stock> execute(ReleaseByServiceOrderStockCommand cmd);

    record ReleaseByServiceOrderStockCommand(
            UUID serviceOrderId
    ) {
    }
}
