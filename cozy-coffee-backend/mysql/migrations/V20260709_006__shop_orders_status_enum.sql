-- DC7: shop_orders.status -> ENUM + fix DEFAULT bug
-- Current DEFAULT 'completed' is wrong (new orders should be pending)
USE cozy_order;

ALTER TABLE shop_orders
  MODIFY COLUMN status ENUM('pending','preparing','completed','cancelled')
    NOT NULL DEFAULT 'pending'
    COMMENT '订单状态: pending=待处理 preparing=制作中 completed=已完成 cancelled=已取消';
