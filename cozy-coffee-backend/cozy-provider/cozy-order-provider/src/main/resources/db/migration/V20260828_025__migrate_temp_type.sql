-- V2 商品体系 · Phase 1D TempType 砍温数据迁移
-- ALL_OK → HOT_COLD（热/冰，去 WARM）；NO_HOT → COLD_ONLY（冰/温并入：不可热即冰）
-- 与 cozy-order-api TempType 枚举改版同批生效（重启时 Flyway 先 apply 数据，再加载新枚举）
-- 历史订单 shop_order_items 快照中的 warm 保留不动；新单禁止产生 warm（由枚举 allowedValues 拦截）

-- 迁移前断言：确认无未知 temp_type 值
-- SELECT temp_type, COUNT(*) FROM coffee_products GROUP BY temp_type;

UPDATE `coffee_products` SET `temp_type` = 'HOT_COLD' WHERE `temp_type` = 'ALL_OK';
UPDATE `coffee_products` SET `temp_type` = 'COLD_ONLY' WHERE `temp_type` = 'NO_HOT';
