ALTER TABLE stock_movements
    ADD COLUMN service_order_id UUID;

CREATE INDEX idx_stock_movements_service_order_id ON stock_movements (service_order_id);
