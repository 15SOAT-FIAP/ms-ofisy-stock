package br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder;

import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockResult;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.MissingStockItem;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.domain.exception.InvalidStockQuantityException;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumeByServiceOrderStockServiceTest {

    private static final UUID SERVICE_ORDER_ID = UUID.randomUUID();
    private static final UUID RADIATOR_ID = UUID.randomUUID();
    private static final UUID FILTER_ID = UUID.randomUUID();

    @Mock
    private StockRepository repository;

    @Mock
    private ConsumeStockUseCase consumeStockUseCase;

    @InjectMocks
    private ConsumeByServiceOrderStockService consumeByServiceOrderService;

    private ConsumeByServiceOrderStockCommand command(StockItem... items) {
        return new ConsumeByServiceOrderStockCommand(SERVICE_ORDER_ID, List.of(items));
    }

    @Nested
    class Execute {

        @Test
        void shouldConsumeEveryItemLinkedToServiceOrderWhenAllHaveBalance() {
            when(repository.lockById(RADIATOR_ID)).thenReturn(Optional.of(persistedStock(RADIATOR_ID, "Radiador", 10)));
            when(repository.lockById(FILTER_ID)).thenReturn(Optional.of(persistedStock(FILTER_ID, "Filtro de óleo", 3)));
            Stock radiator = persistedStock(RADIATOR_ID, "Radiador", 8);
            Stock filter = persistedStock(FILTER_ID, "Filtro de óleo", 0);
            when(consumeStockUseCase.execute(new ConsumeStockCommand(RADIATOR_ID, 2, SERVICE_ORDER_ID))).thenReturn(radiator);
            when(consumeStockUseCase.execute(new ConsumeStockCommand(FILTER_ID, 3, SERVICE_ORDER_ID))).thenReturn(filter);

            ConsumeByServiceOrderStockResult result = consumeByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 2), new StockItem(FILTER_ID, 3)));

            assertThat(result.consumed()).isTrue();
            assertThat(result.consumedStocks()).containsExactlyInAnyOrder(radiator, filter);
            assertThat(result.missingItems()).isEmpty();
        }

        @Test
        void shouldConsumeNothingWhenAnyItemHasInsufficientBalance() {
            when(repository.lockById(RADIATOR_ID)).thenReturn(Optional.of(persistedStock(RADIATOR_ID, "Radiador", 10)));
            when(repository.lockById(FILTER_ID)).thenReturn(Optional.of(persistedStock(FILTER_ID, "Filtro de óleo", 1)));

            ConsumeByServiceOrderStockResult result = consumeByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 2), new StockItem(FILTER_ID, 3)));

            assertThat(result.consumed()).isFalse();
            assertThat(result.consumedStocks()).isEmpty();
            assertThat(result.missingItems()).containsExactly(new MissingStockItem(FILTER_ID, 3, 1));
            verify(consumeStockUseCase, never()).execute(any());
        }

        @Test
        void shouldReportMissingItemWithZeroAvailableWhenStockDoesNotExist() {
            when(repository.lockById(RADIATOR_ID)).thenReturn(Optional.empty());

            ConsumeByServiceOrderStockResult result = consumeByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 2)));

            assertThat(result.missingItems()).containsExactly(new MissingStockItem(RADIATOR_ID, 2, 0));
            verify(consumeStockUseCase, never()).execute(any());
        }

        @Test
        void shouldSumQuantitiesWhenSameStockAppearsMoreThanOnce() {
            when(repository.lockById(RADIATOR_ID)).thenReturn(Optional.of(persistedStock(RADIATOR_ID, "Radiador", 4)));

            ConsumeByServiceOrderStockResult result = consumeByServiceOrderService.execute(
                    command(new StockItem(RADIATOR_ID, 2), new StockItem(RADIATOR_ID, 3)));

            assertThat(result.missingItems()).containsExactly(new MissingStockItem(RADIATOR_ID, 5, 4));
        }

        @Test
        void shouldConsumeSummedQuantityOnceWhenSameStockAppearsMoreThanOnce() {
            when(repository.lockById(RADIATOR_ID)).thenReturn(Optional.of(persistedStock(RADIATOR_ID, "Radiador", 10)));

            consumeByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, 2), new StockItem(RADIATOR_ID, 3)));

            verify(consumeStockUseCase).execute(new ConsumeStockCommand(RADIATOR_ID, 5, SERVICE_ORDER_ID));
        }

        @Test
        void shouldReturnConsumedWithoutTouchingStockWhenThereAreNoItems() {
            ConsumeByServiceOrderStockResult result = consumeByServiceOrderService.execute(command());

            assertThat(result.consumed()).isTrue();
            assertThat(result.consumedStocks()).isEmpty();
            verify(repository, never()).lockById(any());
            verify(consumeStockUseCase, never()).execute(any());
        }

        @Test
        void shouldThrowInvalidStockQuantityExceptionWhenQuantityIsNotPositive() {
            assertThatThrownBy(() -> consumeByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, 0))))
                    .isInstanceOf(InvalidStockQuantityException.class);

            verify(consumeStockUseCase, never()).execute(any());
        }

        @Test
        void shouldThrowInvalidStockQuantityExceptionWhenQuantityIsNull() {
            assertThatThrownBy(() -> consumeByServiceOrderService.execute(command(new StockItem(RADIATOR_ID, null))))
                    .isInstanceOf(InvalidStockQuantityException.class);

            verify(consumeStockUseCase, never()).execute(any());
        }
    }
}
