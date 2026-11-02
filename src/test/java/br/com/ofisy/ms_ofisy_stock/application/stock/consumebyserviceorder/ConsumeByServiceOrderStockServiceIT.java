package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockResult;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import br.com.ofisy.ms_ofisy_stock.shared.jwt.JwtTestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "jwt.secret=" + JwtTestTokens.SECRET)
class ConsumeByServiceOrderStockServiceIT {

    @Autowired
    private ConsumeByServiceOrderStockUseCase consumeByServiceOrderStockUseCase;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private UUID serviceOrderId;
    private UUID radiatorId;
    private UUID filterId;

    @BeforeEach
    void setUp() {
        serviceOrderId = UUID.randomUUID();
        radiatorId = newStock(10);
        filterId = newStock(2);
    }

    private UUID newStock(int quantity) {
        return stockRepository.save(Stock.create("Produto " + UUID.randomUUID(), "Produto de teste", quantity,
                new BigDecimal("50.00"), "Testes", 1)).getId();
    }

    private int quantityOf(UUID stockId) {
        return stockRepository.findById(stockId).orElseThrow().getQuantity();
    }

    @Test
    void shouldConsumeAllItemsAndLinkMovementsToServiceOrder() {
        ConsumeByServiceOrderStockResult result = consumeByServiceOrderStockUseCase.execute(
                new ConsumeByServiceOrderStockCommand(serviceOrderId,
                        List.of(new StockItem(radiatorId, 4), new StockItem(filterId, 2))));

        assertThat(result.consumed()).isTrue();
        assertThat(quantityOf(radiatorId)).isEqualTo(6);
        assertThat(quantityOf(filterId)).isZero();
        assertThat(stockMovementRepository.findByServiceOrderId(serviceOrderId))
                .extracting(StockMovement::getStockId, StockMovement::getMovementType, StockMovement::getQuantity)
                .containsExactlyInAnyOrder(
                        tuple(radiatorId, MovementType.OUT, 4),
                        tuple(filterId, MovementType.OUT, 2));
    }

    @Test
    void shouldNotLoseUpdatesWhenServiceOrdersConsumeTheSameItemConcurrently() throws Exception {
        int serviceOrders = 8;
        ExecutorService executor = Executors.newFixedThreadPool(serviceOrders);
        try {
            List<Future<ConsumeByServiceOrderStockResult>> results = executor.invokeAll(
                    IntStream.range(0, serviceOrders)
                            .mapToObj(i -> (Callable<ConsumeByServiceOrderStockResult>) () ->
                                    consumeByServiceOrderStockUseCase.execute(new ConsumeByServiceOrderStockCommand(
                                            UUID.randomUUID(), List.of(new StockItem(radiatorId, 1)))))
                            .toList());
            for (Future<ConsumeByServiceOrderStockResult> result : results) {
                assertThat(result.get().consumed()).isTrue();
            }
        } finally {
            executor.shutdown();
        }

        assertThat(quantityOf(radiatorId)).isEqualTo(2);
    }

    @Test
    void shouldKeepEveryBalanceWhenOneItemIsMissing() {
        ConsumeByServiceOrderStockResult result = consumeByServiceOrderStockUseCase.execute(
                new ConsumeByServiceOrderStockCommand(serviceOrderId,
                        List.of(new StockItem(radiatorId, 4), new StockItem(filterId, 3))));

        assertThat(result.consumed()).isFalse();
        assertThat(result.missingItems()).containsExactly(new MissingStockItem(filterId, 3, 2));
        assertThat(quantityOf(radiatorId)).isEqualTo(10);
        assertThat(quantityOf(filterId)).isEqualTo(2);
        assertThat(stockMovementRepository.findByServiceOrderId(serviceOrderId)).isEmpty();
    }
}
