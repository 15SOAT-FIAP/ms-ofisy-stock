package br.com.ofisy.ms_ofisy_stock.application.stock.add;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase.RegisterStockMovementCommand;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AddStockService implements AddStockUseCase {

    private final StockRepository repository;
    private final RegisterStockMovementUseCase registerStockMovementUseCase;

    @Override
    public Stock execute(AddStockCommand cmd) {
        Stock stock = repository.findById(cmd.stockId())
                .orElseThrow(() -> new StockNotFoundException(cmd.stockId()));

        Integer previousQuantity = stock.getQuantity();
        stock.addQuantity(cmd.quantity());

        registerStockMovementUseCase.execute(new RegisterStockMovementCommand(
                stock.getId(),
                MovementType.IN,
                cmd.quantity(),
                previousQuantity,
                stock.getQuantity()
        ));
        return repository.save(stock);
    }
}
