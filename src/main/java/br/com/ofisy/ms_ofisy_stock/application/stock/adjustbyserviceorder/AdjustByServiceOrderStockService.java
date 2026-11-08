package br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase.ReleaseStockCommand;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AdjustByServiceOrderStockService implements AdjustByServiceOrderStockUseCase {

    private final StockRepository repository;
    private final StockMovementRepository stockMovementRepository;
    private final ConsumeStockUseCase consumeStockUseCase;
    private final ReleaseStockUseCase releaseStockUseCase;

    // Leva o que está com a OS (o que saiu e ainda não voltou) até a lista recebida: baixa o que aumentou e
    // devolve o que diminuiu. Repetir o ajuste com a mesma lista não altera o saldo.
    // Se faltar saldo para algum aumento, nada se move. Todos os itens que mudam são travados em ordem de id
    // antes de qualquer movimentação, a mesma ordem do consumo e da devolução
    @Override
    public AdjustByServiceOrderStockResult execute(AdjustByServiceOrderStockCommand cmd) {
        Map<UUID, Integer> differenceByStock = new TreeMap<>();
        for (StockItem item : cmd.items()) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new InvalidStockQuantityException(item.quantity());
            }
            differenceByStock.merge(item.stockId(), item.quantity(), Integer::sum);
        }
        withServiceOrder(cmd.serviceOrderId()).forEach((stockId, quantity) ->
                differenceByStock.merge(stockId, -quantity, Integer::sum));
        differenceByStock.values().removeIf(difference -> difference == 0);

        List<MissingStockItem> missingItems = new ArrayList<>();
        differenceByStock.forEach((stockId, difference) -> {
            int availableQuantity = repository.lockById(stockId).map(Stock::getQuantity).orElse(0);
            if (difference > 0 && availableQuantity < difference) {
                missingItems.add(new MissingStockItem(stockId, difference, availableQuantity));
            }
        });
        if (!missingItems.isEmpty()) {
            return new AdjustByServiceOrderStockResult(missingItems);
        }

        differenceByStock.forEach((stockId, difference) -> {
            if (difference > 0) {
                consumeStockUseCase.execute(new ConsumeStockCommand(stockId, difference, cmd.serviceOrderId()));
            } else {
                releaseStockUseCase.execute(new ReleaseStockCommand(stockId, -difference, cmd.serviceOrderId()));
            }
        });
        return new AdjustByServiceOrderStockResult(List.of());
    }

    private Map<UUID, Integer> withServiceOrder(UUID serviceOrderId) {
        Map<UUID, Integer> quantityByStock = new TreeMap<>();
        for (StockMovement movement : stockMovementRepository.findByServiceOrderId(serviceOrderId)) {
            int quantity = movement.getMovementType() == MovementType.OUT
                    ? movement.getQuantity()
                    : -movement.getQuantity();
            quantityByStock.merge(movement.getStockId(), quantity, Integer::sum);
        }
        return quantityByStock;
    }
}
