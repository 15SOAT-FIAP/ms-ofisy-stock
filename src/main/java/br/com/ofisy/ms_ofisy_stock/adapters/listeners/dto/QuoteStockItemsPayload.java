package br.com.ofisy.ms_ofisy_stock.adapters.listeners.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

// Payload dos eventos do orçamento que levam a lista de itens de estoque. Só os campos que o estoque usa. Os demais que o billing venha a enviar são ignorados
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuoteStockItemsPayload(
        UUID quoteId,
        List<StockItemPayload> stockItems
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StockItemPayload(
            UUID stockId,
            Integer quantity
    ) {
    }
}
