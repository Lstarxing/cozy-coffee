-- V2 商品体系 · Phase 1B 商品行最终落地 / 商品迁移
-- 新建 V2 商品行（24 固定 + 8 手冲豆单池 = 32 行），旧行下线
-- 数据源：《CozyCoffee V2 商品选规格总表》规格 + Part 3.4 商品文案 + 3.5 商品短码
-- 耦合说明：
--   1) temp_type 暂用旧枚举值（热/冰=ALL_OK、冰=COLD_ONLY、热=HOT_ONLY），P1D 砍温时 ALL_OK→HOT_COLD
--   2) bean_id/blend_id 先 NULL，豆/拼配主数据录入后挂接（互斥二选一）
--   3) image_url 先 NULL，图片生成后按 3.5 目录树填充

-- ── 迁移前断言（执行前先扫描，脏数据先修再迁）─────────────
-- SELECT temp_type, COUNT(*) FROM coffee_products GROUP BY temp_type;   -- 无未知值
-- SELECT id FROM coffee_products WHERE (size_type='DEFAULT' AND (price IS NULL OR price_medium IS NOT NULL OR price_large IS NOT NULL))
--    OR (size_type='MEDIUM_LARGE' AND (price IS NOT NULL OR price_medium IS NULL OR price_large IS NULL));  -- 价格互斥
-- SELECT id FROM coffee_products WHERE bean_id IS NOT NULL AND blend_id IS NOT NULL;  -- bean/blend 互斥

-- ── 1) 新建 V2 商品行 ─────────────────────────────────
-- 价格互斥：DEFAULT → price；MEDIUM_LARGE → price_medium + price_large（price NULL）
-- temp_type：热/冰→ALL_OK（P1D 转 HOT_COLD）；default_sugar_level：NO_SUGAR_ONLY 商品为 NULL

-- 基表 price 原为 NOT NULL，与「MEDIUM_LARGE 商品 price=NULL」互斥语义冲突，先放宽为可空（DEFAULT 商品仍要求非空，由应用层 2.8 校验）
ALTER TABLE `coffee_products` MODIFY COLUMN `price` DECIMAL(10,2) NULL COMMENT '基础价（DEFAULT 商品用；MEDIUM_LARGE 商品为 NULL）';

INSERT INTO `coffee_products`
  (`name`,`description`,`price`,`price_medium`,`price_large`,`image_url`,`category`,`status`,`sort_order`,`size_type`,`sugar_type`,`temp_type`,`default_sugar_level`,`serving_mode`,`serving_config`,`serving_desc`)
VALUES
-- 01 经典咖啡 ESPRESSO（美式在前，Espresso 不主推）
('Cozy 美式','以中深烘双拼为基底，黑巧克力与焦糖为主调，收尾干净。',NULL,22.00,25.00,NULL,'ESPRESSO','active',1,'MEDIUM_LARGE','FREE_CHOICE','ALL_OK','NO_ADDED_SUGAR',NULL,NULL,NULL),
('Espresso','双份浓缩，直接呈现 COZY HOUSE 拼配本身的黑巧克力与焦糖风味。',18.00,NULL,NULL,NULL,'ESPRESSO','active',2,'DEFAULT','NO_SUGAR_ONLY','HOT_ONLY',NULL,NULL,NULL,NULL),

