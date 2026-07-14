ALTER TABLE shop_orders
    ADD COLUMN idempotency_key VARCHAR(64) NULL COMMENT 'Client supplied idempotency key' AFTER user_id;

CREATE UNIQUE INDEX uk_shop_orders_user_idempotency
    ON shop_orders(user_id, idempotency_key);
