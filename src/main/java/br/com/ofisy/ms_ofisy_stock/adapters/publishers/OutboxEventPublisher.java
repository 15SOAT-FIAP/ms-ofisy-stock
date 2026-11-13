package br.com.ofisy.ms_ofisy_stock.adapters.publishers;

import br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging.OutboxEventEntity;
import br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_stock.adapters.tracing.TraceContextPropagator;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class OutboxEventPublisher {

    private static final String PUBLISH_SPAN_NAME = "outbox.publish";
    private static final TypeReference<Map<String, String>> TRACE_CONTEXT_TYPE = new TypeReference<>() {};

    private final JpaOutboxEventRepository jpa;
    private final SnsOperations snsOperations;
    private final JsonMapper jsonMapper;
    private final TraceContextPropagator traceContextPropagator;
    private final String topicName;
    private final int batchSize;

    public OutboxEventPublisher(JpaOutboxEventRepository jpa,
                                SnsOperations snsOperations,
                                JsonMapper jsonMapper,
                                TraceContextPropagator traceContextPropagator,
                                @Value("${ofisy.messaging.topics.stock}") String topicName,
                                @Value("${ofisy.messaging.outbox.batch-size}") int batchSize) {
        this.jpa = jpa;
        this.snsOperations = snsOperations;
        this.jsonMapper = jsonMapper;
        this.traceContextPropagator = traceContextPropagator;
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
                // O job roda em um trace próprio, então a publicação é feita como filha do trace que gerou o evento
                traceContextPropagator.runWithParent(traceContext(event), PUBLISH_SPAN_NAME, spanAttributes(event),
                        () -> snsOperations.sendNotification(topicName, notification(event)));
                event.markPublished(Instant.now());
            } catch (Exception e) {
                log.error("OutboxEventPublisher.publishPending >> failed to publish event {} of type {} for group {}",
                        event.getEventId(), event.getEventType(), event.getMessageGroupId(), e);
                failedGroups.add(event.getMessageGroupId());
            }
        }
    }

    // Um contexto ilegível não pode travar o outbox: o evento é publicado mesmo assim, só sem a ligação com o trace de origem
    private Map<String, String> traceContext(OutboxEventEntity event) {
        if (event.getTraceContext() == null) {
            return Map.of();
        }

        try {
            return jsonMapper.readValue(event.getTraceContext(), TRACE_CONTEXT_TYPE);
        } catch (JacksonException e) {
            log.warn("OutboxEventPublisher.traceContext >> ignoring unreadable trace context of event {}", event.getEventId(), e);
            return Map.of();
        }
    }

    // O StockLow não pertence a uma OS, então o id da OS só entra no span quando existe
    private Map<String, String> spanAttributes(OutboxEventEntity event) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("messaging.destination.name", topicName);
        attributes.put("ofisy.event.id", event.getEventId().toString());
        attributes.put("ofisy.event.type", event.getEventType());
        attributes.put("ofisy.message_group.id", event.getMessageGroupId().toString());
        if (event.getServiceOrderId() != null) {
            attributes.put("ofisy.service_order.id", event.getServiceOrderId().toString());
        }
        return attributes;
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
