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
    private final UUID serviceOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StockMovement(UUID stockId, MovementType movementType, Integer quantity, Integer previousQuantity,
                          Integer newQuantity, UUID serviceOrderId) {
        this.stockId = stockId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.serviceOrderId = serviceOrderId;
    }

    public static StockMovement create(UUID stockId, MovementType movementType, Integer quantity,
                                       Integer previousQuantity, Integer newQuantity) {
        return create(stockId, movementType, quantity, previousQuantity, newQuantity, null);
    }

    public static StockMovement create(UUID stockId, MovementType movementType, Integer quantity,
                                       Integer previousQuantity, Integer newQuantity, UUID serviceOrderId) {
        StockMovement movement = new StockMovement(stockId, movementType, quantity, previousQuantity, newQuantity,
                serviceOrderId);
        movement.createdAt = LocalDateTime.now();
        movement.updatedAt = LocalDateTime.now();
        return movement;
    }

    public static StockMovement reconstruct(UUID id, UUID stockId, MovementType movementType, Integer quantity,
                                            Integer previousQuantity, Integer newQuantity, UUID serviceOrderId,
                                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        StockMovement movement = new StockMovement(stockId, movementType, quantity, previousQuantity, newQuantity,
                serviceOrderId);
        movement.id = id;
        movement.createdAt = createdAt;
        movement.updatedAt = updatedAt;
        return movement;
    }
}
