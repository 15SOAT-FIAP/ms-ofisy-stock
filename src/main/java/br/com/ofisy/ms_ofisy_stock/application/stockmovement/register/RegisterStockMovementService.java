package br.com.ofisy.ms_ofisy_stock.application.stockmovement.register;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RegisterStockMovementService implements RegisterStockMovementUseCase {

    private final StockMovementRepository repository;

    @Override
    public StockMovement execute(RegisterStockMovementCommand cmd) {
        StockMovement movement = StockMovement.create(
                cmd.stockId(),
                cmd.type(),
                cmd.quantity(),
                cmd.previousQuantity(),
                cmd.newQuantity()
        );
        return repository.save(movement);
    }
}
