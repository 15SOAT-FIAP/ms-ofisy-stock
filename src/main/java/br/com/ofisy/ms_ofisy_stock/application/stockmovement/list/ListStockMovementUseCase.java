package br.com.ofisy.ms_ofisy_stock.application.stockmovement.list;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListStockMovementUseCase {
    Page<StockMovement> execute(Pageable pageable);
}
