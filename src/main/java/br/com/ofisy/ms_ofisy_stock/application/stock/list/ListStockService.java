package br.com.ofisy.ms_ofisy_stock.application.stock.list;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListStockService implements ListStockUseCase {

    private final StockRepository repository;

    @Override
    public Page<Stock> execute(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
