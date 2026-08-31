-- V2 商品体系 · 商品图片挂接（设计文档 Part 5：31 张正式资产，图挂 coffee_products.image_url）
-- 图片资产已放网关 classpath static/images/v2/，DB 存相对路径 /images/v2/{短码}.png，前端统一拼 IMAGE_BASE
-- 设计文档 1.4 明确：coffee_origin/bean/blend 暂不建 image_url——Origin/Bean 商品视觉由商品资产短码 04-origin-{origin} 引用
-- 商品行按 name + status='active' 定位

-- 01 经典咖啡
UPDATE `coffee_products` SET `image_url`='/images/v2/01-espresso.png' WHERE `name`='Espresso' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/01-americano.png' WHERE `name`='Cozy 美式' AND `status`='active';
-- 02 奶咖
UPDATE `coffee_products` SET `image_url`='/images/v2/02-flat-white.png' WHERE `name`='澳白' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/02-cappuccino.png' WHERE `name`='卡布奇诺' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/02-caffe-latte.png' WHERE `name`='经典拿铁' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/02-coconut-latte.png' WHERE `name`='生椰拿铁' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/02-oat-latte.png' WHERE `name`='燕麦拿铁' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/02-mocha.png' WHERE `name`='摩卡' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/02-caramel-macchiato.png' WHERE `name`='焦糖玛奇朵' AND `status`='active';
-- 03 招牌特调
UPDATE `coffee_products` SET `image_url`='/images/v2/03-dirty.png' WHERE `name`='Cozy Dirty' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/03-osmanthus-latte.png' WHERE `name`='拿铁金·桂花特调' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/03-lychee-shake.png' WHERE `name`='冰摇荔枝咖啡' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/03-orange-sparkling.png' WHERE `name`='柑橘气泡美式' AND `status`='active';
-- 04 精品咖啡：手冲豆单池按产区（04-origin-{origin}，冷萃共用 Bean 产区图，一豆两喝/三喝独立图）
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-ethiopia.png' WHERE `name` IN ('今日冷萃','埃塞俄比亚·耶加雪菲') AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-kenya.png' WHERE `name`='肯尼亚·涅里' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-brazil.png' WHERE `name`='巴西·米纳斯' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-colombia.png' WHERE `name`='哥伦比亚·安第斯' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-guatemala.png' WHERE `name`='危地马拉·安提瓜' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-panama.png' WHERE `name`='巴拿马·瑰夏' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-indonesia.png' WHERE `name`='印尼·曼特宁' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-origin-yunnan.png' WHERE `name`='云南·保山' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-one-bean-two.png' WHERE `name`='一豆两喝' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/04-one-bean-three.png' WHERE `name`='一豆三喝' AND `status`='active';
-- 05 非咖啡
UPDATE `coffee_products` SET `image_url`='/images/v2/05-matcha-latte.png' WHERE `name`='抹茶拿铁' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/05-cocoa.png' WHERE `name`='可可' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/05-osmanthus-oolong.png' WHERE `name`='桂花乌龙冷泡茶' AND `status`='active';
-- 06 烘焙与轻食
UPDATE `coffee_products` SET `image_url`='/images/v2/06-salted-caramel-croissant.png' WHERE `name`='海盐焦糖牛角包' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/06-basque-cheesecake.png' WHERE `name`='巴斯克芝士蛋糕' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/06-tiramisu.png' WHERE `name`='提拉米苏（Cozy 版）' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/06-oat-cookie.png' WHERE `name`='手工燕麦曲奇' AND `status`='active';
UPDATE `coffee_products` SET `image_url`='/images/v2/06-chocolate-brownie.png' WHERE `name`='巧克力布朗尼' AND `status`='active';

-- ══════════════════════════════════════════════════════════════
-- 数据验收（本迁移应用后手动跑，期望全部通过）
-- ══════════════════════════════════════════════════════════════
-- ① active 商品 image_url 全部非空（32 行）
-- SELECT COUNT(*) FROM coffee_products WHERE status='active' AND image_url IS NULL;  -- 期望 0
-- ② 图片文件都存在（31 张；今日冷萃与埃塞共用 04-origin-ethiopia）
-- SELECT image_url FROM coffee_products WHERE status='active' ORDER BY image_url;
