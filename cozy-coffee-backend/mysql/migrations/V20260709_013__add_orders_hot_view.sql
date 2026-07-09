-- DH1: shop_orders 表过宽（30 字段冷热混放）-> 冷字段归档视图
-- 冷字段：business_date/pickup_code/pickup_code_generated_at/updated_at/
--         applied_coupon_id/applied_addon_coupon_ids/delivery_fee/delivery_fee_waived/
--         delivery_fee_waived_reason/remark/rewards_granted/points_multiplier
-- 热字段：id/order_no/user_id/status/total_amount/points_earned/exp_earned/
--         pay_amount/discount_amount/total_quantity/dining_method/created_at/store_id
--
-- 当前方案：创建热字段视图（常用查询扫热字段，避免加载冷字段），
-- 物理拆分需应用层配合（SELECT 只查热字段 + JOIN 冷表），暂不执行。
USE cozy_order;

-- 热字段视图（高频查询用：列表、状态流转）
CREATE OR REPLACE VIEW v_shop_orders_hot AS
SELECT
  id, order_no, user_id, status, total_amount, pay_amount,
  discount_amount, points_earned, exp_earned, total_quantity,
  dining_method, store_id, created_at
FROM shop_orders;

-- 全字段视图 = 原表（兼容现有代码）
-- v_shop_orders_full 等价于 SELECT * FROM shop_orders
CREATE OR REPLACE VIEW v_shop_orders_full AS SELECT * FROM shop_orders;

-- TODO Phase 5+ 物理拆分：
-- 1. 新建 shop_orders_cold 表（含 cold 字段 + order_id FK）
-- 2. 迁移冷字段数据
-- 3. ALTER TABLE shop_orders DROP COLUMN cold fields
-- 4. 应用层 SELECT 热字段用 v_shop_orders_hot，详情用 JOIN cold 表
