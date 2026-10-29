package br.com.ofisy.ms_ofisy_stock.adapters.gateways.stockmovement;

import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementRepositoryImplTest {

    @Mock
    private JpaStockMovementRepository jpa;

    private StockMovementRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new StockMovementRepositoryImpl(jpa);
    }

    private StockMovementEntity persistedEntity(UUID id, UUID stockId) {
        return StockMovementEntity.builder()
                .id(id)
                .stockId(stockId)
                .movementType(MovementType.IN)
                .quantity(10)
                .previousQuantity(90)
                .newQuantity(100)
                .createdAt(LocalDateTime.of(2026, 1, 10, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 10, 8, 0))
                .build();
    }

    @Test
    void shouldSaveStockMovementAndReturnPersistedDomain() {
        var id = UUID.randomUUID();
        var stockId = UUID.randomUUID();
        var movement = StockMovement.create(stockId, MovementType.IN, 10, 90, 100);
        when(jpa.save(any(StockMovementEntity.class))).thenReturn(persistedEntity(id, stockId));

        var result = repository.save(movement);

        var captor = ArgumentCaptor.forClass(StockMovementEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getStockId()).isEqualTo(stockId);
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.IN);
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void shouldFindAllStockMovements() {
        var pageable = PageRequest.of(0, 10);
        var id = UUID.randomUUID();
        when(jpa.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(persistedEntity(id, UUID.randomUUID())), pageable, 1));

        var result = repository.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(StockMovement::getId).containsExactly(id);
    }

    @Test
    void shouldFindStockMovementsByStockId() {
        var pageable = PageRequest.of(0, 5);
        var stockId = UUID.randomUUID();
        when(jpa.findByStockId(stockId, pageable))
                .thenReturn(new PageImpl<>(List.of(persistedEntity(UUID.randomUUID(), stockId)), pageable, 1));

        var result = repository.findByStockId(stockId, pageable);

        assertThat(result.getContent()).extracting(StockMovement::getStockId).containsExactly(stockId);
    }

    @Test
    void shouldReturnEmptyPageWhenStockHasNoMovements() {
        var pageable = PageRequest.of(0, 5);
        var stockId = UUID.randomUUID();
        when(jpa.findByStockId(stockId, pageable)).thenReturn(Page.empty(pageable));

        assertThat(repository.findByStockId(stockId, pageable)).isEmpty();
    }
}
