package br.com.ofisy.ms_ofisy_stock.adapters.gateways.messaging;

import br.com.ofisy.ms_ofisy_stock.domain.messaging.ProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedEventRepositoryImplTest {

    @Mock
    private JpaProcessedEventRepository jpa;

    private ProcessedEventRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProcessedEventRepositoryImpl(jpa);
    }

    @Test
    void shouldCheckIfEventWasProcessed() {
        var eventId = UUID.randomUUID();
        when(jpa.existsById(eventId)).thenReturn(true);

        assertThat(repository.existsById(eventId)).isTrue();
    }

    @Test
    void shouldSaveProcessedEvent() {
        var domain = ProcessedEvent.create(UUID.randomUUID(), "QuoteCreated");
        when(jpa.save(any(ProcessedEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = repository.save(domain);

        assertThat(result.getEventId()).isEqualTo(domain.getEventId());
        assertThat(result.getEventType()).isEqualTo(domain.getEventType());
        verify(jpa).save(any(ProcessedEventEntity.class));
    }
}
