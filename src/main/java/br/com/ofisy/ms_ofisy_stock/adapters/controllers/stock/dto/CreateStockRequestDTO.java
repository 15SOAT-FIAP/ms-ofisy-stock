package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateStockRequestDTO(

        @NotBlank(message = "Nome do produto é obrigatório")
        String productName,

        @NotBlank(message = "Descrição é obrigatória")
        String description,

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 0, message = "Quantidade não pode ser negativa")
        Integer quantity,

        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "Preço deve ser maior que zero")
        BigDecimal unitPrice,

        @NotBlank(message = "Categoria é obrigatória")
        String category,

        @NotNull(message = "Limite mínimo é obrigatório")
        @Min(value = 0, message = "Limite mínimo não pode ser negativo")
        Integer minThreshold
) {}
