package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateStockRequestDTO(

        @Pattern(regexp = ".*\\S.*", message = "Nome do produto não pode ficar em branco")
        String productName,

        @Pattern(regexp = ".*\\S.*", message = "Descrição não pode ficar em branco")
        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "Preço deve ser maior que zero")
        BigDecimal unitPrice,

        @Pattern(regexp = ".*\\S.*", message = "Categoria não pode ficar em branco")
        String category,

        @Min(value = 0, message = "Limite mínimo não pode ser negativo")
        Integer minThreshold
) {}
