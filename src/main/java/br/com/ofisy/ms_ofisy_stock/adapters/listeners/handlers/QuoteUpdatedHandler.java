package br.com.ofisy.ms_ofisy_stock.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_stock.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.QuoteStockItemsPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote.AdjustByQuoteStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote.AdjustByQuoteStockUseCase.AdjustByQuoteStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteUpdatedHandler implements EventHandler {

    public static final String TYPE = "QuoteUpdated";

    private final AdjustByQuoteStockUseCase adjustByQuoteStockUseCase;
    private final JsonMapper jsonMapper;

    @Override
    public String type() {
        return TYPE;
    }

    // A lista vem completa, como o orçamento ficou depois da edição, e pode vir vazia quando todas as peças saem.
    // Sem OS, orçamento ou lista de itens não há o que ajustar nem a quem responder: a exceção leva a mensagem para a DLQ
    @Override
    public void handle(EventEnvelope envelope) {
        QuoteStockItemsPayload payload = envelope.payload() == null ? null
                : jsonMapper.treeToValue(envelope.payload(), QuoteStockItemsPayload.class);
        if (envelope.serviceOrderId() == null || payload == null || payload.quoteId() == null
                || payload.stockItems() == null) {
            throw new IllegalArgumentException("QuoteUpdated inválido: " + envelope.eventId());
        }

        List<StockItem> items = payload.stockItems().stream()
                .map(item -> new StockItem(item.stockId(), item.quantity()))
                .toList();
        adjustByQuoteStockUseCase.execute(new AdjustByQuoteStockCommand(envelope.eventId(), envelope.type(),
                envelope.serviceOrderId(), payload.quoteId(), items));
        log.info("QuoteUpdatedHandler.handle >> event {} processed for quote {} of service order {}",
                envelope.eventId(), payload.quoteId(), envelope.serviceOrderId());
    }
}
