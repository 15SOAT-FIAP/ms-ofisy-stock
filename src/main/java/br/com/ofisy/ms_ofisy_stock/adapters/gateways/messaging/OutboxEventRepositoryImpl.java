package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpa;
    private final JsonMapper jsonMapper;

    @Override
    public void save(OutboxEvent outboxEvent) {
        String payloadJson = jsonMapper.writeValueAsString(outboxEvent.getPayload());
        jpa.save(OutboxEventMapper.toEntity(outboxEvent, payloadJson));
    }
}
