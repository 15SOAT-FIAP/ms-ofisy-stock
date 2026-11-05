package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote;

import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;

import java.util.List;
import java.util.UUID;

public interface ConsumeByQuoteStockUseCase {
    void execute(ConsumeByQuoteStockCommand cmd);

    record ConsumeByQuoteStockCommand(
            UUID eventId,
            String eventType,
            UUID serviceOrderId,
            UUID quoteId,
            List<StockItem> items
    ) {
    }
}
