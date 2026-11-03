package br.com.ofisy.ms_ofisy_stock.application.stock.consume;

import br.com.ofisy.ms_ofisy_stock.application.stock.exception.InsufficientStockException;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.register.RegisterStockMovementUseCase.RegisterStockMovementCommand;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ConsumeStockService implements ConsumeStockUseCase {

    public static final String STOCK_LOW = "StockLow";

    private final StockRepository repository;
    private final RegisterStockMovementUseCase registerStockMovementUseCase;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public Stock execute(ConsumeStockCommand cmd) {
        Stock stock = repository.lockById(cmd.stockId())
                .orElseThrow(() -> new StockNotFoundException(cmd.stockId()));

        if (cmd.quantity() != null && stock.getQuantity() < cmd.quantity()) {
            throw new InsufficientStockException(cmd.stockId());
        }

        Integer previousQuantity = stock.getQuantity();
        stock.consumeQuantity(cmd.quantity());

        registerStockMovementUseCase.execute(new RegisterStockMovementCommand(
                stock.getId(),
                MovementType.OUT,
                cmd.quantity(),
                previousQuantity,
                stock.getQuantity(),
                cmd.serviceOrderId()
        ));
        Stock savedStock = repository.save(stock);

        // Como no core, o aviso sai a cada consumo que deixa o saldo igual ou abaixo do mínimo.
        // Não pertence a uma OS, então o grupo da mensagem é o próprio item
        if (savedStock.isLowStock()) {
            outboxEventRepository.save(OutboxEvent.createWithoutServiceOrder(STOCK_LOW, savedStock.getId(),
                    new StockLowPayload(savedStock.getId(), savedStock.getProductName(), savedStock.getQuantity(),
                            savedStock.getMinThreshold())));
        }
        return savedStock;
    }

    public record StockLowPayload(UUID stockId, String productName, Integer currentQuantity, Integer minThreshold) {
    }
}
