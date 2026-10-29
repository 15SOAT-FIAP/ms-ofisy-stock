package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stockmovement;

import br.com.ofisy.ms_ofisy_stock.application.stockmovement.list.ListStockMovementUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stockmovement.listbystock.ListByStockStockMovementUseCase;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockMovementController.class)
class StockMovementControllerTest {

    private static final String BASE_URL = "/api/v1/stock-movements";
    private static final UUID ID = UUID.randomUUID();
    private static final UUID STOCK_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListStockMovementUseCase listStockMovementUseCase;

    @MockitoBean
    private ListByStockStockMovementUseCase listByStockStockMovementUseCase;

    private StockMovement movement() {
        return StockMovement.reconstruct(ID, STOCK_ID, MovementType.OUT, 4, 10, 6,
                LocalDateTime.of(2026, 10, 17, 10, 0), LocalDateTime.of(2026, 10, 17, 10, 0));
    }

    @Nested
    class GetAllStockMovements {

        @Test
        void shouldReturn200WithPageOfMovementsUsingDefaultSizeOf10() throws Exception {
            var pageable = PageRequest.of(0, 10);
            when(listStockMovementUseCase.execute(pageable)).thenReturn(new PageImpl<>(List.of(movement()), pageable, 1));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                    .andExpect(jsonPath("$.content[0].stockId").value(STOCK_ID.toString()))
                    .andExpect(jsonPath("$.content[0].movementType").value("OUT"))
                    .andExpect(jsonPath("$.content[0].quantity").value(4))
                    .andExpect(jsonPath("$.content[0].previousQuantity").value(10))
                    .andExpect(jsonPath("$.content[0].newQuantity").value(6))
                    .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-17T10:00:00"));

            verifyNoInteractions(listByStockStockMovementUseCase);
        }
    }

    @Nested
    class GetByStockId {

        @Test
        void shouldReturn200WithMovementsOfTheStock() throws Exception {
            var pageable = PageRequest.of(0, 10);
            when(listByStockStockMovementUseCase.execute(STOCK_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(movement()), pageable, 1));

            mockMvc.perform(get(BASE_URL).param("stockId", STOCK_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].stockId").value(STOCK_ID.toString()));

            verifyNoInteractions(listStockMovementUseCase);
        }

        @Test
        void shouldReturn400WhenStockIdIsNotUuid() throws Exception {
            mockMvc.perform(get(BASE_URL).param("stockId", "not-a-uuid"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(listByStockStockMovementUseCase);
        }
    }
}
