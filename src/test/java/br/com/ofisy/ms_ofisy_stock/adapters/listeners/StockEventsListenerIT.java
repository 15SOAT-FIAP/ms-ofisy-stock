package br.com.ofisy.ms_ofisy_stock.adapters.listeners;

import br.com.ofisy.ms_ofisy_stock.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_stock.shared.jwt.JwtTestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "jwt.secret=" + JwtTestTokens.SECRET)
class StockEventsListenerIT {

    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @Value("${ofisy.messaging.queues.stock}")
    private String queueName;

    @Test
    void shouldAcknowledgeEventWithoutHandler() {
        UUID serviceOrderId = UUID.randomUUID();
        String body = """
                {
                  "eventId": "%s",
                  "type": "QuoteApproved",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-10-13T10:00:00Z",
                  "payload": { "quoteId": "%s" }
                }
                """.formatted(UUID.randomUUID(), serviceOrderId, UUID.randomUUID());
        String queueUrl = sqsAsyncClient.getQueueUrl(r -> r.queueName(queueName)).join().queueUrl();

        sqsAsyncClient.sendMessage(r -> r
                .queueUrl(queueUrl)
                .messageGroupId(serviceOrderId.toString())
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(body)).join();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(pendingMessages(queueUrl)).isZero());
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
