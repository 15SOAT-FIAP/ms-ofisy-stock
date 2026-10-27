package br.com.ofisy.ms_ofisy_stock.application.stockmovement.listbystock;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListByStockStockMovementServiceTest {

    private static final UUID STOCK_ID = UUID.randomUUID();

    @Mock
    private StockMovementRepository repository;

    @InjectMocks
    private ListByStockStockMovementService listByStockService;

    @Nested
    class Execute {

        @Test
        void shouldReturnMovementsOfTheInformedStock() {
            var pageable = PageRequest.of(0, 5);
            var movements = List.of(StockMovement.create(STOCK_ID, MovementType.IN, 5, 10, 15),
                    StockMovement.create(STOCK_ID, MovementType.OUT, 2, 15, 13));
            when(repository.findByStockId(STOCK_ID, pageable)).thenReturn(new PageImpl<>(movements, pageable, 2));

            Page<StockMovement> result = listByStockService.execute(STOCK_ID, pageable);

            assertThat(result.getContent()).extracting(StockMovement::getStockId).containsOnly(STOCK_ID);
            verify(repository).findByStockId(STOCK_ID, pageable);
        }

        @Test
        void shouldReturnEmptyPageWhenStockHasNoMovements() {
            var pageable = PageRequest.of(0, 5);
            when(repository.findByStockId(STOCK_ID, pageable)).thenReturn(Page.empty(pageable));

            assertThat(listByStockService.execute(STOCK_ID, pageable)).isEmpty();
        }
    }
}
