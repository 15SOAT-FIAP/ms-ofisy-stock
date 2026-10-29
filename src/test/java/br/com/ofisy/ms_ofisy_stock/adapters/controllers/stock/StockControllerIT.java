package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock;

import br.com.ofisy.ms_ofisy_stock.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import br.com.ofisy.ms_ofisy_stock.shared.jwt.JwtTestTokens;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secret=" + JwtTestTokens.SECRET
)
class StockControllerIT {

    private static final String BASE_URL = "/api/v1/stocks";
    private static final String SEEDED_STOCK_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234560101";

    @LocalServerPort
    private int port;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private UUID stockId;
    private String productName;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        productName = "Produto " + UUID.randomUUID();
        stockId = stockRepository.save(Stock.create(productName, "Produto de teste", 10,
                new BigDecimal("50.00"), "Testes", 3)).getId();
    }

    private String createBody(String name) {
        return """
                {
                  "productName": "%s",
                  "description": "Filtro de ar do motor",
                  "quantity": 8,
                  "unitPrice": 42.50,
                  "category": "Motor",
                  "minThreshold": 2
                }
                """.formatted(name);
    }

    @Nested
    class GetAllStocks {

        @Test
        void shouldReturnFirstPageWithDefaultSizeOf10() {
            authorized()
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(10));
        }

        @Test
        void shouldRespectRequestedPageSize() {
            authorized()
                    .queryParam("size", 3)
                    .when()
                    .get(BASE_URL)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(3));
        }
    }

    @Nested
    class GetStockById {

        @Test
        void shouldReturnSeededStock() {
            authorized()
                    .when()
                    .get(BASE_URL + "/{id}", SEEDED_STOCK_ID)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(SEEDED_STOCK_ID))
                    .body("productName", equalTo("Radiador"))
                    .body("category", equalTo("Arrefecimento"));
        }

        @Test
        void shouldReturn404WhenStockDoesNotExist() {
            authorized()
                    .when()
                    .get(BASE_URL + "/{id}", UUID.randomUUID())
                    .then()
                    .statusCode(404)
                    .body("title", equalTo("Estoque não encontrado"));
        }
    }

    @Nested
    class GetByProductName {

        @Test
        void shouldReturnStockByProductName() {
            authorized()
                    .when()
                    .get(BASE_URL + "/productName/{productName}", productName)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(stockId.toString()));
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateStockAndPersistIt() {
            String name = "Filtro de ar " + UUID.randomUUID();

            String id = authorized()
                    .contentType(ContentType.JSON)
                    .body(createBody(name))
                    .when()
                    .post(BASE_URL)
                    .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("productName", equalTo(name))
                    .body("quantity", equalTo(8))
                    .body("isLowStock", equalTo(false))
                    .extract().path("id");

            assertThat(stockRepository.findById(UUID.fromString(id))).isPresent();
        }

        @Test
        void shouldReturn409WhenProductNameAlreadyExists() {
            authorized()
                    .contentType(ContentType.JSON)
                    .body(createBody(productName))
                    .when()
                    .post(BASE_URL)
                    .then()
                    .statusCode(409)
                    .body("title", equalTo("Estoque já existe"));
        }

        @Test
        void shouldReturn400WhenRequiredFieldsAreMissing() {
            authorized()
                    .contentType(ContentType.JSON)
                    .body("{\"productName\": \"Sem descrição\"}")
                    .when()
                    .post(BASE_URL)
                    .then()
                    .statusCode(400)
                    .body("title", equalTo("Erro de validação"))
                    .body("errors.description", equalTo("Descrição é obrigatória"))
                    .body("errors.category", equalTo("Categoria é obrigatória"));
        }
    }

    @Nested
    class AddStock {

        @Test
        void shouldAddQuantityAndRegisterInMovement() {
            authorized()
                    .queryParam("quantity", 5)
                    .when()
                    .post(BASE_URL + "/{id}/add", stockId)
                    .then()
                    .statusCode(200)
                    .body("quantity", equalTo(15));

            var movements = stockMovementRepository.findByStockId(stockId, PageRequest.of(0, 10)).getContent();
            assertThat(movements).singleElement().satisfies(movement -> {
                assertThat(movement.getMovementType()).isEqualTo(MovementType.IN);
                assertThat(movement.getQuantity()).isEqualTo(5);
                assertThat(movement.getPreviousQuantity()).isEqualTo(10);
                assertThat(movement.getNewQuantity()).isEqualTo(15);
            });
        }

        @Test
        void shouldReturn400WhenQuantityIsNotPositive() {
            authorized()
                    .queryParam("quantity", 0)
                    .when()
                    .post(BASE_URL + "/{id}/add", stockId)
                    .then()
                    .statusCode(400)
                    .body("title", equalTo("Quantidade inválida"));

            assertThat(stockRepository.findById(stockId).orElseThrow().getQuantity()).isEqualTo(10);
        }
    }

    @Nested
    class ConsumeStock {

        @Test
        void shouldConsumeQuantityAndRegisterOutMovement() {
            authorized()
                    .queryParam("quantity", 8)
                    .when()
                    .post(BASE_URL + "/{id}/consume", stockId)
                    .then()
                    .statusCode(200)
                    .body("quantity", equalTo(2))
                    .body("isLowStock", equalTo(true));

            var movements = stockMovementRepository.findByStockId(stockId, PageRequest.of(0, 10)).getContent();
            assertThat(movements).extracting(StockMovement::getMovementType).containsExactly(MovementType.OUT);
        }

        @Test
        void shouldReturn409AndKeepQuantityWhenStockIsInsufficient() {
            authorized()
                    .queryParam("quantity", 11)
                    .when()
                    .post(BASE_URL + "/{id}/consume", stockId)
                    .then()
                    .statusCode(409)
                    .body("title", equalTo("Estoque insuficiente"));

            assertThat(stockRepository.findById(stockId).orElseThrow().getQuantity()).isEqualTo(10);
            assertThat(stockMovementRepository.findByStockId(stockId, PageRequest.of(0, 10))).isEmpty();
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateOnlyInformedFields() {
            authorized()
                    .contentType(ContentType.JSON)
                    .body("{\"unitPrice\": 65.00, \"category\": \"Filtros\"}")
                    .when()
                    .put(BASE_URL + "/{id}", stockId)
                    .then()
                    .statusCode(200)
                    .body("productName", equalTo(productName))
                    .body("category", equalTo("Filtros"))
                    .body("unitPrice", equalTo(65.0f));
        }

        @Test
        void shouldReturn409WhenNewProductNameBelongsToAnotherStock() {
            authorized()
                    .contentType(ContentType.JSON)
                    .body("{\"productName\": \"Radiador\"}")
                    .when()
                    .put(BASE_URL + "/{id}", stockId)
                    .then()
                    .statusCode(409)
                    .body("title", equalTo("Estoque já existe"));
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
                    .statusCode(401)
                    .body("status", equalTo(401))
                    .body("title", equalTo("Usuário não autorizado"));
        }

        @Test
        void shouldReturn401WhenTokenIsSignedWithAnotherSecret() {
            getWithToken(JwtTestTokens.withRole(JwtTestTokens.OTHER_SECRET, "STOCKMAN")).statusCode(401);
        }

        @Test
        void shouldReturn401WhenTokenIsExpired() {
            getWithToken(JwtTestTokens.expired(JwtTestTokens.SECRET, "STOCKMAN")).statusCode(401);
        }

        @Test
        void shouldReturn401WhenTokenWasIssuedForACustomer() {
            getWithToken(JwtTestTokens.withIssuer(JwtTestTokens.SECRET, JwtTestTokens.CUSTOMER_ISSUER, "STOCKMAN"))
                    .statusCode(401);
        }

        @Test
        void shouldReturn403WhenRoleIsNotAllowed() {
            getWithToken(JwtTestTokens.withRole(JwtTestTokens.SECRET, "ATTENDANT"))
                    .statusCode(403)
                    .body("status", equalTo(403))
                    .body("title", equalTo("Usuário sem permissão"));
        }

        @Test
        void shouldReturn403WhenTokenHasNoRole() {
            getWithToken(JwtTestTokens.withoutRole(JwtTestTokens.SECRET)).statusCode(403);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", "STOCKMAN", "MECHANIC"})
        void shouldReturn200OnQueriesForAllowedRoles(String role) {
            getWithToken(JwtTestTokens.withRole(JwtTestTokens.SECRET, role)).statusCode(200);
        }

        @Test
        void shouldReturn403AndKeepQuantityWhenMechanicTriesToConsume() {
            RestAssured.given()
                    .header("Authorization", "Bearer " + JwtTestTokens.withRole(JwtTestTokens.SECRET, "MECHANIC"))
                    .queryParam("quantity", 5)
                    .when()
                    .post(BASE_URL + "/{id}/consume", stockId)
                    .then()
                    .statusCode(403);

            assertThat(stockRepository.findById(stockId).orElseThrow().getQuantity()).isEqualTo(10);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", "STOCKMAN"})
        void shouldAllowChangesForAllowedRoles(String role) {
            RestAssured.given()
                    .header("Authorization", "Bearer " + JwtTestTokens.withRole(JwtTestTokens.SECRET, role))
                    .queryParam("quantity", 1)
                    .when()
                    .post(BASE_URL + "/{id}/add", stockId)
                    .then()
                    .statusCode(200)
                    .body("quantity", equalTo(11));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/actuator/health", "/v3/api-docs"})
        void shouldAllowPublicPathsWithoutToken(String path) {
            RestAssured.given()
                    .when()
                    .get(path)
                    .then()
                    .statusCode(200);
        }

        private ValidatableResponse getWithToken(String token) {
            return RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(BASE_URL + "/{id}", stockId)
                    .then();
        }
    }

    private RequestSpecification authorized() {
        return RestAssured.given()
                .header("Authorization", "Bearer " + JwtTestTokens.withRole(JwtTestTokens.SECRET, "STOCKMAN"));
    }
}
