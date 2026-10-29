package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock;

import br.com.ofisy.ms_ofisy_stock.application.stock.add.AddStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.add.AddStockUseCase.AddStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.create.CreateStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.InsufficientStockException;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockAlreadyExistsException;
import br.com.ofisy.ms_ofisy_stock.application.stock.exception.StockNotFoundException;
import br.com.ofisy.ms_ofisy_stock.application.stock.identifybyid.IdentifyByIdStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.identifybyproduct.IdentifyByProductStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.list.ListStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.update.UpdateStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.update.UpdateStockUseCase.UpdateStockCommand;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
class StockControllerTest {

    private static final String BASE_URL = "/api/v1/stocks";
    private static final UUID ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateStockUseCase createStockUseCase;

    @MockitoBean
    private UpdateStockUseCase updateStockUseCase;

    @MockitoBean
    private AddStockUseCase addStockUseCase;

    @MockitoBean
    private ConsumeStockUseCase consumeStockUseCase;

    @MockitoBean
    private ListStockUseCase listStockUseCase;

    @MockitoBean
    private IdentifyByIdStockUseCase identifyByIdStockUseCase;

    @MockitoBean
    private IdentifyByProductStockUseCase identifyByProductStockUseCase;

    private Stock stock(Integer quantity) {
        return Stock.reconstruct(ID, "Radiador", "Radiador de água com reservatório", quantity,
                new BigDecimal("350.00"), "Arrefecimento", 2,
                LocalDateTime.of(2026, 10, 17, 10, 0), LocalDateTime.of(2026, 10, 17, 11, 0));
    }

    @Nested
    class GetAllStocks {

        @Test
        void shouldReturn200WithPageOfStocksUsingDefaultSizeOf10() throws Exception {
            var pageable = PageRequest.of(0, 10);
            when(listStockUseCase.execute(pageable)).thenReturn(new PageImpl<>(List.of(stock(10)), pageable, 1));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                    .andExpect(jsonPath("$.content[0].productName").value("Radiador"));
        }
    }

    @Nested
    class GetStockById {

        @Test
        void shouldReturn200WithStock() throws Exception {
            when(identifyByIdStockUseCase.execute(ID)).thenReturn(stock(10));

            mockMvc.perform(get(BASE_URL + "/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.productName").value("Radiador"))
                    .andExpect(jsonPath("$.description").value("Radiador de água com reservatório"))
                    .andExpect(jsonPath("$.quantity").value(10))
                    .andExpect(jsonPath("$.unitPrice").value(350.00))
                    .andExpect(jsonPath("$.category").value("Arrefecimento"))
                    .andExpect(jsonPath("$.minThreshold").value(2))
                    .andExpect(jsonPath("$.isLowStock").value(false))
                    .andExpect(jsonPath("$.createdAt").value("2026-10-17T10:00:00"))
                    .andExpect(jsonPath("$.updatedAt").value("2026-10-17T11:00:00"));
        }

        @Test
        void shouldReturnIsLowStockTrueWhenQuantityReachesMinThreshold() throws Exception {
            when(identifyByIdStockUseCase.execute(ID)).thenReturn(stock(2));

            mockMvc.perform(get(BASE_URL + "/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isLowStock").value(true));
        }

        @Test
        void shouldReturn404WhenStockDoesNotExist() throws Exception {
            when(identifyByIdStockUseCase.execute(ID)).thenThrow(new StockNotFoundException(ID));

            mockMvc.perform(get(BASE_URL + "/{id}", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Estoque não encontrado"))
                    .andExpect(jsonPath("$.detail").value("Estoque não encontrado: ID " + ID));
        }

        @Test
        void shouldReturn400WhenIdIsNotUuid() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(identifyByIdStockUseCase);
        }
    }

    @Nested
    class GetByProductName {

