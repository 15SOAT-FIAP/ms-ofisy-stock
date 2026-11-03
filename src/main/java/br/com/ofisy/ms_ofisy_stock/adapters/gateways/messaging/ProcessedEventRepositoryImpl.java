package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEvent;
import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpa;

    @Override
    public boolean existsById(UUID eventId) {
        return jpa.existsById(eventId);
    }

    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        ProcessedEventEntity entity = ProcessedEventMapper.toEntity(processedEvent);
        ProcessedEventEntity savedEntity = jpa.save(entity);
        return ProcessedEventMapper.toDomain(savedEntity);
    }
}
