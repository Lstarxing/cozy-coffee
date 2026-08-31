-- V2 商品体系 · brew_method 对齐（设计文档注 8 / 选规格总表第 63·107 行）
-- 精品 Bean 商品 = 每支 Bean 一个商品，用户必选出品方式 POUR_OVER(手冲,用 price) / COLD_BREW(冷萃,用 cold_brew_price)
-- 今日冷萃不再独立商品（下线），冷萃 = Bean 商品的 COLD_BREW 出品

-- 1) coffee_products：brew_method（非空=可冲泡精品 Bean）+ cold_brew_price（冷萃价）
ALTER TABLE `coffee_products`
  ADD COLUMN `brew_method` VARCHAR(20) NULL COMMENT '出品方式（精品 Bean 必选规格）：POUR_OVER/COLD_BREW；NULL=非 Bean 商品' AFTER `tags`,
  ADD COLUMN `cold_brew_price` DECIMAL(10,2) NULL COMMENT '冷萃出品价（COLD_BREW 用；手冲用 price）' AFTER `brew_method`;

-- 2) shop_order_items：订单项存出品方式
ALTER TABLE `shop_order_items`
  ADD COLUMN `brew_method` VARCHAR(20) NULL COMMENT '出品方式：POUR_OVER/COLD_BREW（精品 Bean）' AFTER `temperature`;

-- 3) 今日冷萃下线（不再独立商品）
UPDATE `coffee_products` SET `status`='inactive' WHERE `name`='今日冷萃' AND `status`='active';

-- 4) 8 支手冲 Bean 设 brew_method + cold_brew_price（统一 38）
UPDATE `coffee_products` SET `brew_method`='POUR_OVER', `cold_brew_price`=38.00
WHERE `name` IN ('埃塞俄比亚·耶加雪菲','肯尼亚·涅里','巴西·米纳斯','哥伦比亚·安第斯','危地马拉·安提瓜','巴拿马·瑰夏','印尼·曼特宁','云南·保山')
  AND `status`='active';

-- ══════════════════════════════════════════════════════════════
-- 数据验收（本迁移应用后手动跑，期望全部通过）
-- ══════════════════════════════════════════════════════════════
-- ① active 精品 Bean = 8（今日冷萃已下线，总数 active 31）
-- SELECT COUNT(*) FROM coffee_products WHERE status='active';  -- 期望 31
-- ② 8 支手冲全部有 brew_method='POUR_OVER' + cold_brew_price=38
-- SELECT COUNT(*) FROM coffee_products WHERE status='active' AND brew_method IS NOT NULL;  -- 期望 8
-- ③ 非 Bean 商品 brew_method/cold_brew_price 为 NULL
-- SELECT COUNT(*) FROM coffee_products WHERE status='active' AND category<>'SPECIALTY' AND brew_method IS NOT NULL;  -- 期望 0
