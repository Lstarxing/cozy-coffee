-- ============================================================
-- 清理历史遗留、不再使用的商品（管理端不应再展示）：
--   1) V1 遗留下架商品：image_url 仍指向旧 /images/seed/ 目录
--      （由 V1__init 等早期迁移种入；V2 商品体系上线后已用
--       /images/products/coffee 布局，这些属废弃数据）
--   2) 「今日冷萃」：产品方案已确定不作为独立商品
--      （曾以 V2 图占位并置 inactive，应删除）
-- 说明：仅删 inactive + 旧路径/旧方案，不影响在售 V2 商品；
--       订单历史依赖 order_items 内快照，不受影响。
-- ============================================================

DELETE FROM coffee_products
 WHERE status = 'inactive'
   AND image_url LIKE '/images/seed/%';

DELETE FROM coffee_products
 WHERE status = 'inactive'
   AND name = '今日冷萃';
