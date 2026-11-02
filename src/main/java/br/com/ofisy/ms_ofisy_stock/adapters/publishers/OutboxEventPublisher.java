package br.com.ofisy.ms_ofisy_stock.adapters.publishers;

import br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging.OutboxEventEntity;
import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class OutboxEventPublisher {

    private final JpaOutboxEventRepository jpa;
    private final SnsOperations snsOperations;
    private final JsonMapper jsonMapper;
    private final String topicName;
    private final int batchSize;

    public OutboxEventPublisher(JpaOutboxEventRepository jpa,
                                SnsOperations snsOperations,
                                JsonMapper jsonMapper,
                                @Value("${ofisy.messaging.topics.stock}") String topicName,
                                @Value("${ofisy.messaging.outbox.batch-size}") int batchSize) {
        this.jpa = jpa;
        this.snsOperations = snsOperations;
        this.jsonMapper = jsonMapper;
        this.topicName = topicName;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ofisy.messaging.outbox.publish-interval}")
    @Transactional
    public void publishPending() {
        List<OutboxEventEntity> pending = jpa.findByPublishedAtIsNullOrderByOccurredAtAsc(Limit.of(batchSize));
        Set<UUID> failedGroups = new HashSet<>();

        for (OutboxEventEntity event : pending) {
            if (failedGroups.contains(event.getMessageGroupId())) {
                continue;
            }

            try {
                snsOperations.sendNotification(topicName, notification(event));
                event.markPublished(Instant.now());
            } catch (Exception e) {
                log.error("OutboxEventPublisher.publishPending >> failed to publish event {} of type {} for group {}",
                        event.getEventId(), event.getEventType(), event.getMessageGroupId(), e);
                failedGroups.add(event.getMessageGroupId());
            }
        }
    }

    private SnsNotification<EventEnvelope> notification(OutboxEventEntity event) {
        EventEnvelope envelope = new EventEnvelope(
                event.getEventId(),
                event.getEventType(),
                event.getServiceOrderId(),
                event.getOccurredAt(),
                jsonMapper.readTree(event.getPayload()));

        return SnsNotification.builder(envelope)
                .groupId(event.getMessageGroupId().toString())
                .deduplicationId(event.getEventId().toString())
                .build();
    }
}
