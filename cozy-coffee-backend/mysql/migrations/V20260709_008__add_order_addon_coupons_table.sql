-- DC1: applied_addon_coupon_ids VARCHAR(255) JSON -> order_addon_coupons 关联表
-- 当前 shop_orders.applied_addon_coupon_ids 存 JSON 数组如 "[1,2,3]"
-- 无法索引、无法 JOIN、数据完整性靠应用层合同
USE cozy_order;

CREATE TABLE order_addon_coupons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL COMMENT '订单ID',
  coupon_id BIGINT NOT NULL COMMENT '附加券ID (cozy_mall.user_coupons.id)',
  coupon_code VARCHAR(32) NOT NULL COMMENT '券码冗余，方便查询避免跨库JOIN',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_coupon (order_id, coupon_id) COMMENT '防重复绑定',
  INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单附加券关联表';

-- 数据迁移（从已有 JSON 字段拆到关联表，仅迁移非 NULL 且非空 JSON）
-- 因 VARCHAR 字段可能含无效 JSON，逐条迁移用应用层脚本更安全。
-- 此处提供 SQL 框架，实际迁移在应用层批量执行：
-- INSERT INTO order_addon_coupons (order_id, coupon_id, coupon_code)
-- SELECT o.id, coupon_id, '' FROM shop_orders o
-- CROSS JOIN JSON_TABLE(o.applied_addon_coupon_ids, '$[*]' COLUMNS(coupon_id BIGINT PATH '$')) jt
-- WHERE o.applied_addon_coupon_ids IS NOT NULL AND o.applied_addon_coupon_ids != '[]';

-- 迁移完成后删除旧列（TODO: 待迁移脚本验证后执行）
-- ALTER TABLE shop_orders DROP COLUMN applied_addon_coupon_ids;
