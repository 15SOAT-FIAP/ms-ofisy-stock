package br.com.ofisy.ms_ofisy_stock.domain.stockmovement;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class StockMovement {

    private UUID id;
    private final UUID stockId;
    private final MovementType movementType;
    private final Integer quantity;
    private final Integer previousQuantity;
    private final Integer newQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StockMovement(UUID stockId, MovementType movementType, Integer quantity, Integer previousQuantity,
                          Integer newQuantity) {
        this.stockId = stockId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
    }

    public static StockMovement create(UUID stockId, MovementType movementType, Integer quantity,
                                       Integer previousQuantity, Integer newQuantity) {
        StockMovement movement = new StockMovement(stockId, movementType, quantity, previousQuantity, newQuantity);
        movement.createdAt = LocalDateTime.now();
        movement.updatedAt = LocalDateTime.now();
        return movement;
    }

    public static StockMovement reconstruct(UUID id, UUID stockId, MovementType movementType, Integer quantity,
                                            Integer previousQuantity, Integer newQuantity,
                                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        StockMovement movement = new StockMovement(stockId, movementType, quantity, previousQuantity, newQuantity);
        movement.id = id;
        movement.createdAt = createdAt;
        movement.updatedAt = updatedAt;
        return movement;
    }
}
