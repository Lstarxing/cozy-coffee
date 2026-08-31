-- V2 价格调整（评审收敛）：抹茶/可可补大杯价、Bean 冷萃 38→42、一豆两喝 58→68、一豆三喝 68→88
-- 保持：手冲 50、瑰夏 68、生椰/燕麦 32/35、Cozy Dirty 32
-- 冷萃 ¥42 为 Bean 商品的 COLD_BREW 出品价（cold_brew_price 列），不建独立「今日冷萃」商品

-- 抹茶拿铁 30/34（MEDIUM_LARGE 补大杯价）
UPDATE `coffee_products` SET `price_large`=34.00 WHERE `name`='抹茶拿铁' AND `status`='active';
-- 可可 26/30（MEDIUM_LARGE 补大杯价）
UPDATE `coffee_products` SET `price_large`=30.00 WHERE `name`='可可' AND `status`='active';

-- Bean 冷萃 38→42（8 支手冲的 COLD_BREW 出品价）
UPDATE `coffee_products` SET `cold_brew_price`=42.00
WHERE `name` IN ('埃塞俄比亚·耶加雪菲','肯尼亚·涅里','巴西·米纳斯','哥伦比亚·安第斯','危地马拉·安提瓜','巴拿马·瑰夏','印尼·曼特宁','云南·保山')
  AND `status`='active';

-- 一豆两喝 58→68（默认常规 Bean；瑰夏不开放）
UPDATE `coffee_products` SET `price`=68.00 WHERE `name`='一豆两喝' AND `status`='active';
-- 一豆三喝 68→88
UPDATE `coffee_products` SET `price`=88.00 WHERE `name`='一豆三喝' AND `status`='active';

-- ══════════════════════════════════════════════════════════════
-- 数据验收（本迁移应用后手动跑，期望全部通过）
-- ══════════════════════════════════════════════════════════════
-- ① 抹茶 30/34、可可 26/30
-- SELECT name, price_medium, price_large FROM coffee_products WHERE name IN ('抹茶拿铁','可可') AND status='active';
-- ② 8 支手冲 cold_brew_price=42
-- SELECT COUNT(*) FROM coffee_products WHERE status='active' AND cold_brew_price=42.00;  -- 期望 8
-- ③ 一豆两喝 68 / 一豆三喝 88
-- SELECT name, price FROM coffee_products WHERE name IN ('一豆两喝','一豆三喝') AND status='active';
-- ④ 生椰/燕麦仍 32/35、Dirty 32、手冲 50、瑰夏 68
-- SELECT name, price, price_medium, price_large FROM coffee_products
-- WHERE name IN ('生椰拿铁','燕麦拿铁','Cozy Dirty','埃塞俄比亚·耶加雪菲','巴拿马·瑰夏') AND status='active';
