package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stockmovement;

import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stockmovement.dto.StockMovementResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "API de Movimentação de Estoque")
public interface StockMovementApi {

    @Operation(summary = "Listar todas as movimentações de estoque com paginação - Roles autorizadas: (ADMIN, STOCKMAN)")
    @ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<Page<StockMovementResponseDTO>> getAllStockMovements(Pageable pageable);

    @Operation(summary = "Listar as movimentações de um estoque com paginação - Roles autorizadas: (ADMIN, STOCKMAN)")
    @ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<Page<StockMovementResponseDTO>> getByStockId(UUID stockId, Pageable pageable);
}
