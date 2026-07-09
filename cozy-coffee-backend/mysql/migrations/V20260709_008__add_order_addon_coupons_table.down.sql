-- Rollback DC1: drop order_addon_coupons table
USE cozy_order;
DROP TABLE IF EXISTS order_addon_coupons;
-- 如果需要恢复 JSON 列（仅当迁移完成后执行过 DROP COLUMN）：
-- ALTER TABLE shop_orders ADD COLUMN applied_addon_coupon_ids VARCHAR(255) COMMENT '附加券ID JSON 数组';
