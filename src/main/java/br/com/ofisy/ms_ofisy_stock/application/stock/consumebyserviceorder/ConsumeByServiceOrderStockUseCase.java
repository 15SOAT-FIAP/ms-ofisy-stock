package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;

import java.util.List;
import java.util.UUID;

public interface ConsumeByServiceOrderStockUseCase {
    ConsumeByServiceOrderStockResult execute(ConsumeByServiceOrderStockCommand cmd);

    record ConsumeByServiceOrderStockCommand(
            UUID serviceOrderId,
            List<StockItem> items
    ) {
    }

    record StockItem(
            UUID stockId,
            Integer quantity
    ) {
    }

    record MissingStockItem(
            UUID stockId,
            Integer requestedQuantity,
            Integer availableQuantity
    ) {
    }

    record ConsumeByServiceOrderStockResult(
            List<Stock> consumedStocks,
            List<MissingStockItem> missingItems
    ) {
        public boolean consumed() {
            return missingItems.isEmpty();
        }
    }
}
