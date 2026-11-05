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
        void shouldNotPublishStockRefundedWhenServiceOrderHasNothingToRefund() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            String queueUrl = queueUrl();

            send(queueUrl, serviceOrderId, allExecutionsCancelledBody(eventId, serviceOrderId));

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(pendingMessages(queueUrl)).isZero();
                assertThat(processedEventRepository.existsById(eventId)).isTrue();
            });
            assertThat(outboxEventsOf(serviceOrderId)).isEmpty();
        }

        @Test
        void shouldReceiveEventPublishedOnExecutionTopic() {
            UUID eventId = UUID.randomUUID();
            UUID serviceOrderId = UUID.randomUUID();
            UUID stockId = newStock(10);
            consumeByServiceOrderStockUseCase.execute(
                    new ConsumeByServiceOrderStockCommand(serviceOrderId, List.of(new StockItem(stockId, 3))));
            String topicArn = snsClient.listTopics().topics().stream()
                    .filter(topic -> topic.topicArn().endsWith(":" + EXECUTION_TOPIC_NAME))
                    .findFirst()
                    .orElseThrow()
                    .topicArn();

            snsClient.publish(r -> r
                    .topicArn(topicArn)
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
