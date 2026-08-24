-- 订单状态 enum 增加 delivering（外送订单出餐 → 配送中）
-- 履约改造时状态机 OrderStateMachine 增加 DELIVERING，但 DB enum 未同步，外送出餐写入 delivering 报 Data truncated
ALTER TABLE `shop_orders`
  MODIFY COLUMN `status` enum('pending','preparing','delivering','completed','cancelled')
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending'
    COMMENT '订单状态: pending=待处理 preparing=制作中 delivering=配送中 completed=已完成 cancelled=已取消';
