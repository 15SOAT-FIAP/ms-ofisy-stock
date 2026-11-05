package br.com.ofisy.ms_ofisy_stock.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_stock.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder.RefundByServiceOrderStockUseCase.RefundByServiceOrderStockCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteReprovedHandler implements EventHandler {

    public static final String TYPE = "QuoteReproved";

    private final RefundByServiceOrderStockUseCase refundByServiceOrderStockUseCase;

    @Override
    public String type() {
        return TYPE;
    }

    // Os stockItems do payload são ignorados: a devolução segue as movimentações da OS, que dizem o que saiu de fato
    @Override
    public void handle(EventEnvelope envelope) {
        refundByServiceOrderStockUseCase.execute(
                new RefundByServiceOrderStockCommand(envelope.eventId(), envelope.type(), envelope.serviceOrderId()));
        log.info("QuoteReprovedHandler.handle >> event {} processed, stock of service order {} refunded",
                envelope.eventId(), envelope.serviceOrderId());
    }
}
