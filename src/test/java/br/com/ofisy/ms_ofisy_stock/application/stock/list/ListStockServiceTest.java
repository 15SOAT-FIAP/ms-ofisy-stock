package br.com.ofisy.ms_ofisy_stock.application.stock.list;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
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

import static br.com.ofisy.ms_ofisy_stock.application.stock.StockFixture.persistedStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListStockServiceTest {

    @Mock
    private StockRepository repository;

    @InjectMocks
    private ListStockService listService;

    @Nested
    class Execute {

        @Test
        void shouldReturnPageOfStocks() {
            var pageable = PageRequest.of(1, 5);
            var stocks = List.of(persistedStock(UUID.randomUUID(), "Radiador", 10),
                    persistedStock(UUID.randomUUID(), "Alternador", 4));
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(stocks, pageable, 12));

            Page<Stock> result = listService.execute(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(12);
            verify(repository).findAll(pageable);
        }

        @Test
        void shouldReturnEmptyPageWhenThereAreNoStocks() {
            var pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));

            assertThat(listService.execute(pageable)).isEmpty();
        }
    }
}
