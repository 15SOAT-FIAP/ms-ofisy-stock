package br.com.ofisy.ms_ofisy_stock.domain.stockmovement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository {

    StockMovement save(StockMovement stockMovement);

    Page<StockMovement> findAll(Pageable pageable);

    Page<StockMovement> findByStockId(UUID stockId, Pageable pageable);

    List<StockMovement> findByServiceOrderId(UUID serviceOrderId);
}
