DROP INDEX uk_shop_orders_user_idempotency ON shop_orders;
ALTER TABLE shop_orders DROP COLUMN idempotency_key;
