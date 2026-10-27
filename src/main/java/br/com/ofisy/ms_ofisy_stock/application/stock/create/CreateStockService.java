package br.com.ofisy.ms_ofisy_stock.application.stock.create;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockAlreadyExistsException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateStockService implements CreateStockUseCase {

    private final StockRepository repository;

    @Override
    public Stock execute(CreateStockCommand cmd) {
        if (repository.findByProductName(cmd.productName()).isPresent()) {
            throw new StockAlreadyExistsException(cmd.productName());
        }

        Stock stock = Stock.create(
                cmd.productName(),
                cmd.description(),
                cmd.quantity(),
                cmd.unitPrice(),
                cmd.category(),
                cmd.minThreshold()
        );
        return repository.save(stock);
    }
}
