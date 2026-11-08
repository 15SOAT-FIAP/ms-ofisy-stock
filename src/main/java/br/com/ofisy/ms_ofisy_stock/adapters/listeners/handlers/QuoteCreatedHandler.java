package br.com.ofisy.ms_ofisy_stock.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_stock.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.QuoteStockItemsPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockUseCase.ConsumeByQuoteStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteCreatedHandler implements EventHandler {

    public static final String TYPE = "QuoteCreated";

    private final ConsumeByQuoteStockUseCase consumeByQuoteStockUseCase;
    private final JsonMapper jsonMapper;

    @Override
    public String type() {
        return TYPE;
    }

    // Sem OS, orçamento ou lista de itens não há o que consumir nem a quem responder: a exceção leva a mensagem para a DLQ
    @Override
    public void handle(EventEnvelope envelope) {
        QuoteStockItemsPayload payload = envelope.payload() == null ? null
                : jsonMapper.treeToValue(envelope.payload(), QuoteStockItemsPayload.class);
        if (envelope.serviceOrderId() == null || payload == null || payload.quoteId() == null
                || payload.stockItems() == null) {
            throw new IllegalArgumentException("QuoteCreated inválido: " + envelope.eventId());
        }

        List<StockItem> items = payload.stockItems().stream()
                .map(item -> new StockItem(item.stockId(), item.quantity()))
                .toList();
        consumeByQuoteStockUseCase.execute(new ConsumeByQuoteStockCommand(envelope.eventId(), envelope.type(),
                envelope.serviceOrderId(), payload.quoteId(), items));
        log.info("QuoteCreatedHandler.handle >> event {} processed for quote {} of service order {}",
                envelope.eventId(), payload.quoteId(), envelope.serviceOrderId());
    }
}
