-- V2 商品体系清理：删除 V1 遗留、代码零引用的冗余表/视图（为上线瘦身）
-- 依据：全后端检索（java/xml/yml）确认 product_sku / order_addon_coupons / v_shop_orders_* 无任何引用
--   - product_sku：V1 的 SKU 表；V2 设计明确「不建 SKU」（见商品体系封版结论）
--   - order_addon_coupons：V1 加料券-订单关联表，从未落地使用（0 行）
--   - v_shop_orders_full / v_shop_orders_hot：V1 订单查询视图，已被 ShopOrder 实体查询取代
-- 已确认：无任何表外键引用这些表，DROP 安全

DROP VIEW IF EXISTS `v_shop_orders_full`;
DROP VIEW IF EXISTS `v_shop_orders_hot`;

DROP TABLE IF EXISTS `product_sku`;
DROP TABLE IF EXISTS `order_addon_coupons`;
