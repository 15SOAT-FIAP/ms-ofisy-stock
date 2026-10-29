package br.com.ofisy.ms_ofisy_stock.application.stockmovement.list;

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
class ListStockMovementServiceTest {

    @Mock
    private StockMovementRepository repository;

    @InjectMocks
    private ListStockMovementService listService;

    @Nested
    class Execute {

        @Test
        void shouldReturnPageOfMovements() {
            var pageable = PageRequest.of(0, 10);
            var movements = List.of(StockMovement.create(UUID.randomUUID(), MovementType.IN, 5, 10, 15),
                    StockMovement.create(UUID.randomUUID(), MovementType.OUT, 2, 15, 13));
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(movements, pageable, 2));

            Page<StockMovement> result = listService.execute(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(repository).findAll(pageable);
        }

        @Test
        void shouldReturnEmptyPageWhenThereAreNoMovements() {
            var pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));

            assertThat(listService.execute(pageable)).isEmpty();
        }
    }
}
