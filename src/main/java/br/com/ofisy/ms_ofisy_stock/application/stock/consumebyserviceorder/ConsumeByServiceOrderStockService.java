package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ConsumeByServiceOrderStockService implements ConsumeByServiceOrderStockUseCase {

    private final StockRepository repository;
    private final StockMovementRepository stockMovementRepository;
    private final ConsumeStockUseCase consumeStockUseCase;

    // Valida todos os itens antes de consumir: se algum faltar, nada sai do estoque.
    // Consome só o que ainda não saiu para a OS, então repetir o consumo não altera o saldo.
    // Os itens são travados em ordem de id, para duas OS com itens em comum não se bloquearem mutuamente
    @Override
    public ConsumeByServiceOrderStockResult execute(ConsumeByServiceOrderStockCommand cmd) {
        Map<UUID, Integer> requestedByStock = new TreeMap<>();
        for (StockItem item : cmd.items()) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new InvalidStockQuantityException(item.quantity());
            }
            requestedByStock.merge(item.stockId(), item.quantity(), Integer::sum);
        }

        Map<UUID, Integer> alreadyConsumedByStock = consumedByServiceOrder(cmd.serviceOrderId());
        Map<UUID, Integer> pendingByStock = new TreeMap<>();
        requestedByStock.forEach((stockId, quantity) -> {
            int pending = quantity - alreadyConsumedByStock.getOrDefault(stockId, 0);
            if (pending > 0) {
                pendingByStock.put(stockId, pending);
            }
        });

        List<MissingStockItem> missingItems = pendingByStock.entrySet().stream()
                .flatMap(entry -> findMissing(entry.getKey(), entry.getValue()).stream())
                .toList();
        if (!missingItems.isEmpty()) {
            return new ConsumeByServiceOrderStockResult(List.of(), missingItems);
        }

        List<Stock> consumedStocks = pendingByStock.entrySet().stream()
                .map(entry -> consumeStockUseCase.execute(
                        new ConsumeStockCommand(entry.getKey(), entry.getValue(), cmd.serviceOrderId())))
                .toList();
        return new ConsumeByServiceOrderStockResult(consumedStocks, List.of());
    }

    // Conta todas as saídas da OS, mesmo as já devolvidas: um consumo repetido depois da devolução não baixa o saldo de novo
    private Map<UUID, Integer> consumedByServiceOrder(UUID serviceOrderId) {
        return stockMovementRepository.findByServiceOrderId(serviceOrderId).stream()
                .filter(movement -> movement.getMovementType() == MovementType.OUT)
                .collect(Collectors.groupingBy(StockMovement::getStockId,
                        Collectors.summingInt(StockMovement::getQuantity)));
    }

    private Optional<MissingStockItem> findMissing(UUID stockId, Integer requestedQuantity) {
        int availableQuantity = repository.lockById(stockId)
                .map(Stock::getQuantity)
                .orElse(0);
        return availableQuantity < requestedQuantity
                ? Optional.of(new MissingStockItem(stockId, requestedQuantity, availableQuantity))
                : Optional.empty();
    }
}
