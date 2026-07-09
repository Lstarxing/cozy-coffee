-- DH2: Drop redundant indexes on shop_orders (saves write overhead)
USE cozy_order;

-- idx_user_id is prefix of idx_user_created -- duplicate
ALTER TABLE shop_orders DROP INDEX idx_user_id;

-- idx_order_no duplicates UNIQUE KEY order_no -- the unique constraint already indexes it
ALTER TABLE shop_orders DROP INDEX idx_order_no;
