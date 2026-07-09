-- DC3: Drop foreign keys (internet project anti-pattern)
-- Verified FKs via: SELECT * FROM information_schema.KEY_COLUMN_USAGE WHERE REFERENCED_TABLE_NAME IS NOT NULL

-- 1. cozy_order.shop_order_items -> shop_orders (ON DELETE CASCADE, dangerous)
USE cozy_order;
ALTER TABLE shop_order_items
  DROP FOREIGN KEY fk_shop_order_items_order_id;

-- 2. cozy_member.points_lot_consumptions -> points_lots (ON DELETE RESTRICT)
USE cozy_member;
ALTER TABLE points_lot_consumptions
  DROP FOREIGN KEY fk_points_lot_consumptions_lot_id;
