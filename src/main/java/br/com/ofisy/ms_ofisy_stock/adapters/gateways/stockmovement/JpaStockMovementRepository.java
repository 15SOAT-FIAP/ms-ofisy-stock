package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stockmovement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaStockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {

    Page<StockMovementEntity> findByStockId(UUID stockId, Pageable pageable);
}
