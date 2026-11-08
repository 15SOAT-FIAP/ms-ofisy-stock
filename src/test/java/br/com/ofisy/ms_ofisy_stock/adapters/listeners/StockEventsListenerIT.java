package br.com.ofisy.ms_ofisy_stock.adapters.listeners;

import br.com.ofisy.ms_ofisy_stock.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging.JpaProcessedEventRepository;
import br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging.OutboxEventEntity;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.ConsumeByServiceOrderStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consumebyserviceorder.ConsumeByServiceOrderStockUseCase.StockItem;
import br.com.ofisy.ms_ofisy_stock.domain.stock.Stock;
import br.com.ofisy.ms_ofisy_stock.domain.stock.StockRepository;
import br.com.ofisy.ms_ofisy_stock.shared.jwt.JwtTestTokens;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "jwt.secret=" + JwtTestTokens.SECRET)
class StockEventsListenerIT {

    private static final String EXECUTION_TOPIC_NAME = "ofisy-execution-events.fifo";
    private static final String BILLING_TOPIC_NAME = "ofisy-billing-events.fifo";

    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @Autowired
    private SnsClient snsClient;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ConsumeByServiceOrderStockUseCase consumeByServiceOrderStockUseCase;

    @Autowired
    private JpaProcessedEventRepository processedEventRepository;

    @Autowired
    private JpaOutboxEventRepository outboxEventRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${ofisy.messaging.queues.stock}")
    private String queueName;

    @Test
    void shouldAcknowledgeEventWithoutHandler() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        String body = """
                {
                  "eventId": "%s",
                  "type": "QuoteApproved",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-10-13T10:00:00Z",
                  "payload": { "quoteId": "%s" }
                }
                """.formatted(eventId, serviceOrderId, UUID.randomUUID());
        String queueUrl = queueUrl();

