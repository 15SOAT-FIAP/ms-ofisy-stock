package br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder;

import java.util.List;
import java.util.UUID;

public interface ReleaseByServiceOrderStockUseCase {
    List<ReleasedStockItem> execute(ReleaseByServiceOrderStockCommand cmd);

    record ReleaseByServiceOrderStockCommand(
            UUID serviceOrderId
    ) {
    }

    record ReleasedStockItem(
            UUID stockId,
            Integer quantity
    ) {
    }
}
