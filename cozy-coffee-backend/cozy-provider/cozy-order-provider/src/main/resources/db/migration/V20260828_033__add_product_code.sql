-- V2 商品体系 · product_code 商品短码列（评审收敛：ID/code 是身份，name 是内容）
-- 短码源自设计文档 3.5 资产命名；后续迁移优先用 product_code 定位，不再依赖 name（展示内容会改）
-- 注意：UPDATE 必须带 status='active'，否则会命中同名已下线旧行导致 UNIQUE 冲突

ALTER TABLE `coffee_products`
  ADD COLUMN `product_code` VARCHAR(64) NULL COMMENT '商品短码（3.5 资产命名，身份标识；01-espresso 等）' AFTER `name`,
  ADD UNIQUE KEY `uk_product_code` (`product_code`);

UPDATE `coffee_products` SET `product_code`='01-espresso' WHERE `name`='Espresso' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='01-americano' WHERE `name`='Cozy 美式' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='02-flat-white' WHERE `name`='澳白' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='02-cappuccino' WHERE `name`='卡布奇诺' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='02-caffe-latte' WHERE `name`='经典拿铁' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='02-coconut-latte' WHERE `name`='生椰拿铁' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='02-oat-latte' WHERE `name`='燕麦拿铁' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='02-mocha' WHERE `name`='摩卡' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='02-caramel-macchiato' WHERE `name`='焦糖玛奇朵' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='03-dirty' WHERE `name`='Cozy Dirty' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='03-osmanthus-latte' WHERE `name`='拿铁金·桂花特调' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='03-lychee-shake' WHERE `name`='冰摇荔枝咖啡' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='03-orange-sparkling' WHERE `name`='柑橘气泡美式' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-ethiopia' WHERE `name`='埃塞俄比亚·耶加雪菲' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-kenya' WHERE `name`='肯尼亚·涅里' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-brazil' WHERE `name`='巴西·米纳斯' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-colombia' WHERE `name`='哥伦比亚·安第斯' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-guatemala' WHERE `name`='危地马拉·安提瓜' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-panama' WHERE `name`='巴拿马·瑰夏' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-indonesia' WHERE `name`='印尼·曼特宁' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-origin-yunnan' WHERE `name`='云南·保山' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-one-bean-two' WHERE `name`='一豆两喝' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='04-one-bean-three' WHERE `name`='一豆三喝' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='05-matcha-latte' WHERE `name`='抹茶拿铁' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='05-cocoa' WHERE `name`='可可' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='05-osmanthus-oolong' WHERE `name`='桂花乌龙冷泡茶' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='06-salted-caramel-croissant' WHERE `name`='海盐焦糖牛角包' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='06-basque-cheesecake' WHERE `name`='巴斯克芝士蛋糕' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='06-tiramisu' WHERE `name`='提拉米苏（Cozy 版）' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='06-oat-cookie' WHERE `name`='手工燕麦曲奇' AND `status`='active';
UPDATE `coffee_products` SET `product_code`='06-chocolate-brownie' WHERE `name`='巧克力布朗尼' AND `status`='active';

-- 数据验收：active 商品 product_code 全部非空（31 个，今日冷萃已下线不补码）
-- SELECT COUNT(*) FROM coffee_products WHERE status='active' AND product_code IS NULL;  -- 期望 0
