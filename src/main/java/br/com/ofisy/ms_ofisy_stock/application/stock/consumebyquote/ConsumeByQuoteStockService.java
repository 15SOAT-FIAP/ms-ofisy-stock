package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote;

import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockResult;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ConsumeByQuoteStockService implements ConsumeByQuoteStockUseCase {

    public static final String STOCK_CONSUMED = "StockConsumed";
    public static final String STOCK_CONSUMPTION_FAILED = "StockConsumptionFailed";

    private final ConsumeByServiceOrderStockUseCase consumeByServiceOrderStockUseCase;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    // Falta de saldo não é erro: nada é consumido e o StockConsumptionFailed avisa o billing para cancelar o orçamento
    @Override
    public void execute(ConsumeByQuoteStockCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        ConsumeByServiceOrderStockResult result = consumeByServiceOrderStockUseCase.execute(
                new ConsumeByServiceOrderStockCommand(cmd.serviceOrderId(), cmd.items()));
        if (result.consumed()) {
            outboxEventRepository.save(OutboxEvent.create(STOCK_CONSUMED, cmd.serviceOrderId(),
                    new StockConsumedPayload(cmd.quoteId(), cmd.items())));
        } else {
            outboxEventRepository.save(OutboxEvent.create(STOCK_CONSUMPTION_FAILED, cmd.serviceOrderId(),
                    new StockConsumptionFailedPayload(cmd.quoteId(), result.missingItems())));
        }

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }

    public record StockConsumedPayload(UUID quoteId, List<StockItem> items) {
    }

    public record StockConsumptionFailedPayload(UUID quoteId, List<MissingStockItem> missingItems) {
    }
}
