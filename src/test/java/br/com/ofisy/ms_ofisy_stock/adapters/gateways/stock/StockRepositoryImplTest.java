package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stock;

import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockRepositoryImplTest {

    @Mock
    private JpaStockRepository jpa;

    private StockRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new StockRepositoryImpl(jpa);
    }

    private StockEntity persistedEntity(UUID id) {
        return StockEntity.builder()
                .id(id)
                .productName("Radiador")
                .description("Radiador de água com reservatório")
                .quantity(10)
                .unitPrice(new BigDecimal("350.00"))
                .category("Arrefecimento")
                .minThreshold(2)
                .createdAt(LocalDateTime.of(2026, 1, 10, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 10, 8, 0))
                .build();
    }

    @Test
    void shouldSaveStockAndReturnPersistedDomain() {
        var id = UUID.randomUUID();
        var stock = Stock.create("Radiador", "Radiador de água com reservatório", 10,
                new BigDecimal("350.00"), "Arrefecimento", 2);
        when(jpa.save(any(StockEntity.class))).thenReturn(persistedEntity(id));

        var result = repository.save(stock);

        var captor = ArgumentCaptor.forClass(StockEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getProductName()).isEqualTo("Radiador");
        assertThat(captor.getValue().getQuantity()).isEqualTo(10);
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void shouldFindAllStocks() {
        var pageable = PageRequest.of(0, 10);
        var id = UUID.randomUUID();
        when(jpa.findAll(pageable)).thenReturn(new PageImpl<>(List.of(persistedEntity(id)), pageable, 1));

        var result = repository.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(Stock::getId).containsExactly(id);
    }

    @Test
    void shouldFindStockById() {
        var id = UUID.randomUUID();
        when(jpa.findById(id)).thenReturn(Optional.of(persistedEntity(id)));

        var result = repository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    void shouldReturnEmptyWhenStockIsNotFoundById() {
        var id = UUID.randomUUID();
        when(jpa.findById(id)).thenReturn(Optional.empty());

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void shouldFindStockByProductName() {
        var id = UUID.randomUUID();
        when(jpa.findByProductName("Radiador")).thenReturn(Optional.of(persistedEntity(id)));

        var result = repository.findByProductName("Radiador");

        assertThat(result).isPresent();
        assertThat(result.get().getProductName()).isEqualTo("Radiador");
    }

    @Test
    void shouldReturnEmptyWhenStockIsNotFoundByProductName() {
        when(jpa.findByProductName("Inexistente")).thenReturn(Optional.empty());

        assertThat(repository.findByProductName("Inexistente")).isEmpty();
    }
}
