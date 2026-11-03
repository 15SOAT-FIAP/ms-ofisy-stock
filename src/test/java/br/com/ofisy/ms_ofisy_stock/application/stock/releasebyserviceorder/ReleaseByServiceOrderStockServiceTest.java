package br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase.ReleaseStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase.ReleaseByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.releasebyserviceorder.ReleaseByServiceOrderStockUseCase.ReleasedStockItem;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseByServiceOrderStockServiceTest {

    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID RADIATOR_ID = UUID.randomUUID();
    private static final UUID FILTER_ID = UUID.randomUUID();

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ReleaseStockUseCase releaseStockUseCase;

    @InjectMocks
    private ReleaseByServiceOrderStockService releaseByServiceOrderService;

    private StockMovement movement(UUID stockId, MovementType type, int quantity) {
        return StockMovement.create(stockId, type, quantity, 10, 10, SERVICE_ORDER_ID);
    }

    @Nested
    class Execute {

        @Test
        void shouldReleaseEveryItemConsumedByServiceOrder() {
            when(stockMovementRepository.findByServiceOrderId(SERVICE_ORDER_ID)).thenReturn(List.of(
                    movement(RADIATOR_ID, MovementType.OUT, 2),
                    movement(FILTER_ID, MovementType.OUT, 3)));

            List<ReleasedStockItem> result = releaseByServiceOrderService.execute(new ReleaseByServiceOrderStockCommand(SERVICE_ORDER_ID));

            assertThat(result).containsExactlyInAnyOrder(
                    new ReleasedStockItem(RADIATOR_ID, 2),
                    new ReleasedStockItem(FILTER_ID, 3));
            verify(releaseStockUseCase).execute(new ReleaseStockCommand(RADIATOR_ID, 2, SERVICE_ORDER_ID));
            verify(releaseStockUseCase).execute(new ReleaseStockCommand(FILTER_ID, 3, SERVICE_ORDER_ID));
        }

        @Test
        void shouldReleaseItemsInStockIdOrder() {
            UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            when(stockMovementRepository.findByServiceOrderId(SERVICE_ORDER_ID)).thenReturn(List.of(
                    movement(higherId, MovementType.OUT, 1),
                    movement(lowerId, MovementType.OUT, 1)));

            List<ReleasedStockItem> result = releaseByServiceOrderService.execute(new ReleaseByServiceOrderStockCommand(SERVICE_ORDER_ID));

            assertThat(result).extracting(ReleasedStockItem::stockId).containsExactly(lowerId, higherId);
        }

        @Test
        void shouldReleaseOnlyWhatHasNotBeenReturnedYet() {
            when(stockMovementRepository.findByServiceOrderId(SERVICE_ORDER_ID)).thenReturn(List.of(
                    movement(RADIATOR_ID, MovementType.OUT, 2),
                    movement(RADIATOR_ID, MovementType.OUT, 3),
                    movement(RADIATOR_ID, MovementType.IN, 1)));

            releaseByServiceOrderService.execute(new ReleaseByServiceOrderStockCommand(SERVICE_ORDER_ID));

            verify(releaseStockUseCase).execute(new ReleaseStockCommand(RADIATOR_ID, 4, SERVICE_ORDER_ID));
            verifyNoMoreInteractions(releaseStockUseCase);
        }

        @Test
        void shouldNotReleaseAgainWhenServiceOrderWasAlreadyReleased() {
            when(stockMovementRepository.findByServiceOrderId(SERVICE_ORDER_ID)).thenReturn(List.of(
                    movement(RADIATOR_ID, MovementType.OUT, 2),
                    movement(RADIATOR_ID, MovementType.IN, 2)));

            List<ReleasedStockItem> result = releaseByServiceOrderService.execute(new ReleaseByServiceOrderStockCommand(SERVICE_ORDER_ID));

            assertThat(result).isEmpty();
            verify(releaseStockUseCase, never()).execute(any());
        }

        @Test
        void shouldDoNothingWhenServiceOrderHasNoMovements() {
            when(stockMovementRepository.findByServiceOrderId(SERVICE_ORDER_ID)).thenReturn(List.of());

            List<ReleasedStockItem> result = releaseByServiceOrderService.execute(new ReleaseByServiceOrderStockCommand(SERVICE_ORDER_ID));

            assertThat(result).isEmpty();
            verify(releaseStockUseCase, never()).execute(any());
        }
    }
}
