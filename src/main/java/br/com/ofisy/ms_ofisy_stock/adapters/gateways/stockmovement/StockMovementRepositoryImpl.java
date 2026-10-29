package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stockmovement;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockMovementRepositoryImpl implements StockMovementRepository {

    private final JpaStockMovementRepository jpa;

    @Override
    public StockMovement save(StockMovement stockMovement) {
        StockMovementEntity entity = StockMovementMapper.toEntity(stockMovement);
        StockMovementEntity savedEntity = jpa.save(entity);
        return StockMovementMapper.toDomain(savedEntity);
    }

    @Override
    public Page<StockMovement> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(StockMovementMapper::toDomain);
    }

    @Override
    public Page<StockMovement> findByStockId(UUID stockId, Pageable pageable) {
        return jpa.findByStockId(stockId, pageable)
                .map(StockMovementMapper::toDomain);
    }
}
