package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ConsumeByServiceOrderStockService implements ConsumeByServiceOrderStockUseCase {

    private final StockRepository repository;
    private final ConsumeStockUseCase consumeStockUseCase;

    // Valida todos os itens antes de consumir: se algum faltar, nada sai do estoque.
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

        List<MissingStockItem> missingItems = requestedByStock.entrySet().stream()
                .flatMap(entry -> findMissing(entry.getKey(), entry.getValue()).stream())
                .toList();
        if (!missingItems.isEmpty()) {
            return new ConsumeByServiceOrderStockResult(List.of(), missingItems);
        }

        List<Stock> consumedStocks = requestedByStock.entrySet().stream()
                .map(entry -> consumeStockUseCase.execute(
                        new ConsumeStockCommand(entry.getKey(), entry.getValue(), cmd.serviceOrderId())))
                .toList();
        return new ConsumeByServiceOrderStockResult(consumedStocks, List.of());
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
