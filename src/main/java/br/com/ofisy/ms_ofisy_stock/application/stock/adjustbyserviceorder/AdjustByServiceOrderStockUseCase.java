package br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;

import java.util.List;
import java.util.UUID;

public interface AdjustByServiceOrderStockUseCase {
    AdjustByServiceOrderStockResult execute(AdjustByServiceOrderStockCommand cmd);

    record AdjustByServiceOrderStockCommand(
            UUID serviceOrderId,
            List<StockItem> items
    ) {
    }

    record AdjustByServiceOrderStockResult(
            List<MissingStockItem> missingItems
    ) {
        public boolean adjusted() {
            return missingItems.isEmpty();
        }
    }
}
