package br.com.ofisy.ms_ofisy_stock.application.stock.update;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockAlreadyExistsException;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateStockService implements UpdateStockUseCase {

    private final StockRepository repository;

    @Override
    public Stock execute(UpdateStockCommand cmd) {
        Stock stock = repository.findById(cmd.stockId())
                .orElseThrow(() -> new StockNotFoundException(cmd.stockId()));

        if (cmd.productName() != null) {
            repository.findByProductName(cmd.productName())
                    .filter(existing -> !existing.getId().equals(stock.getId()))
                    .ifPresent(existing -> {
                        throw new StockAlreadyExistsException(cmd.productName());
                    });
        }

        stock.update(
                cmd.productName(),
                cmd.description(),
                cmd.unitPrice(),
                cmd.category(),
                cmd.minThreshold()
        );
        return repository.save(stock);
    }
}
