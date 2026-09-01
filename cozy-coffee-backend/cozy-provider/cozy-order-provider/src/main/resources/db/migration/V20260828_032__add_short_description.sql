-- V2 商品体系 · 双字段描述：short_description（菜单列表凝练一句，完整展示不做省略）+ description（选规格/详情详细）
-- 契约：short_description ≤40 中文字符；咖啡/茶饮「风味词·风味词·质感」、体验「一句式体验描述」、烘焙「口感+核心卖点」
-- 列表前端不 line-clamp / 不 ellipsis；内容层主动控制长度

ALTER TABLE `coffee_products`
  ADD COLUMN `short_description` VARCHAR(100) NULL COMMENT '菜单列表简短描述（完整展示，不做省略；≤40字）；选规格/详情用 description' AFTER `description`;

-- 回填凝练描述（3.4 风味句 / 一句式；按商品短码定位）
UPDATE `coffee_products` SET `short_description`='黑巧克力 · 焦糖 · 醇厚' WHERE `name`='Espresso' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='黑巧克力 · 焦糖 · 柑橘' WHERE `name`='Cozy 美式' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='焦糖 · 牛奶巧克力 · 丝绒' WHERE `name`='澳白' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='奶香 · 咖啡 · 蓬松' WHERE `name`='卡布奇诺' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='焦糖 · 牛奶巧克力 · 顺滑' WHERE `name`='经典拿铁' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='椰香 · 焦糖 · 甜润' WHERE `name`='生椰拿铁' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='燕麦谷香 · 焦糖 · 温和' WHERE `name`='燕麦拿铁' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='黑巧 · 焦糖 · 奶油' WHERE `name`='摩卡' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='焦糖 · 香草 · 奶香' WHERE `name`='焦糖玛奇朵' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='黑巧 · 焦糖 · 醇厚' WHERE `name`='Cozy Dirty' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='桂花 · 蜂蜜 · 茶感' WHERE `name`='拿铁金·桂花特调' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='荔枝 · 茉莉 · 柑橘' WHERE `name`='冰摇荔枝咖啡' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='橙皮 · 柑橘 · 清爽' WHERE `name`='柑橘气泡美式' AND `status`='active';
-- 手冲豆单池 8 支：显式写死，与 description 解耦（description 未来可能长文，不复制）
UPDATE `coffee_products` SET `short_description`='茉莉与柑橘花香，像一杯明亮的花园茶' WHERE `name`='埃塞俄比亚·耶加雪菲' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='黑醋栗与莓果酸质，红酒般余韵' WHERE `name`='肯尼亚·涅里' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='坚果与巧克力，醇厚平衡的日常基底' WHERE `name`='巴西·米纳斯' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='焦糖与红莓，干净均衡' WHERE `name`='哥伦比亚·安第斯' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='黑巧克力与香料，沉稳层次' WHERE `name`='危地马拉·安提瓜' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='茉莉、佛手柑与蜂蜜，热带水果般的明亮芳香' WHERE `name`='巴拿马·瑰夏' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='草本与黑巧克力，厚实饱满' WHERE `name`='印尼·曼特宁' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='坚果焦糖与红茶茶感，东方的平衡表达' WHERE `name`='云南·保山' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='同一支豆，两种萃取：Espresso + 手冲' WHERE `name`='一豆两喝' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='同一支豆，三种表达：Espresso / 手冲 / 奶咖' WHERE `name`='一豆三喝' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='抹茶 · 海苔鲜 · 奶香' WHERE `name`='抹茶拿铁' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='可可 · 苦甜 · 醇厚' WHERE `name`='可可' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='桂花 · 乌龙 · 清甜' WHERE `name`='桂花乌龙冷泡茶' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='酥皮与海盐焦糖的甜咸平衡' WHERE `name`='海盐焦糖牛角包' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='焦香表面与绵密内芯' WHERE `name`='巴斯克芝士蛋糕' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='融入店内浓缩咖啡液，入口即化' WHERE `name`='提拉米苏（Cozy 版）' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='低糖手工曲奇，燕麦香气' WHERE `name`='手工燕麦曲奇' AND `status`='active';
UPDATE `coffee_products` SET `short_description`='外层微脆、内芯湿润' WHERE `name`='巧克力布朗尼' AND `status`='active';

-- 数据验收：active 商品 short_description 非空且非空串（31 个，今日冷萃已下线）
-- SELECT COUNT(*) FROM coffee_products
-- WHERE status='active' AND (short_description IS NULL OR TRIM(short_description) = '');  -- 期望 0
