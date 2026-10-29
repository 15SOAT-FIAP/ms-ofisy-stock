package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stockmovement;

import br.com.ofisy.ms_ofisy_stock.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import br.com.ofisy.ms_ofisy_stock.shared.jwt.JwtTestTokens;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secret=" + JwtTestTokens.SECRET
)
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
            authorized()
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
            authorized()
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
            authorized()
                    .queryParam("stockId", UUID.randomUUID())
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(0));
        }
    }

    @Nested
    class Authorization {

        @Test
        void shouldReturn401WhenTokenIsMissing() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(401);
        }

        @ParameterizedTest
        @ValueSource(strings = {"MECHANIC", "ATTENDANT"})
        void shouldReturn403WhenRoleIsNotAllowed(String role) {
            getWithToken(JwtTestTokens.withRole(JwtTestTokens.SECRET, role)).statusCode(403);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", "STOCKMAN"})
        void shouldReturn200ForAllowedRoles(String role) {
            getWithToken(JwtTestTokens.withRole(JwtTestTokens.SECRET, role)).statusCode(200);
        }

        private ValidatableResponse getWithToken(String token) {
            return RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .queryParam("stockId", stockId)
                    .when()
                    .get(BASE_URL)
                    .then();
        }
    }

    private RequestSpecification authorized() {
        return RestAssured.given()
                .header("Authorization", "Bearer " + JwtTestTokens.withRole(JwtTestTokens.SECRET, "STOCKMAN"));
    }
}
