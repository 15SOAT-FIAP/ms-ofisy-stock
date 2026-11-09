package br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyquote;

import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase.AdjustByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase.AdjustByServiceOrderStockResult;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.StockConsumedPayload;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.StockConsumptionFailedPayload;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.STOCK_CONSUMED;
import static br.com.ofisy.ms_ofisy_stock.application.stock.consumebyquote.ConsumeByQuoteStockService.STOCK_CONSUMPTION_FAILED;
import static br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockService.STOCK_REFUNDED;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdjustByQuoteStockService implements AdjustByQuoteStockUseCase {

    private final AdjustByServiceOrderStockUseCase adjustByServiceOrderStockUseCase;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    // A resposta é a mesma da criação do orçamento: StockConsumed com a lista final, ou StockConsumptionFailed
    // com o que faltou, e nesse caso nada se move.
    // A edição chega pelo tópico do billing e o cancelamento pelo do execution, sem ordem garantida entre eles.
    // Se a OS já teve os itens devolvidos, a edição chegou atrasada e é ignorada, para não baixar de novo
    @Override
    public void execute(AdjustByQuoteStockCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        if (outboxEventRepository.existsByServiceOrderIdAndEventType(cmd.serviceOrderId(), STOCK_REFUNDED)) {
            log.warn("AdjustByQuoteStockService.execute >> service order {} was already refunded, ignoring event {}",
                    cmd.serviceOrderId(), cmd.eventId());
        } else {
            AdjustByServiceOrderStockResult result = adjustByServiceOrderStockUseCase.execute(
                    new AdjustByServiceOrderStockCommand(cmd.serviceOrderId(), cmd.items()));
            if (result.adjusted()) {
                outboxEventRepository.save(OutboxEvent.create(STOCK_CONSUMED, cmd.serviceOrderId(),
                        new StockConsumedPayload(cmd.quoteId(), cmd.items())));
            } else {
                outboxEventRepository.save(OutboxEvent.create(STOCK_CONSUMPTION_FAILED, cmd.serviceOrderId(),
                        new StockConsumptionFailedPayload(cmd.quoteId(), result.missingItems())));
            }
        }

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }
}
