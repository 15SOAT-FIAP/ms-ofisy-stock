package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface JpaStockRepository extends JpaRepository<StockEntity, UUID> {

    Optional<StockEntity> findByProductName(String productName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockEntity> findForUpdateById(UUID id);
}
