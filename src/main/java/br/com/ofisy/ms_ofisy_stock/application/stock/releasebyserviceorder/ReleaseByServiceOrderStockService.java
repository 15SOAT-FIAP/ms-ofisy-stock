package br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase.ReleaseStockCommand;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReleaseByServiceOrderStockService implements ReleaseByServiceOrderStockUseCase {

    private final StockMovementRepository stockMovementRepository;
    private final ReleaseStockUseCase releaseStockUseCase;

    // Devolve só o que saiu para a OS e ainda não voltou, então repetir a devolução não altera o saldo
    @Override
    public List<Stock> execute(ReleaseByServiceOrderStockCommand cmd) {
        Map<UUID, Integer> pendingByStock = new LinkedHashMap<>();
        for (StockMovement movement : stockMovementRepository.findByServiceOrderId(cmd.serviceOrderId())) {
            int quantity = movement.getMovementType() == MovementType.OUT
                    ? movement.getQuantity()
                    : -movement.getQuantity();
            pendingByStock.merge(movement.getStockId(), quantity, Integer::sum);
        }

        return pendingByStock.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> releaseStockUseCase.execute(
                        new ReleaseStockCommand(entry.getKey(), entry.getValue(), cmd.serviceOrderId())))
                .toList();
    }
}