        send(queueUrl, serviceOrderId, body);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(pendingMessages(queueUrl)).isZero());
        assertThat(processedEventRepository.existsById(eventId)).isFalse();
    }

    @Nested
    class QuoteCreated {

        @Test
        void shouldConsumeItemsOnceAndPublishStockConsumedWhenEventIsDeliveredTwice() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();
            UUID stockId = newStock(10);
            String queueUrl = queueUrl();

            // Dedup ids diferentes simulam uma reentrega fora da janela de deduplicação do FIFO
            send(queueUrl, serviceOrderId, quoteCreatedBody(eventId, serviceOrderId, quoteId, stockId, 4));
            send(queueUrl, serviceOrderId, quoteCreatedBody(eventId, serviceOrderId, quoteId, stockId, 4));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            assertThat(quantityOf(stockId)).isEqualTo(6);

            List<OutboxEventEntity> events = outboxEventsOf(serviceOrderId);
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().getEventType()).isEqualTo("StockConsumed");
            JsonNode payload = jsonMapper.readTree(events.getFirst().getPayload());
            assertThat(payload.get("quoteId").asString()).isEqualTo(quoteId.toString());
            assertThat(payload.get("items")).hasSize(1);
            assertThat(payload.get("items").get(0).get("stockId").asString()).isEqualTo(stockId.toString());
            assertThat(payload.get("items").get(0).get("quantity").asInt()).isEqualTo(4);
        }

        @Test
        void shouldPublishStockConsumptionFailedAndKeepBalanceWhenItemIsMissing() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();
            UUID stockId = newStock(2);
            String queueUrl = queueUrl();

            send(queueUrl, serviceOrderId, quoteCreatedBody(eventId, serviceOrderId, quoteId, stockId, 3));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            assertThat(quantityOf(stockId)).isEqualTo(2);

            List<OutboxEventEntity> events = outboxEventsOf(serviceOrderId);
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().getEventType()).isEqualTo("StockConsumptionFailed");
            JsonNode payload = jsonMapper.readTree(events.getFirst().getPayload());
            assertThat(payload.get("quoteId").asString()).isEqualTo(quoteId.toString());
            JsonNode missing = payload.get("missingItems").get(0);
            assertThat(missing.get("stockId").asString()).isEqualTo(stockId.toString());
            assertThat(missing.get("requestedQuantity").asInt()).isEqualTo(3);
            assertThat(missing.get("availableQuantity").asInt()).isEqualTo(2);
        }

        @Test
        void shouldReceiveEventPublishedOnBillingTopic() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);

            snsClient.publish(r -> r
                    .topicArn(topicArn(BILLING_TOPIC_NAME))
                    .messageGroupId(serviceOrderId.toString())
                    .messageDeduplicationId(UUID.randomUUID().toString())
                    .message(quoteCreatedBody(eventId, serviceOrderId, UUID.randomUUID(), stockId, 3)));

            await().atMost(Duration.ofSeconds(30))
                    .untilAsserted(() -> assertThat(processedEventRepository.existsById(eventId)).isTrue());
            assertThat(quantityOf(stockId)).isEqualTo(7);
        }
    }

    @Nested
    class QuoteUpdated {

        @Test
        void shouldAdjustItemsOnceAndPublishStockConsumedWhenEventIsDeliveredTwice() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();
            UUID radiatorId = newStock(10);
            UUID filterId = newStock(5);
            consumeByServiceOrderStockUseCase.execute(new ConsumeByServiceOrderStockCommand(serviceOrderId,
                    List.of(new StockItem(radiatorId, 4), new StockItem(filterId, 2))));
            String queueUrl = queueUrl();

            // O radiador passa de 4 para 6 e o filtro sai do orçamento
            String items = """
                    [ { "stockId": "%s", "quantity": 6 } ]""".formatted(radiatorId);
            send(queueUrl, serviceOrderId, quoteUpdatedBody(eventId, serviceOrderId, quoteId, items));
            send(queueUrl, serviceOrderId, quoteUpdatedBody(eventId, serviceOrderId, quoteId, items));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            assertThat(quantityOf(radiatorId)).isEqualTo(4);
            assertThat(quantityOf(filterId)).isEqualTo(5);

            List<OutboxEventEntity> events = outboxEventsOf(serviceOrderId);
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().getEventType()).isEqualTo("StockConsumed");
            JsonNode payload = jsonMapper.readTree(events.getFirst().getPayload());
            assertThat(payload.get("quoteId").asString()).isEqualTo(quoteId.toString());
            assertThat(payload.get("items")).hasSize(1);
            assertThat(payload.get("items").get(0).get("stockId").asString()).isEqualTo(radiatorId.toString());
            assertThat(payload.get("items").get(0).get("quantity").asInt()).isEqualTo(6);
        }

        @Test
        void shouldPublishStockConsumptionFailedAndKeepBalanceWhenIncreaseIsMissing() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();
            UUID stockId = newStock(5);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 4))));
            String queueUrl = queueUrl();

            send(queueUrl, serviceOrderId, quoteUpdatedBody(eventId, serviceOrderId, quoteId, """
                    [ { "stockId": "%s", "quantity": 7 } ]""".formatted(stockId)));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            assertThat(quantityOf(stockId)).isEqualTo(1);

            List<OutboxEventEntity> events = outboxEventsOf(serviceOrderId);
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().getEventType()).isEqualTo("StockConsumptionFailed");
            JsonNode missing = jsonMapper.readTree(events.getFirst().getPayload()).get("missingItems").get(0);
            assertThat(missing.get("stockId").asString()).isEqualTo(stockId.toString());
            assertThat(missing.get("requestedQuantity").asInt()).isEqualTo(3);
            assertThat(missing.get("availableQuantity").asInt()).isEqualTo(1);
        }

        @Test
        void shouldIgnoreUpdateThatArrivesAfterServiceOrderWasRefunded() {
            UUID cancelledEventId = UUID.randomUUID();
            UUID updatedEventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 4))));
            String queueUrl = queueUrl();

            send(queueUrl, serviceOrderId, allExecutionsCancelledBody(cancelledEventId, serviceOrderId));
            send(queueUrl, serviceOrderId, quoteUpdatedBody(updatedEventId, serviceOrderId, UUID.randomUUID(), """
                    [ { "stockId": "%s", "quantity": 6 } ]""".formatted(stockId)));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(cancelledEventId)).isTrue();
                assertThat(processedEventRepository.existsById(updatedEventId)).isTrue();
            });
            assertThat(quantityOf(stockId)).isEqualTo(10);
            assertThat(outboxEventsOf(serviceOrderId)).extracting(OutboxEventEntity::getEventType)
                    .containsExactly("StockRefunded");
        }

        @Test
        void shouldReceiveEventPublishedOnBillingTopic() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 3))));

            snsClient.publish(r -> r
                    .topicArn(topicArn(BILLING_TOPIC_NAME))
                    .messageGroupId(serviceOrderId.toString())
                    .messageDeduplicationId(UUID.randomUUID().toString())
                    .message(quoteUpdatedBody(eventId, serviceOrderId, UUID.randomUUID(), "[]")));

            await().atMost(Duration.ofSeconds(30))
                    .untilAsserted(() -> assertThat(processedEventRepository.existsById(eventId)).isTrue());
            assertThat(quantityOf(stockId)).isEqualTo(10);
        }
    }

    @Nested
    class QuoteReproved {

        @Test
        void shouldRefundConsumedItemsAndPublishStockRefunded() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 4))));
            String queueUrl = queueUrl();

            send(queueUrl, serviceOrderId, quoteReprovedBody(eventId, serviceOrderId));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            assertThat(quantityOf(stockId)).isEqualTo(10);

            List<OutboxEventEntity> refunds = outboxEventsOf(serviceOrderId);
            assertThat(refunds).hasSize(1);
            assertThat(refunds.getFirst().getEventType()).isEqualTo("StockRefunded");
            JsonNode items = jsonMapper.readTree(refunds.getFirst().getPayload()).get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).get("stockId").asString()).isEqualTo(stockId.toString());
            assertThat(items.get(0).get("quantity").asInt()).isEqualTo(4);
        }

        @Test
        void shouldNotRefundAgainWhenServiceOrderIsCancelledAfterReproval() {
            UUID reprovedEventId = UUID.randomUUID();
            UUID cancelledEventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 4))));
            String queueUrl = queueUrl();

            send(queueUrl, serviceOrderId, quoteReprovedBody(reprovedEventId, serviceOrderId));
            send(queueUrl, serviceOrderId, allExecutionsCancelledBody(cancelledEventId, serviceOrderId));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(reprovedEventId)).isTrue();
                assertThat(processedEventRepository.existsById(cancelledEventId)).isTrue();
            });
            assertThat(quantityOf(stockId)).isEqualTo(10);

            // O segundo StockRefunded sai vazio: os itens já tinham voltado na reprovação
            List<Integer> refundedItems = outboxEventsOf(serviceOrderId).stream()
                    .filter(event -> "StockRefunded".equals(event.getEventType()))
                    .map(event -> jsonMapper.readTree(event.getPayload()).get("items").size())
                    .toList();
            assertThat(refundedItems).containsExactlyInAnyOrder(1, 0);
        }

        @Test
        void shouldReceiveEventPublishedOnBillingTopic() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 3))));

            snsClient.publish(r -> r
                    .topicArn(topicArn(BILLING_TOPIC_NAME))
                    .messageGroupId(serviceOrderId.toString())
                    .messageDeduplicationId(UUID.randomUUID().toString())
                    .message(quoteReprovedBody(eventId, serviceOrderId)));

            await().atMost(Duration.ofSeconds(30))
                    .untilAsserted(() -> assertThat(processedEventRepository.existsById(eventId)).isTrue());
            assertThat(quantityOf(stockId)).isEqualTo(10);
        }
    }

    @Nested
    class AllExecutionsCancelled {

        @Test
        void shouldRefundConsumedItemsOnceWhenEventIsDeliveredTwice() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 4))));
            String queueUrl = queueUrl();

            // Dedup ids diferentes simulam uma reentrega fora da janela de deduplicação do FIFO
            send(queueUrl, serviceOrderId, allExecutionsCancelledBody(eventId, serviceOrderId));
            send(queueUrl, serviceOrderId, allExecutionsCancelledBody(eventId, serviceOrderId));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            assertThat(quantityOf(stockId)).isEqualTo(10);

            List<OutboxEventEntity> refunds = outboxEventsOf(serviceOrderId);
            assertThat(refunds).hasSize(1);
            assertThat(refunds.getFirst().getEventType()).isEqualTo("StockRefunded");
            JsonNode items = jsonMapper.readTree(refunds.getFirst().getPayload()).get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).get("stockId").asString()).isEqualTo(stockId.toString());
            assertThat(items.get(0).get("quantity").asInt()).isEqualTo(4);
        }

        @Test
        void shouldPublishStockRefundedWithEmptyItemsWhenServiceOrderHasNothingToRefund() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            String queueUrl = queueUrl();

            send(queueUrl, serviceOrderId, allExecutionsCancelledBody(eventId, serviceOrderId));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            List<OutboxEventEntity> refunds = outboxEventsOf(serviceOrderId);
            assertThat(refunds).hasSize(1);
            assertThat(refunds.getFirst().getEventType()).isEqualTo("StockRefunded");
            assertThat(jsonMapper.readTree(refunds.getFirst().getPayload()).get("items")).isEmpty();
        }

        @Test
        void shouldReceiveEventPublishedOnExecutionTopic() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 3))));
            snsClient.publish(r -> r
                    .topicArn(topicArn(EXECUTION_TOPIC_NAME))
                    .messageGroupId(serviceOrderId.toString())
                    .messageDeduplicationId(UUID.randomUUID().toString())
                    .message(allExecutionsCancelledBody(eventId, serviceOrderId)));

            await().atMost(Duration.ofSeconds(30))
                    .untilAsserted(() -> assertThat(processedEventRepository.existsById(eventId)).isTrue());
            assertThat(quantityOf(stockId)).isEqualTo(10);
        }
    }

    private UUID newStock(int quantity) {
        return stockRepository.save(Stock.create("Produto " + UUID.randomUUID(), "Produto de teste", quantity,
                new BigDecimal("50.00"), "Testes", 1)).getId();
    }

    private int quantityOf(UUID stockId) {
        return stockRepository.findById(stockId).orElseThrow().getQuantity();
    }

    private List<OutboxEventEntity> outboxEventsOf(UUID serviceOrderId) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> serviceOrderId.equals(event.getServiceOrderId()))
                .toList();
    }

    private String topicArn(String topicName) {
        return snsClient.listTopics().topics().stream()
                .filter(topic -> topic.topicArn().endsWith(":" + topicName))
                .findFirst()
                .orElseThrow()
                .topicArn();
    }

    private String quoteCreatedBody(UUID eventId, UUID serviceOrderId, UUID quoteId, UUID stockId, int quantity) {
        return """
                {
                  "eventId": "%s",
                  "type": "QuoteCreated",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-11-02T10:00:00Z",
                  "payload": {
                    "quoteId": "%s",
                    "stockItems": [ { "stockId": "%s", "quantity": %d } ]
                  }
                }
                """.formatted(eventId, serviceOrderId, quoteId, stockId, quantity);
    }

    private String quoteUpdatedBody(UUID eventId, UUID serviceOrderId, UUID quoteId, String stockItems) {
        return """
                {
                  "eventId": "%s",
                  "type": "QuoteUpdated",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-11-02T10:30:00Z",
                  "payload": {
                    "quoteId": "%s",
                    "stockItems": %s
                  }
                }
                """.formatted(eventId, serviceOrderId, quoteId, stockItems);
    }

    private String quoteReprovedBody(UUID eventId, UUID serviceOrderId) {
        return """
                {
                  "eventId": "%s",
                  "type": "QuoteReproved",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-11-02T11:00:00Z",
                  "payload": {
                    "quoteId": "%s",
                    "reason": "Valor acima do esperado",
                    "stockItems": []
                  }
                }
                """.formatted(eventId, serviceOrderId, UUID.randomUUID());
    }

    private String allExecutionsCancelledBody(UUID eventId, UUID serviceOrderId) {
        return """
                {
                  "eventId": "%s",
                  "type": "AllExecutionsCancelled",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-10-20T10:00:00Z",
                  "payload": {}
                }
                """.formatted(eventId, serviceOrderId);
    }

    private String queueUrl() {
        return sqsAsyncClient.getQueueUrl(r -> r.queueName(queueName)).join().queueUrl();
    }

    private void send(String queueUrl, UUID serviceOrderId, String body) {
        sqsAsyncClient.sendMessage(r -> r
                .queueUrl(queueUrl)
                .messageGroupId(serviceOrderId.toString())
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(body)).join();
    }

    private int pendingMessages(String queueUrl) {
        Map<QueueAttributeName, String> attributes = sqsAsyncClient.getQueueAttributes(r -> r
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE))
                .join()
                .attributes();
        return attributes.values().stream().mapToInt(Integer::parseInt).sum();
    }
}
