package br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase.AdjustByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.adjustbyserviceorder.AdjustByServiceOrderStockUseCase.AdjustByServiceOrderStockResult;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.release.ReleaseStockUseCase.ReleaseStockCommand;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.MovementType;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovement;
import br.com.ofisy.ms_ofisy_stock.domain.stockmovement.StockMovementRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.ofisy.ms_ofisy_stock.application.stock.StockFixture.persistedStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjustByServiceOrderStockServiceTest {

    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    // Ids fixos para a ordem das travas ser previsível
    private static final UUID RADIATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FILTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private StockRepository repository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ConsumeStockUseCase consumeStockUseCase;

    @Mock
    private ReleaseStockUseCase releaseStockUseCase;

    @InjectMocks
    private AdjustByServiceOrderStockService adjustByServiceOrderService;

    private AdjustByServiceOrderStockCommand command(StockItem... items) {
        return new AdjustByServiceOrderStockCommand(SERVICE_ORDER_ID, List.of(items));
    }

    private StockMovement movement(UUID stockId, MovementType type, int quantity) {
        return StockMovement.create(stockId, type, quantity, 10, 10, SERVICE_ORDER_ID);
    }

    private void withServiceOrder(StockMovement... movements) {
        when(stockMovementRepository.findByServiceOrderId(SERVICE_ORDER_ID)).thenReturn(List.of(movements));
    }

    private void available(UUID stockId, int quantity) {
        when(repository.lockById(stockId)).thenReturn(Optional.of(persistedStock(stockId, "Produto", quantity)));
    }

    @Nested
    class Execute {

        @Test
        void shouldConsumeOnlyTheIncreaseWhenQuantityGoesUp() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 2));
            available(RADIATOR_ID, 10);

            AdjustByServiceOrderStockResult result = adjustByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 5)));

            assertThat(result.adjusted()).isTrue();
            verify(consumeStockUseCase).execute(new ConsumeStockCommand(RADIATOR_ID, 3, SERVICE_ORDER_ID));
            verify(releaseStockUseCase, never()).execute(any());
        }

        @Test
        void shouldReleaseOnlyTheDecreaseWhenQuantityGoesDown() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 5));
            available(RADIATOR_ID, 10);

            adjustByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, 2)));

            verify(releaseStockUseCase).execute(new ReleaseStockCommand(RADIATOR_ID, 3, SERVICE_ORDER_ID));
            verify(consumeStockUseCase, never()).execute(any());
        }

        @Test
        void shouldConsumeNewItemAndReleaseRemovedItem() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 2));
            available(RADIATOR_ID, 10);
            available(FILTER_ID, 10);

            adjustByServiceOrderService.execute(command(new StockItem(FILTER_ID, 1)));

            verify(releaseStockUseCase).execute(new ReleaseStockCommand(RADIATOR_ID, 2, SERVICE_ORDER_ID));
            verify(consumeStockUseCase).execute(new ConsumeStockCommand(FILTER_ID, 1, SERVICE_ORDER_ID));
        }

        @Test
        void shouldReleaseEverythingWhenListIsEmpty() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 2), movement(FILTER_ID, MovementType.OUT, 1));
            available(RADIATOR_ID, 10);
            available(FILTER_ID, 10);

            AdjustByServiceOrderStockResult result = adjustByServiceOrderService.execute(command());

            assertThat(result.adjusted()).isTrue();
            verify(releaseStockUseCase).execute(new ReleaseStockCommand(RADIATOR_ID, 2, SERVICE_ORDER_ID));
            verify(releaseStockUseCase).execute(new ReleaseStockCommand(FILTER_ID, 1, SERVICE_ORDER_ID));
        }

        @Test
        void shouldCompareWithWhatIsStillWithServiceOrderAfterPreviousReleases() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 5), movement(RADIATOR_ID, MovementType.IN, 3));
            available(RADIATOR_ID, 10);

            adjustByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, 4)));

            verify(consumeStockUseCase).execute(new ConsumeStockCommand(RADIATOR_ID, 2, SERVICE_ORDER_ID));
        }

        @Test
        void shouldNotMoveAnythingWhenListMatchesWhatIsWithServiceOrder() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 2));

            AdjustByServiceOrderStockResult result = adjustByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 2)));

            assertThat(result.adjusted()).isTrue();
            verify(repository, never()).lockById(any());
            verify(consumeStockUseCase, never()).execute(any());
            verify(releaseStockUseCase, never()).execute(any());
        }

        @Test
        void shouldMoveNothingWhenAnyIncreaseHasInsufficientBalance() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 4), movement(FILTER_ID, MovementType.OUT, 1));
            available(RADIATOR_ID, 10);
            available(FILTER_ID, 1);

            AdjustByServiceOrderStockResult result = adjustByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 1), new StockItem(FILTER_ID, 4)));

            assertThat(result.adjusted()).isFalse();
            assertThat(result.missingItems()).containsExactly(new MissingStockItem(FILTER_ID, 3, 1));
            verify(consumeStockUseCase, never()).execute(any());
            verify(releaseStockUseCase, never()).execute(any());
        }

        @Test
        void shouldReportMissingItemWithZeroAvailableWhenStockDoesNotExist() {
            withServiceOrder();
            when(repository.lockById(RADIATOR_ID)).thenReturn(Optional.empty());

            AdjustByServiceOrderStockResult result = adjustByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 2)));

            assertThat(result.missingItems()).containsExactly(new MissingStockItem(RADIATOR_ID, 2, 0));
        }

        @Test
        void shouldSumQuantitiesWhenSameStockAppearsMoreThanOnce() {
            withServiceOrder(movement(RADIATOR_ID, MovementType.OUT, 1));
            available(RADIATOR_ID, 10);

            adjustByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, 2), new StockItem(RADIATOR_ID, 3)));

            verify(consumeStockUseCase).execute(new ConsumeStockCommand(RADIATOR_ID, 4, SERVICE_ORDER_ID));
        }

        @Test
        void shouldLockEveryChangedItemInIdOrderBeforeMovingAnything() {
            withServiceOrder(movement(FILTER_ID, MovementType.OUT, 1));
            available(RADIATOR_ID, 10);
            available(FILTER_ID, 10);

            adjustByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, 1)));

            InOrder order = inOrder(repository, consumeStockUseCase, releaseStockUseCase);
            order.verify(repository).lockById(RADIATOR_ID);
            order.verify(repository).lockById(FILTER_ID);
            order.verify(consumeStockUseCase).execute(new ConsumeStockCommand(RADIATOR_ID, 1, SERVICE_ORDER_ID));
            order.verify(releaseStockUseCase).execute(new ReleaseStockCommand(FILTER_ID, 1, SERVICE_ORDER_ID));
        }

        @Test
        void shouldThrowInvalidStockQuantityExceptionWhenQuantityIsNotPositive() {
            assertThatThrownBy(() -> adjustByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, 0))))
                    .isInstanceOf(InvalidStockQuantityException.class);

            verify(consumeStockUseCase, never()).execute(any());
            verify(releaseStockUseCase, never()).execute(any());
        }

        @Test
        void shouldThrowInvalidStockQuantityExceptionWhenQuantityIsNull() {
            assertThatThrownBy(() -> adjustByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, null))))
                    .isInstanceOf(InvalidStockQuantityException.class);

            verify(consumeStockUseCase, never()).execute(any());
        }
    }
}
