USE cozy_order;
ALTER TABLE shop_orders ADD INDEX idx_user_id (user_id) USING BTREE;
ALTER TABLE shop_orders ADD INDEX idx_order_no (order_no) USING BTREE;
