package br.com.ofisy.ms_ofisy_stock.application.stock.list;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListStockUseCase {
    Page<Stock> execute(Pageable pageable);
}
