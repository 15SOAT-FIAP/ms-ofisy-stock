package br.com.ofisy.ms_ofisy_stock.application.stock.refundbyserviceorder;

import java.util.UUID;

public interface RefundByServiceOrderStockUseCase {
    void execute(RefundByServiceOrderStockCommand cmd);

    record RefundByServiceOrderStockCommand(
            UUID eventId,
            String eventType,
            UUID serviceOrderId
    ) {
    }
}
