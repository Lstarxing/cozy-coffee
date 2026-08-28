-- V2 商品体系 · Phase 1C 加料组 + 组内项种子
-- product_addons 补默认项 + 按《选规格总表》挂加料组/组内项（含 price_delta）
-- 商品行按 name + status='active' 子查询定位（排除 P1B 下线旧行；不依赖自增 id）
-- 设计文档 Part 2.2 / 2.3 + 选规格总表 Section 0

-- ── 0) product_addons 补默认项 + 参考价调整 ─────────────────
-- 全脂奶 = 奶型默认项（+0）；豆奶 奶型收敛暂不开放（inactive）
INSERT INTO `product_addons` (`name`,`code`,`price`,`category`,`description`,`status`,`sort_order`)
VALUES
  ('全脂奶','WHOLE_MILK',0.00,'MILK','奶型默认项','active',7),
  ('豆奶','SOY_MILK',4.00,'MILK','豆奶替换（奶型收敛，暂不开放）','inactive',8);

-- 参考价调整（主数据参考增量；实际订单价由 price_delta 决定——燕麦拿铁的 OAT price_delta=0 不受影响）
UPDATE `product_addons` SET `price`=3.00 WHERE `code` IN ('OAT_MILK','COCONUT_MILK');

-- ── 1) MILK 组（SINGLE 1/1 必选）──────────────────────
-- 1a) 标准奶型：全脂默认 +0 / 燕麦 +3 —— 澳白·卡布·拿铁·摩卡·玛奇朵·桂花·抹茶·可可
INSERT INTO `coffee_product_addon_group` (`product_id`,`category`,`selection_mode`,`min_select`,`max_select`,`sort_order`)
SELECT `id`,'MILK','SINGLE',1,1,1 FROM `coffee_products`
WHERE `name` IN ('澳白','卡布奇诺','经典拿铁','摩卡','焦糖玛奇朵','拿铁金·桂花特调','抹茶拿铁','可可') AND `status`='active';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='WHOLE_MILK'),1,0.00,1
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name` IN ('澳白','卡布奇诺','经典拿铁','摩卡','焦糖玛奇朵','拿铁金·桂花特调','抹茶拿铁','可可') AND p.`status`='active' AND g.category='MILK';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='OAT_MILK'),0,3.00,2
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name` IN ('澳白','卡布奇诺','经典拿铁','摩卡','焦糖玛奇朵','拿铁金·桂花特调','抹茶拿铁','可可') AND p.`status`='active' AND g.category='MILK';

-- 1b) 生椰拿铁：椰奶默认 +0 / 燕麦 +3
INSERT INTO `coffee_product_addon_group` (`product_id`,`category`,`selection_mode`,`min_select`,`max_select`,`sort_order`)
SELECT `id`,'MILK','SINGLE',1,1,1 FROM `coffee_products` WHERE `name`='生椰拿铁' AND `status`='active';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='COCONUT_MILK'),1,0.00,1
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name`='生椰拿铁' AND p.`status`='active' AND g.category='MILK';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='OAT_MILK'),0,3.00,2
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name`='生椰拿铁' AND p.`status`='active' AND g.category='MILK';

-- 1c) 燕麦拿铁：燕麦默认 +0 / 全脂 +0（切换不降价）
INSERT INTO `coffee_product_addon_group` (`product_id`,`category`,`selection_mode`,`min_select`,`max_select`,`sort_order`)
SELECT `id`,'MILK','SINGLE',1,1,1 FROM `coffee_products` WHERE `name`='燕麦拿铁' AND `status`='active';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='OAT_MILK'),1,0.00,1
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name`='燕麦拿铁' AND p.`status`='active' AND g.category='MILK';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='WHOLE_MILK'),0,0.00,2
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name`='燕麦拿铁' AND p.`status`='active' AND g.category='MILK';

