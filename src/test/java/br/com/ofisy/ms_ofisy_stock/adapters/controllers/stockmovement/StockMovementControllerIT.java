package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stockmovement;

import br.com.ofisy.ms_ofisy_stock.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockMovementControllerIT {

    private static final String BASE_URL = "/api/v1/stock-movements";

    @LocalServerPort
    private int port;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private UUID stockId;
    private UUID inMovementId;
    private UUID outMovementId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        stockId = stockRepository.save(Stock.create("Produto " + UUID.randomUUID(), "Produto de teste", 10,
                new BigDecimal("50.00"), "Testes", 3)).getId();
        inMovementId = stockMovementRepository.save(StockMovement.create(stockId, MovementType.IN, 5, 10, 15)).getId();
        outMovementId = stockMovementRepository.save(StockMovement.create(stockId, MovementType.OUT, 3, 15, 12)).getId();
    }

    @Nested
    class GetAllStockMovements {

        @Test
        void shouldReturnPageOfMovements() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("totalElements", greaterThanOrEqualTo(2));
        }
    }

    @Nested
    class GetByStockId {

        @Test
        void shouldReturnOnlyMovementsOfTheStock() {
            RestAssured.given()
                    .queryParam("stockId", stockId)
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(2))
                    .body("content.id", containsInAnyOrder(inMovementId.toString(), outMovementId.toString()))
                    .body("content.stockId", everyItem(equalTo(stockId.toString())));
        }

        @Test
        void shouldReturnEmptyPageWhenStockHasNoMovements() {
            RestAssured.given()
                    .queryParam("stockId", UUID.randomUUID())
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(0));
        }
    }
}