        @Test
        void shouldReturn200WithStock() throws Exception {
            when(identifyByProductStockUseCase.execute("Radiador")).thenReturn(stock(10));

            mockMvc.perform(get(BASE_URL + "/productName/{productName}", "Radiador"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productName").value("Radiador"));
        }

        @Test
        void shouldReturn404WhenProductDoesNotExist() throws Exception {
            when(identifyByProductStockUseCase.execute("Inexistente")).thenThrow(new StockNotFoundException("Inexistente"));

            mockMvc.perform(get(BASE_URL + "/productName/{productName}", "Inexistente"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Estoque não encontrado: Produto Inexistente"));
        }
    }

    @Nested
    class Create {

        private static final String VALID_BODY = """
                {
                  "productName": "Radiador",
                  "description": "Radiador de água com reservatório",
                  "quantity": 10,
                  "unitPrice": 350.00,
                  "category": "Arrefecimento",
                  "minThreshold": 2
                }
                """;

        @Test
        void shouldReturn201WithCreatedStock() throws Exception {
            when(createStockUseCase.execute(new CreateStockUseCase.CreateStockCommand("Radiador",
                    "Radiador de água com reservatório", 10, new BigDecimal("350.00"), "Arrefecimento", 2)))
                    .thenReturn(stock(10));

            mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.productName").value("Radiador"));
        }

        @Test
        void shouldReturn400WithFieldErrorsWhenBodyIsInvalid() throws Exception {
            String body = """
                    {
                      "productName": " ",
                      "quantity": -1,
                      "unitPrice": 0,
                      "category": "Arrefecimento",
                      "minThreshold": -1
                    }
                    """;

            mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Erro de validação"))
                    .andExpect(jsonPath("$.errors.productName").value("Nome do produto é obrigatório"))
                    .andExpect(jsonPath("$.errors.description").value("Descrição é obrigatória"))
                    .andExpect(jsonPath("$.errors.quantity").value("Quantidade não pode ser negativa"))
                    .andExpect(jsonPath("$.errors.unitPrice").value("Preço deve ser maior que zero"))
                    .andExpect(jsonPath("$.errors.minThreshold").value("Limite mínimo não pode ser negativo"));

            verifyNoInteractions(createStockUseCase);
        }

        @Test
        void shouldReturn400WhenBodyIsMalformed() throws Exception {
            mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": \"dez\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Erro de validação"));

            verifyNoInteractions(createStockUseCase);
        }

        @Test
        void shouldReturn409WhenProductAlreadyExists() throws Exception {
            when(createStockUseCase.execute(any())).thenThrow(new StockAlreadyExistsException("Radiador"));

            mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Estoque já existe"))
                    .andExpect(jsonPath("$.detail").value("Já existe um estoque cadastrado para o produto Radiador"));
        }
    }

    @Nested
    class AddStock {

        @Test
        void shouldReturn200WithUpdatedQuantity() throws Exception {
            when(addStockUseCase.execute(new AddStockCommand(ID, 5))).thenReturn(stock(15));

            mockMvc.perform(post(BASE_URL + "/{id}/add", ID).param("quantity", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity").value(15));
        }

        @Test
        void shouldReturn400WhenQuantityIsInvalid() throws Exception {
            when(addStockUseCase.execute(new AddStockCommand(ID, 0))).thenThrow(new InvalidStockQuantityException(0));

            mockMvc.perform(post(BASE_URL + "/{id}/add", ID).param("quantity", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Quantidade inválida"));
        }

        @Test
        void shouldReturn400WhenQuantityIsMissing() throws Exception {
            mockMvc.perform(post(BASE_URL + "/{id}/add", ID))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(addStockUseCase);
        }

        @Test
        void shouldReturn404WhenStockDoesNotExist() throws Exception {
            when(addStockUseCase.execute(new AddStockCommand(ID, 5))).thenThrow(new StockNotFoundException(ID));

            mockMvc.perform(post(BASE_URL + "/{id}/add", ID).param("quantity", "5"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ConsumeStock {

        @Test
        void shouldReturn200WithUpdatedQuantity() throws Exception {
            when(consumeStockUseCase.execute(new ConsumeStockCommand(ID, 4))).thenReturn(stock(6));

            mockMvc.perform(post(BASE_URL + "/{id}/consume", ID).param("quantity", "4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity").value(6));
        }

        @Test
        void shouldReturn409WhenStockIsInsufficient() throws Exception {
            when(consumeStockUseCase.execute(new ConsumeStockCommand(ID, 11))).thenThrow(new InsufficientStockException(ID));

            mockMvc.perform(post(BASE_URL + "/{id}/consume", ID).param("quantity", "11"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Estoque insuficiente"))
                    .andExpect(jsonPath("$.detail").value("Estoque insuficiente: ID " + ID));
        }
    }

    @Nested
    class Update {

        @Test
        void shouldReturn200WithUpdatedStock() throws Exception {
            when(updateStockUseCase.execute(new UpdateStockCommand(ID, null, null, new BigDecimal("400.00"), null, 3)))
                    .thenReturn(stock(10));

            mockMvc.perform(put(BASE_URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"unitPrice\": 400.00, \"minThreshold\": 3}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()));
        }

        @Test
        void shouldReturn400WhenTextFieldsAreBlank() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productName\": \"\", \"description\": \"  \", \"category\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.productName").value("Nome do produto não pode ficar em branco"))
                    .andExpect(jsonPath("$.errors.description").value("Descrição não pode ficar em branco"))
                    .andExpect(jsonPath("$.errors.category").value("Categoria não pode ficar em branco"));

            verify(updateStockUseCase, never()).execute(any());
        }

        @Test
        void shouldReturn409WhenNewProductNameBelongsToAnotherStock() throws Exception {
            when(updateStockUseCase.execute(any())).thenThrow(new StockAlreadyExistsException("Alternador"));

            mockMvc.perform(put(BASE_URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productName\": \"Alternador\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldReturn404WhenStockDoesNotExist() throws Exception {
            when(updateStockUseCase.execute(any())).thenThrow(new StockNotFoundException(ID));

            mockMvc.perform(put(BASE_URL + "/{id}", ID).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"category\": \"Motor\"}"))
                    .andExpect(status().isNotFound());
        }
    }
}
