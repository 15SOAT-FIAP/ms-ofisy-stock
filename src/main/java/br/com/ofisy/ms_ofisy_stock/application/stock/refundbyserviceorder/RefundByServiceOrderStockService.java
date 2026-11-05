package br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase.ReleaseByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase.ReleasedStockItem;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RefundByServiceOrderStockService implements RefundByServiceOrderStockUseCase {

    public static final String STOCK_REFUNDED = "StockRefunded";

    private final ReleaseByServiceOrderStockUseCase releaseByServiceOrderStockUseCase;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    // O StockRefunded sai sempre, mesmo sem nada a devolver (OS sem consumo ou já devolvida), porque o
    // billing e o core seguem a saga a partir dele. Nesse caso a lista de itens vai vazia
    @Override
    public void execute(RefundByServiceOrderStockCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        List<ReleasedStockItem> releasedItems = releaseByServiceOrderStockUseCase.execute(
                new ReleaseByServiceOrderStockCommand(cmd.serviceOrderId()));
        outboxEventRepository.save(OutboxEvent.create(STOCK_REFUNDED, cmd.serviceOrderId(),
                new StockRefundedPayload(releasedItems)));

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }

    public record StockRefundedPayload(List<ReleasedStockItem> items) {
    }
}
