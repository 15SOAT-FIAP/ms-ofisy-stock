package br.com.ofisy.ms_ofisy_stock.domain.stockmovement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockMovementRepository {

    StockMovement save(StockMovement stockMovement);

    Page<StockMovement> findAll(Pageable pageable);

    Page<StockMovement> findByStockId(UUID stockId, Pageable pageable);
}
