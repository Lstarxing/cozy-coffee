USE cozy_order;
ALTER TABLE shop_orders
  MODIFY COLUMN status VARCHAR(20)
    DEFAULT 'completed'
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    COMMENT '订单状态:pending/completed/cancelled';
