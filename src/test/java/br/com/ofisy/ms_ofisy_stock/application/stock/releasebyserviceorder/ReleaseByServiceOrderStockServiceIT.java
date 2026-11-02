package br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase.ReleaseByServiceOrderStockCommand;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "jwt.secret=" + JwtTestTokens.SECRET)
class ReleaseByServiceOrderStockServiceIT {

    @Autowired
    private ReleaseByServiceOrderStockUseCase releaseByServiceOrderStockUseCase;

    @Autowired
    private ConsumeStockUseCase consumeStockUseCase;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private UUID serviceOrderId;
    private UUID stockId;

    @BeforeEach
    void setUp() {
        serviceOrderId = UUID.randomUUID();
        stockId = stockRepository.save(Stock.create("Produto " + UUID.randomUUID(), "Produto de teste", 10,
                new BigDecimal("50.00"), "Testes", 3)).getId();
    }

    private int currentQuantity() {
        return stockRepository.findById(stockId).orElseThrow().getQuantity();
    }

    @Test
    void shouldReturnConsumedItemsAndLinkMovementsToServiceOrder() {
        consumeStockUseCase.execute(new ConsumeStockCommand(stockId, 4, serviceOrderId));
        assertThat(currentQuantity()).isEqualTo(6);

        releaseByServiceOrderStockUseCase.execute(new ReleaseByServiceOrderStockCommand(serviceOrderId));

        assertThat(currentQuantity()).isEqualTo(10);
        assertThat(stockMovementRepository.findByServiceOrderId(serviceOrderId))
                .extracting(StockMovement::getMovementType, StockMovement::getQuantity)
                .containsExactlyInAnyOrder(tuple(MovementType.OUT, 4), tuple(MovementType.IN, 4));
    }

    @Test
    void shouldNotReturnItemsTwiceWhenReleaseIsRepeated() {
        consumeStockUseCase.execute(new ConsumeStockCommand(stockId, 4, serviceOrderId));

        releaseByServiceOrderStockUseCase.execute(new ReleaseByServiceOrderStockCommand(serviceOrderId));
        releaseByServiceOrderStockUseCase.execute(new ReleaseByServiceOrderStockCommand(serviceOrderId));

        assertThat(currentQuantity()).isEqualTo(10);
        assertThat(stockMovementRepository.findByServiceOrderId(serviceOrderId)).hasSize(2);
    }

    @Test
    void shouldIgnoreMovementsFromOtherServiceOrders() {
        consumeStockUseCase.execute(new ConsumeStockCommand(stockId, 4, serviceOrderId));
        consumeStockUseCase.execute(new ConsumeStockCommand(stockId, 3, UUID.randomUUID()));

        releaseByServiceOrderStockUseCase.execute(new ReleaseByServiceOrderStockCommand(serviceOrderId));

        assertThat(currentQuantity()).isEqualTo(7);
    }
}
