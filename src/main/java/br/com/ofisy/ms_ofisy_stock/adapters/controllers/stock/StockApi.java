package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock;

import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto.CreateStockRequestDTO;
import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto.StockResponseDTO;
import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto.UpdateStockRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "API de Estoque")
public interface StockApi {

    @Operation(summary = "Listar estoques com paginação - Roles autorizadas: (ADMIN, STOCKMAN, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Estoques listados com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<Page<StockResponseDTO>> getAllStocks(Pageable pageable);

    @Operation(summary = "Obter estoque por ID - Roles autorizadas: (ADMIN, STOCKMAN, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Estoque encontrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Estoque não encontrado")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<StockResponseDTO> getStockById(UUID id);

    @Operation(summary = "Obter estoque por nome do produto - Roles autorizadas: (ADMIN, STOCKMAN, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Estoque encontrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Estoque não encontrado")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<StockResponseDTO> getByProductName(String productName);

    @Operation(summary = "Cadastrar um novo estoque - Roles autorizadas: (ADMIN, STOCKMAN)")
    @ApiResponse(responseCode = "201", description = "Estoque cadastrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados para cadastro de estoque inválidos")
    @ApiResponse(responseCode = "409", description = "Já existe um estoque para o produto informado")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<StockResponseDTO> create(CreateStockRequestDTO request);

    @Operation(summary = "Dar entrada de quantidade no estoque - Roles autorizadas: (ADMIN, STOCKMAN)")
    @ApiResponse(responseCode = "200", description = "Entrada registrada com sucesso")
    @ApiResponse(responseCode = "400", description = "Quantidade inválida")
    @ApiResponse(responseCode = "404", description = "Estoque não encontrado")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<StockResponseDTO> addStock(UUID id, Integer quantity);

    @Operation(summary = "Consumir quantidade do estoque - Roles autorizadas: (ADMIN, STOCKMAN)")
    @ApiResponse(responseCode = "200", description = "Consumo registrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Quantidade inválida")
    @ApiResponse(responseCode = "404", description = "Estoque não encontrado")
    @ApiResponse(responseCode = "409", description = "Estoque insuficiente")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<StockResponseDTO> consumeStock(UUID id, Integer quantity);

    @Operation(summary = "Atualizar um estoque existente - Roles autorizadas: (ADMIN, STOCKMAN)")
    @ApiResponse(responseCode = "200", description = "Estoque atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados para atualização de estoque inválidos")
    @ApiResponse(responseCode = "404", description = "Estoque não encontrado")
    @ApiResponse(responseCode = "409", description = "Já existe um estoque para o produto informado")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<StockResponseDTO> update(UUID id, UpdateStockRequestDTO request);
}
