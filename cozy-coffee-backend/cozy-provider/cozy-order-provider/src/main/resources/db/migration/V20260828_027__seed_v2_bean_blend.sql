-- V2 商品体系 · 内容录入：8 单品豆 + 2 拼配 + 商品 bean_id/blend_id 挂接（设计文档 3.2/3.3）
-- 依赖：021 已建三表 + 8 产区种子；023 已建 32 商品行；024 已建加料组
-- 挂接规则（3.3 矩阵）：奶咖/经典→VELVET_MILK·COZY_HOUSE；特调按 Bean 表达风味；手冲豆单池按产区；体验商品挂默认 bean（运营可切）
-- 商品行按 name + status='active' 定位（排除已下线旧行）

-- ── 1) 单品豆（8 支，origin_id 子查询）────────────────────
INSERT INTO `coffee_bean`
  (`code`,`name`,`name_en`,`origin_id`,`altitude`,`processing`,`variety`,`roast`,`flavor_notes`,`body`,`acidity`,`role`,`description`,`sort_order`,`status`)
VALUES
  ('BRAZIL_SOE','巴西·浓缩基底','Brazil Base',(SELECT id FROM `coffee_origin` WHERE code='BRAZIL'),'800-1,200m','Natural','Bourbon / Mundo Novo','Medium-Dark','黑巧克力 · 焦糖 · 烤坚果','饱满','低酸','浓缩基底','深烘高甜，日常浓缩基底',1,'active'),
  ('ETH_WASHED','埃塞·水洗','Ethiopia Washed',(SELECT id FROM `coffee_origin` WHERE code='ETHIOPIA'),'1,900-2,200m','Washed','Heirloom','Light-Medium','茉莉 · 柑橘 · 伯爵茶','明亮','中等酸','花香层次','埃塞水洗的花香与茶感，今日豆单常驻',2,'active'),
  ('COL_WASHED','哥伦比亚·水洗','Colombia Washed',(SELECT id FROM `coffee_origin` WHERE code='COLOMBIA'),'1,600-2,000m','Washed','Caturra / Castillo','Medium','焦糖 · 红莓 · 坚果','均衡','均衡','平衡主体','干净均衡的日常主体',3,'active'),
  ('PANAMA_GEISHA','巴拿马·瑰夏','Panama Geisha',(SELECT id FROM `coffee_origin` WHERE code='PANAMA'),'1,500-1,800m','Washed','Geisha','Light','茉莉 · 佛手柑 · 热带水果 · 蜂蜜','精致','明亮','芳香高点','顶级花香与佛手柑，体验线主打',4,'active'),
  ('KENYA_SOE','肯尼亚·涅里','Kenya Nyeri',(SELECT id FROM `coffee_origin` WHERE code='KENYA'),'1,700-1,900m','Double Washed','SL28 / SL34','Light-Medium','黑醋栗 · 莓果 · 西柚 · 红酒','明亮','高酸','莓果骨架','肯尼亚双水洗的高酸莓果骨架',5,'active'),
  ('GUATEMALA_SOE','危地马拉·安提瓜','Guatemala Antigua',(SELECT id FROM `coffee_origin` WHERE code='GUATEMALA'),'1,500-1,700m','Washed','Bourbon / Caturra','Medium-Dark','黑巧克力 · 香料 · 坚果 · 柑橘','厚实','平衡','香料层次','安提瓜火山的沉稳香料层次',6,'active'),
  ('INDONESIA_SOE','印尼·曼特宁','Indonesia Mandheling',(SELECT id FROM `coffee_origin` WHERE code='INDONESIA'),'1,100-1,500m','Wet Hulled','TYPICA / Ateng','Dark','草本 · 香料 · 黑巧克力','醇厚','低酸','醇厚深度','湿刨法曼特宁的厚实苦甜',7,'active'),
  ('YUNNAN_SOE','云南·保山','Yunnan Baoshan',(SELECT id FROM `coffee_origin` WHERE code='YUNNAN'),'1,200-1,600m','Washed','Catimor','Medium','坚果 · 焦糖 · 红茶 · 柑橘','均衡','均衡','东方表达','云南保山的红茶茶感与坚果甜',8,'active');

-- ── 2) 拼配（2 支，composition_json 子查询）──────────────
INSERT INTO `coffee_blend`
  (`code`,`name`,`name_en`,`description`,`composition_json`,`roast`,`flavor_notes`,`body`,`acidity`,`sort_order`,`status`)
VALUES
  ('COZY_HOUSE','Cozy 拼配','Cozy House Blend','品牌日常基底拼配：埃塞水洗花香 × 巴西醇厚坚果',
   JSON_ARRAY(JSON_OBJECT('beanId',(SELECT id FROM `coffee_bean` WHERE code='ETH_WASHED'),'ratio',40),
              JSON_OBJECT('beanId',(SELECT id FROM `coffee_bean` WHERE code='BRAZIL_SOE'),'ratio',60)),
   'Medium-Dark','黑巧克力 · 焦糖 · 柑橘','饱满 · 平衡','平衡',1,'active'),
  ('VELVET_MILK','醇香奶咖拼配','Velvet Milk Blend','奶咖专属拼配：巴西甜感 × 哥伦比亚均衡',
   JSON_ARRAY(JSON_OBJECT('beanId',(SELECT id FROM `coffee_bean` WHERE code='BRAZIL_SOE'),'ratio',60),
              JSON_OBJECT('beanId',(SELECT id FROM `coffee_bean` WHERE code='COL_WASHED'),'ratio',40)),
   'Medium','焦糖 · 牛奶巧克力 · 坚果','顺滑 · 丝绒','柔和',2,'active');

