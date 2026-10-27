package br.com.ofisy.ms_ofisy_stock.application.stockmovement.listbystock;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListByStockStockMovementService implements ListByStockStockMovementUseCase {

    private final StockMovementRepository repository;

    @Override
    public Page<StockMovement> execute(UUID stockId, Pageable pageable) {
        return repository.findByStockId(stockId, pageable);
    }
}
