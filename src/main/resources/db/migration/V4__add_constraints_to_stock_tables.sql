ALTER TABLE stocks
    ALTER COLUMN description SET NOT NULL,
    ALTER COLUMN category SET NOT NULL,
    ADD CONSTRAINT uq_stocks_product_name UNIQUE (product_name);

ALTER TABLE stock_movements
    ALTER COLUMN updated_at SET NOT NULL;
