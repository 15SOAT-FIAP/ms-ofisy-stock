package br.com.ofisy.ms_ofisy_stock.domain.messaging;

public interface OutboxEventRepository {

    void save(OutboxEvent outboxEvent);
}