-- ── 3) 商品挂接（bean_id / blend_id 二选一；手冲豆单池按产区）────────
-- 3a) 经典咖啡 → COZY_HOUSE 拼配
UPDATE `coffee_products` SET `blend_id`=(SELECT id FROM `coffee_blend` WHERE code='COZY_HOUSE')
  WHERE `name` IN ('Espresso','Cozy 美式') AND `status`='active';
-- 3b) 奶咖 → VELVET_MILK 拼配
UPDATE `coffee_products` SET `blend_id`=(SELECT id FROM `coffee_blend` WHERE code='VELVET_MILK')
  WHERE `name` IN ('澳白','卡布奇诺','经典拿铁','生椰拿铁','燕麦拿铁') AND `status`='active';
-- 3c) 奶咖（固定配方类）→ COZY_HOUSE
UPDATE `coffee_products` SET `blend_id`=(SELECT id FROM `coffee_blend` WHERE code='COZY_HOUSE')
  WHERE `name` IN ('摩卡','焦糖玛奇朵') AND `status`='active';
-- 3d) 招牌特调（按 Bean 表达风味方向）
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='BRAZIL_SOE')
  WHERE `name`='Cozy Dirty' AND `status`='active';
UPDATE `coffee_products` SET `blend_id`=(SELECT id FROM `coffee_blend` WHERE code='COZY_HOUSE')
  WHERE `name` IN ('拿铁金·桂花特调','柑橘气泡美式') AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='ETH_WASHED')
  WHERE `name`='冰摇荔枝咖啡' AND `status`='active';
-- 3e) 精品咖啡：手冲豆单池按产区；冷萃/体验挂默认 bean（运营可切 status）
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='ETH_WASHED')
  WHERE `name`='埃塞俄比亚·耶加雪菲' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='KENYA_SOE')
  WHERE `name`='肯尼亚·涅里' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='BRAZIL_SOE')
  WHERE `name`='巴西·米纳斯' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='COL_WASHED')
  WHERE `name`='哥伦比亚·安第斯' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='GUATEMALA_SOE')
  WHERE `name`='危地马拉·安提瓜' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='PANAMA_GEISHA')
  WHERE `name`='巴拿马·瑰夏' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='INDONESIA_SOE')
  WHERE `name`='印尼·曼特宁' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='YUNNAN_SOE')
  WHERE `name`='云南·保山' AND `status`='active';
UPDATE `coffee_products` SET `bean_id`=(SELECT id FROM `coffee_bean` WHERE code='ETH_WASHED')
  WHERE `name` IN ('今日冷萃','一豆两喝','一豆三喝') AND `status`='active';

-- ── 4) 标签录入（3.3 矩阵；TOP1 数据驱动不静态录入，手冲豆单池「视产区」由运营按当期豆设置）────────
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('CLASSIC')
  WHERE `name` IN ('澳白','卡布奇诺','经典拿铁') AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('COCONUT','COLD')
  WHERE `name`='生椰拿铁' AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('PLANT-BASED')
  WHERE `name`='燕麦拿铁' AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('COLD','SIGNATURE')
  WHERE `name`='Cozy Dirty' AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('NEW')
  WHERE `name`='拿铁金·桂花特调' AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('COLD','FRUITY','NEW')
  WHERE `name`='冰摇荔枝咖啡' AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('COLD','FRUITY','CITRUS','NEW')
  WHERE `name`='柑橘气泡美式' AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('COLD')
  WHERE `name` IN ('今日冷萃','桂花乌龙冷泡茶') AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('EXPERIENCE')
  WHERE `name`='一豆两喝' AND `status`='active';
UPDATE `coffee_products` SET `tags`=JSON_ARRAY('EXPERIENCE','LIMITED')
  WHERE `name`='一豆三喝' AND `status`='active';

-- ══════════════════════════════════════════════════════════════
-- 数据验收（本迁移应用后手动跑，期望全部通过）
-- ══════════════════════════════════════════════════════════════
-- ① 豆/拼配数量：期望 bean=8 / blend=2
-- SELECT 'bean' AS type, COUNT(*) FROM coffee_bean
-- UNION ALL SELECT 'blend', COUNT(*) FROM coffee_blend;
-- ② 咖啡商品全部挂接（active 咖啡类 bean_id/blend_id 二选一非空）
-- SELECT name, category, bean_id, blend_id FROM coffee_products
-- WHERE category IN ('ESPRESSO','MILK','SIGNATURE','SPECIALTY') AND status='active'
--   AND bean_id IS NULL AND blend_id IS NULL;  -- 期望 0 行
-- ③ 非咖啡/烘焙不挂接
-- SELECT COUNT(*) FROM coffee_products
-- WHERE category IN ('NON_COFFEE','BAKERY') AND (bean_id IS NOT NULL OR blend_id IS NOT NULL);  -- 期望 0
-- ④ 拼配 composition Σ=100（COZY_HOUSE 40+60、VELVET_MILK 60+40）
-- SELECT code, JSON_LENGTH(composition_json) FROM coffee_blend;