-- 02 奶咖 MILK
('澳白','双份浓缩以薄奶泡融合，咖啡主体更突出，是更「咖啡」的奶咖。默认不另外加糖，可按需调整甜度。',30.00,NULL,NULL,NULL,'MILK','active',1,'DEFAULT','FREE_CHOICE','HOT_ONLY','NO_ADDED_SUGAR',NULL,NULL,NULL),
('卡布奇诺','绵密厚奶泡与浓缩交融，顶部撒可可粉，口感蓬松。',NULL,28.00,32.00,NULL,'MILK','active',2,'MEDIUM_LARGE','FREE_CHOICE','ALL_OK','STANDARD',NULL,NULL,NULL),
('经典拿铁','鲜牛乳与中深烘双拼融合，奶香与咖香平衡。',NULL,28.00,32.00,NULL,'MILK','active',3,'MEDIUM_LARGE','FREE_CHOICE','ALL_OK','STANDARD',NULL,NULL,NULL),
('生椰拿铁','生椰乳 x 浓缩，热带椰香与咖啡甜感，店内招牌。',NULL,32.00,35.00,NULL,'MILK','active',4,'MEDIUM_LARGE','FREE_CHOICE','COLD_ONLY','STANDARD',NULL,NULL,NULL),
('燕麦拿铁','燕麦奶与浓缩融合，谷物香气与焦糖甜感更突出，口感顺滑。',NULL,32.00,35.00,NULL,'MILK','active',5,'MEDIUM_LARGE','FREE_CHOICE','ALL_OK','STANDARD',NULL,NULL,NULL),
('摩卡','比利时黑巧克力酱 x 浓缩 x 鲜奶，顶层鲜奶油。',NULL,32.00,36.00,NULL,'MILK','active',6,'MEDIUM_LARGE','MIN_LESS_SWEET','ALL_OK','STANDARD',NULL,NULL,NULL),
('焦糖玛奇朵','香草糖浆打底、绵密奶泡与焦糖淋酱，浓缩与焦糖甜感层层叠加。',NULL,33.00,37.00,NULL,'MILK','active',7,'MEDIUM_LARGE','MIN_LESS_SWEET','ALL_OK','STANDARD',NULL,NULL,NULL),

-- 03 招牌特调 SIGNATURE
('Cozy Dirty','冰博克厚乳 x 热浓缩，冷热交融，大口喝出层次。',32.00,NULL,NULL,NULL,'SIGNATURE','active',1,'DEFAULT','NO_SUGAR_ONLY','COLD_ONLY',NULL,NULL,NULL,NULL),
('拿铁金·桂花特调','品牌定制款：干桂花 x 特制糖浆 x 浓缩，秋意入杯。',38.00,NULL,NULL,NULL,'SIGNATURE','active',2,'DEFAULT','MIN_LESS_SWEET','ALL_OK','STANDARD',NULL,NULL,NULL),
('冰摇荔枝咖啡','荔枝果香 x 埃塞水洗花香浓缩，冰摇出清爽果感。',36.00,NULL,NULL,NULL,'SIGNATURE','active',3,'DEFAULT','MIN_LESS_SWEET','COLD_ONLY','STANDARD',NULL,NULL,NULL),
('柑橘气泡美式','橙皮香气与浓缩以苏打水冰摇，橙皮片仅作点缀，果酸与咖啡的清爽对话。',36.00,NULL,NULL,NULL,'SIGNATURE','active',4,'DEFAULT','MIN_LESS_SWEET','COLD_ONLY','STANDARD',NULL,NULL,NULL),

-- 04 精品咖啡 SPECIALTY（今日冷萃 / 一豆两喝 / 一豆三喝 + 手冲豆单池 8 行，随豆单 rotation）
('今日冷萃','咖啡粉低温慢浸，口感更圆润低酸，是豆子另一面的表达。',38.00,NULL,NULL,NULL,'SPECIALTY','active',1,'DEFAULT','NO_SUGAR_ONLY','COLD_ONLY',NULL,NULL,NULL,NULL),
('一豆两喝','同一支豆，两种萃取：Espresso + 手冲。从浓郁到明亮，感受同一支豆子的不同表达。',58.00,NULL,NULL,NULL,'SPECIALTY','active',2,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,'FIXED_COMBINATION','[{"type":"ESPRESSO","quantity":1},{"type":"POUR_OVER","quantity":1}]','Espresso ×1 + 手冲 ×1'),
('一豆三喝','同一支豆，三种表达：Espresso / Pour Over / Milk Coffee。以一支豆理解精品咖啡的层次。',68.00,NULL,NULL,NULL,'SPECIALTY','active',3,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,'FIXED_COMBINATION','[{"type":"ESPRESSO","quantity":1},{"type":"POUR_OVER","quantity":1},{"type":"MILK_COFFEE","quantity":1}]','Espresso + 手冲 + 奶咖'),