-- ── 2) SHOT 组（SINGLE 0/1，额外浓缩 +5）：美式·卡布·拿铁·生椰·燕麦·摩卡·玛奇朵 ──
INSERT INTO `coffee_product_addon_group` (`product_id`,`category`,`selection_mode`,`min_select`,`max_select`,`sort_order`)
SELECT `id`,'SHOT','SINGLE',0,1,2 FROM `coffee_products`
WHERE `name` IN ('Cozy 美式','卡布奇诺','经典拿铁','生椰拿铁','燕麦拿铁','摩卡','焦糖玛奇朵') AND `status`='active';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='EXTRA_SHOT'),0,5.00,1
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name` IN ('Cozy 美式','卡布奇诺','经典拿铁','生椰拿铁','燕麦拿铁','摩卡','焦糖玛奇朵') AND p.`status`='active' AND g.category='SHOT';

-- ── 3) SYRUP 组（SINGLE 0/1 互斥，香草 +4 / 焦糖 +4）：仅经典拿铁·燕麦拿铁 ──
INSERT INTO `coffee_product_addon_group` (`product_id`,`category`,`selection_mode`,`min_select`,`max_select`,`sort_order`)
SELECT `id`,'SYRUP','SINGLE',0,1,3 FROM `coffee_products`
WHERE `name` IN ('经典拿铁','燕麦拿铁') AND `status`='active';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='VANILLA_SYRUP'),0,4.00,1
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name` IN ('经典拿铁','燕麦拿铁') AND p.`status`='active' AND g.category='SYRUP';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='CARAMEL_SYRUP'),0,4.00,2
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name` IN ('经典拿铁','燕麦拿铁') AND p.`status`='active' AND g.category='SYRUP';

-- ── 4) OTHER 组（MULTI 0/1，加奶泡 +3）：经典拿铁·生椰·燕麦拿铁 ──
INSERT INTO `coffee_product_addon_group` (`product_id`,`category`,`selection_mode`,`min_select`,`max_select`,`sort_order`)
SELECT `id`,'OTHER','MULTI',0,1,4 FROM `coffee_products`
WHERE `name` IN ('经典拿铁','生椰拿铁','燕麦拿铁') AND `status`='active';

INSERT INTO `coffee_product_addon` (`group_id`,`addon_id`,`is_default`,`price_delta`,`sort_order`)
SELECT g.id,(SELECT id FROM `product_addons` WHERE code='EXTRA_FOAM'),0,3.00,1
FROM `coffee_product_addon_group` g JOIN `coffee_products` p ON p.id=g.product_id
WHERE p.`name` IN ('经典拿铁','生椰拿铁','燕麦拿铁') AND p.`status`='active' AND g.category='OTHER';

-- ══════════════════════════════════════════════════════════════
-- 数据验收（本迁移应用后手动跑，期望全部通过；22 Group / 34 Item）
-- ══════════════════════════════════════════════════════════════
-- ① 组/项数量：期望 group=22 / item=34
-- SELECT 'group' AS type, COUNT(*) AS cnt FROM coffee_product_addon_group
-- UNION ALL SELECT 'item', COUNT(*) FROM coffee_product_addon;
--
-- ② 每个 MILK 组恰 1 个默认项且 price_delta=0（期望空集）
-- SELECT g.product_id, p.name
-- FROM coffee_product_addon_group g
-- JOIN coffee_product_addon ca ON ca.group_id=g.id AND ca.is_default=1
-- JOIN coffee_products p ON p.id=g.product_id
-- WHERE g.category='MILK'
-- GROUP BY g.product_id, p.name
-- HAVING COUNT(*)<>1 OR MIN(ca.price_delta)<>0;
--
-- ③ price_delta 无负值（期望空集）
-- SELECT g.id, ca.price_delta
-- FROM coffee_product_addon_group g JOIN coffee_product_addon ca ON ca.group_id=g.id
-- WHERE ca.price_delta < 0;
--
-- ④ 摩卡/玛奇朵无 SYRUP 组（期望空集）
-- SELECT p.name
-- FROM coffee_products p JOIN coffee_product_addon_group g ON g.product_id=p.id AND g.category='SYRUP'
-- WHERE p.name IN ('摩卡','焦糖玛奇朵');
--
-- ⑤ 默认奶型抽查（期望：生椰=COCONUT_MILK、燕麦拿铁=OAT_MILK、澳白=WHOLE_MILK）
-- SELECT p.name, a.code AS default_milk
-- FROM coffee_products p
-- JOIN coffee_product_addon_group g ON g.product_id=p.id AND g.category='MILK'
-- JOIN coffee_product_addon ca ON ca.group_id=g.id AND ca.is_default=1
-- JOIN product_addons a ON a.id=ca.addon_id
-- WHERE p.name IN ('生椰拿铁','燕麦拿铁','澳白');
--
-- ⑥ price_delta 明细抽查（经典拿铁 / 生椰 / 燕麦拿铁 全量）
-- SELECT p.name, g.category, a.code, ca.is_default, ca.price_delta
-- FROM coffee_products p
-- JOIN coffee_product_addon_group g ON g.product_id=p.id
-- JOIN coffee_product_addon ca ON ca.group_id=g.id
-- JOIN product_addons a ON a.id=ca.addon_id
-- WHERE p.name IN ('经典拿铁','生椰拿铁','燕麦拿铁')
-- ORDER BY p.name, g.sort_order, ca.sort_order;
