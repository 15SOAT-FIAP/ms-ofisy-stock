package br.com.ofisy.ms_ofisy_stock.domain.messaging;

import java.util.UUID;

public interface ProcessedEventRepository {

    boolean existsById(UUID eventId);

    ProcessedEvent save(ProcessedEvent processedEvent);
}
