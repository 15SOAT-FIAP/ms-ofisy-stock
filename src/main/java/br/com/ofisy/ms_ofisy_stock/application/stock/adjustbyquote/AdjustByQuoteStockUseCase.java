package br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote;

import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;

import java.util.List;
import java.util.UUID;

public interface AdjustByQuoteStockUseCase {
    void execute(AdjustByQuoteStockCommand cmd);

    record AdjustByQuoteStockCommand(
            UUID eventId,
            String eventType,
            UUID serviceOrderId,
            UUID quoteId,
            List<StockItem> items
    ) {
    }
}