('埃塞俄比亚·耶加雪菲','茉莉与柑橘花香，像一杯明亮的花园茶。',50.00,NULL,NULL,NULL,'SPECIALTY','active',4,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),
('肯尼亚·涅里','黑醋栗与莓果酸质，红酒般余韵。',50.00,NULL,NULL,NULL,'SPECIALTY','active',5,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),
('巴西·米纳斯','坚果与巧克力，醇厚平衡的日常基底。',50.00,NULL,NULL,NULL,'SPECIALTY','active',6,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),
('哥伦比亚·安第斯','焦糖与红莓，干净均衡。',50.00,NULL,NULL,NULL,'SPECIALTY','active',7,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),
('危地马拉·安提瓜','黑巧克力与香料，沉稳层次。',50.00,NULL,NULL,NULL,'SPECIALTY','active',8,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),
('巴拿马·瑰夏','茉莉、佛手柑与蜂蜜，热带水果般的明亮芳香。',68.00,NULL,NULL,NULL,'SPECIALTY','active',9,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),
('印尼·曼特宁','草本与黑巧克力，厚实饱满。',50.00,NULL,NULL,NULL,'SPECIALTY','active',10,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),
('云南·保山','坚果焦糖与红茶茶感，东方的平衡表达。',50.00,NULL,NULL,NULL,'SPECIALTY','active',11,'DEFAULT','NO_SUGAR_ONLY','ALL_OK',NULL,NULL,NULL,NULL),

-- 05 非咖啡 NON_COFFEE
('抹茶拿铁','精选抹茶与鲜奶融合，茶香清晰，口感细腻。',NULL,30.00,30.00,NULL,'NON_COFFEE','active',1,'MEDIUM_LARGE','FREE_CHOICE','ALL_OK','STANDARD',NULL,NULL,NULL),
('可可','可可碎慢煮，醇厚苦甜，配 Dirty 的深夜伴侣。',NULL,26.00,26.00,NULL,'NON_COFFEE','active',2,'MEDIUM_LARGE','FREE_CHOICE','HOT_ONLY','STANDARD',NULL,NULL,NULL),
('桂花乌龙冷泡茶','桂花与乌龙低温冷泡，茶韵清甜，无咖啡因。',22.00,NULL,NULL,NULL,'NON_COFFEE','active',3,'DEFAULT','NO_SUGAR_ONLY','COLD_ONLY',NULL,NULL,NULL,NULL),

-- 06 烘焙与轻食 BAKERY（isFood：无杯型/温度/甜度，bean/blend 必须 NULL）
('海盐焦糖牛角包','酥皮与海盐焦糖的甜咸平衡，适合搭配一杯美式。',18.00,NULL,NULL,NULL,'BAKERY','active',1,'DEFAULT',NULL,NULL,NULL,NULL,NULL,NULL),
('巴斯克芝士蛋糕','焦香表面与绵密内芯，浓郁但不过分甜。',35.00,NULL,NULL,NULL,'BAKERY','active',2,'DEFAULT',NULL,NULL,NULL,NULL,NULL,NULL),
('提拉米苏（Cozy 版）','融入店内浓缩咖啡液，入口即化的 Cozy 版提拉米苏。',38.00,NULL,NULL,NULL,'BAKERY','active',3,'DEFAULT',NULL,NULL,NULL,NULL,NULL,NULL),
('手工燕麦曲奇','低糖手工曲奇，燕麦香气，适合凑单。',12.00,NULL,NULL,NULL,'BAKERY','active',4,'DEFAULT',NULL,NULL,NULL,NULL,NULL,NULL),
('巧克力布朗尼','外层微脆、内芯湿润，配 Espresso / Dirty 的经典搭配。',28.00,NULL,NULL,NULL,'BAKERY','active',5,'DEFAULT',NULL,NULL,NULL,NULL,NULL,NULL);

-- ── 2) 旧行下线：所有非 6 系列分类的 active 商品置 inactive（新增行保留）────────
UPDATE `coffee_products`
SET `status` = 'inactive'
WHERE `status` = 'active'
  AND `category` NOT IN ('ESPRESSO','MILK','SIGNATURE','SPECIALTY','NON_COFFEE','BAKERY');
