package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.adapters.tracing.TraceContextPropagator;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpa;
    private final JsonMapper jsonMapper;
    private final TraceContextPropagator traceContextPropagator;

    @Override
    public void save(OutboxEvent outboxEvent) {
        String payloadJson = jsonMapper.writeValueAsString(outboxEvent.getPayload());
        jpa.save(OutboxEventMapper.toEntity(outboxEvent, payloadJson, currentTraceContextJson()));
    }

    @Override
    public boolean existsByServiceOrderIdAndEventType(UUID serviceOrderId, String eventType) {
        return jpa.existsByServiceOrderIdAndEventType(serviceOrderId, eventType);
    }

    // Roda dentro da requisição (ou do consumo da mensagem), então captura o trace que originou o evento
    private String currentTraceContextJson() {
        Map<String, String> traceContext = traceContextPropagator.captureCurrent();
        return traceContext.isEmpty() ? null : jsonMapper.writeValueAsString(traceContext);
    }
}
