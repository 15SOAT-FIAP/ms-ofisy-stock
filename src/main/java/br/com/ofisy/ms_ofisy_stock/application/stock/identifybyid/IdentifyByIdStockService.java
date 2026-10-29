package br.com.ofisy.ms_ofisy_stock.application.stock.identifybyid;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IdentifyByIdStockService implements IdentifyByIdStockUseCase {

    private final StockRepository repository;

    @Override
    public Stock execute(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new StockNotFoundException(id));
    }
}
