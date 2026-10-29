package br.com.ofisy.ms_ofisy_stock.application.stock.identifybyproduct;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IdentifyByProductStockService implements IdentifyByProductStockUseCase {

    private final StockRepository repository;

    @Override
    public Stock execute(String productName) {
        return repository.findByProductName(productName)
                .orElseThrow(() -> new StockNotFoundException(productName));
    }
}
