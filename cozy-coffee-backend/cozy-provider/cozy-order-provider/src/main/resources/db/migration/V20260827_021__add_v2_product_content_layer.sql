-- V2 商品体系 · Phase 0A 内容层
-- coffee_origin / coffee_bean / coffee_blend 建表 + coffee_products 挂接列 + 8 来源主题种子
-- 设计文档：《CozyCoffee V2 商品体系落地设计》Part 1.2 / 1.3

-- 1) Origin Archive：咖啡来源主题档案（地理背景；档案属性在 coffee_bean）
CREATE TABLE IF NOT EXISTS `coffee_origin` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT,
  `code`              VARCHAR(32)   NOT NULL COMMENT '来源代码：ETHIOPIA/KENYA/BRAZIL/COLOMBIA/GUATEMALA/PANAMA/INDONESIA/YUNNAN',
  `country`           VARCHAR(64)   NOT NULL COMMENT '国家（英文）：Ethiopia',
  `country_zh`        VARCHAR(64)   NOT NULL COMMENT '国家（中文）：埃塞俄比亚',
  `region`            VARCHAR(128)  NULL COMMENT '产区 / 子区域：Yirgacheffe / Sidamo（YUNNAN 只到省级，子产区在 coffee_bean）',
  `typical_character` VARCHAR(255)  NULL COMMENT '来源地典型气质（宽泛，探索页文案）：花香 · 明亮 / 醇厚 · 平衡',
  `description`       VARCHAR(500)  NULL COMMENT '来源故事（手冲页 / 探索页文案）',
  `sort_order`        INT           NOT NULL DEFAULT 0,
  `status`            VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT 'active/inactive',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_origin_code` (`code`),
  KEY `idx_origin_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咖啡来源主题档案（Origin Archive）';

-- 2) 单品豆档案（Bean Profile）：一个来源主题可拆多支豆档案；如需批次/采购管理，未来单独建 Bean Batch
CREATE TABLE IF NOT EXISTS `coffee_bean` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `code`         VARCHAR(32)   NOT NULL COMMENT '豆档案代码：ETH_WASHED_LIGHT',
  `name`         VARCHAR(100)  NOT NULL COMMENT '豆名（中文）',
  `name_en`      VARCHAR(100)  NULL COMMENT '豆名（英文）',
  `origin_id`    BIGINT        NOT NULL COMMENT '所属来源主题：coffee_origin.id（逻辑关联）',
  `altitude`     VARCHAR(64)   NULL COMMENT '海拔：1,800-2,200m',
  `processing`   VARCHAR(64)   NULL COMMENT '处理法：Washed / Natural',
  `variety`      VARCHAR(128)  NULL COMMENT '品种：Heirloom / 74110',
  `roast`        VARCHAR(64)   NULL COMMENT '烘焙度：Light / Medium-Dark',
  `flavor_notes` VARCHAR(255)  NULL COMMENT '风味',
  `body`         VARCHAR(64)   NULL COMMENT '醇厚度：Full / Smooth / Dense',
  `acidity`      VARCHAR(64)   NULL COMMENT '酸度：Balanced / Bright',
  `role`         VARCHAR(64)   NULL COMMENT '角色：浓缩基底 / 花香层次',
  `description`  VARCHAR(500)  NULL COMMENT '豆档案简介',
  `sort_order`   INT           NOT NULL DEFAULT 0,
  `status`       VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT 'active/inactive',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bean_code` (`code`),
  KEY `idx_bean_origin` (`origin_id`),
  KEY `idx_bean_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咖啡豆档案（Bean Profile）';

-- 3) 拼配豆（Blend）：composition 用 JSON 保持 3 表
CREATE TABLE IF NOT EXISTS `coffee_blend` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `code`             VARCHAR(32)   NOT NULL COMMENT '拼配代码：COZY_HOUSE / VELVET_MILK',
  `name`             VARCHAR(100)  NOT NULL COMMENT '拼配名（中文）',
  `name_en`          VARCHAR(100)  NULL COMMENT '拼配名（英文）',
  `description`      VARCHAR(500)  NULL COMMENT '拼配简介',
  `composition_json` JSON          NULL COMMENT '拼配比例：[{"beanId":1,"ratio":60},{"beanId":2,"ratio":40}]，合计 100',
  `roast`            VARCHAR(64)   NULL COMMENT '拼配整体烘焙度',
  `flavor_notes`     VARCHAR(255)  NULL COMMENT '拼配整体风味',
  `body`             VARCHAR(64)   NULL COMMENT '醇厚度',
  `acidity`          VARCHAR(64)   NULL COMMENT '酸度',
  `sort_order`       INT           NOT NULL DEFAULT 0,
  `status`           VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT 'active/inactive',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blend_code` (`code`),
  KEY `idx_blend_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咖啡拼配豆（Blend）';

-- 4) coffee_products 挂接（互斥：单品豆或拼配豆二选一；default_sugar_level 可空）
ALTER TABLE `coffee_products`
  ADD COLUMN `bean_id`             BIGINT NULL COMMENT '单品豆：coffee_bean.id（与 blend_id 互斥）' AFTER `temp_type`,
  ADD COLUMN `blend_id`            BIGINT NULL COMMENT '拼配豆：coffee_blend.id（与 bean_id 互斥）' AFTER `bean_id`,
  ADD COLUMN `default_sugar_level` VARCHAR(20) NULL DEFAULT 'STANDARD' COMMENT '默认额外加糖等级：STANDARD/LESS/HALF/NO_ADDED_SUGAR；NO_SUGAR_ONLY 商品为 NULL' AFTER `sugar_type`,
  ADD COLUMN `serving_mode`        VARCHAR(20) NULL COMMENT '出杯模式：NULL=常规 / FIXED_COMBINATION=固定组合（一豆两喝·三喝）' AFTER `blend_id`,
  ADD COLUMN `serving_config`      JSON NULL COMMENT '固定组合结构化构成：[{"type":"ESPRESSO","quantity":1},{"type":"POUR_OVER","quantity":1}]' AFTER `serving_mode`,
  ADD COLUMN `serving_desc`        VARCHAR(200) NULL COMMENT '固定组合出杯说明（仅展示文案）：Espresso ×1 + 手冲 ×1' AFTER `serving_config`,
  ADD KEY `idx_products_bean` (`bean_id`),
  ADD KEY `idx_products_blend` (`blend_id`);

-- 5) 8 来源主题种子（Origin Archive）
-- 典型气质见 Part 1.3；description 沿用 web 端 coffeeOrigins.js 品牌文案迁移，作为前端 8 产区介绍文字的唯一事实来源
INSERT INTO `coffee_origin`
  (`code`,`country`,`country_zh`,`region`,`typical_character`,`description`,`sort_order`,`status`)
VALUES
  ('ETHIOPIA','Ethiopia','埃塞俄比亚','Yirgacheffe / Sidamo','花香 · 明亮 · 伯爵茶感','被视为阿拉比卡咖啡的故乡，丰富的遗传多样性让花香、果香与茶感自然并存。',1,'active'),
  ('KENYA','Kenya','肯尼亚','Nyeri / Kirinyaga','莓果 · 高酸 · 红酒余韵','高海拔与双重水洗塑造鲜明酸质，为拼配提供清晰、明亮而有张力的果香骨架。',2,'active'),
  ('BRAZIL','Brazil','巴西','Minas Gerais','坚果 · 巧克力 · 醇厚平衡','稳定的坚果、可可与焦糖甜感构成浓缩基底，让杯中结构保持醇厚与平衡。',3,'active'),
  ('COLOMBIA','Colombia','哥伦比亚','Andes','焦糖 · 红莓 · 干净均衡','安第斯山脉的多样微气候把甜感与酸质放在同一条清晰的风味轴线上。',4,'active'),
  ('GUATEMALA','Guatemala','危地马拉','Antigua','黑巧克力 · 香料 · 沉稳','火山土壤与昼夜温差积累出扎实甜感，并在尾韵中留下细致的香料层次。',5,'active'),
  ('PANAMA','Panama','巴拿马','Boquete','花香 · 佛手柑 · 明亮芳香','瑰夏把花香、柑橘与蜂蜜般甜感推向高点，为风味体系带来轻盈而明确的芳香记忆。',6,'active'),
  ('INDONESIA','Indonesia','印度尼西亚','Sumatra','草本 · 黑巧克力 · 醇厚','湿刨处理带来低沉香料感与厚重质地，为明亮风味补上深度与余韵。',7,'active'),
  ('YUNNAN','China','中国','Yunnan（云南）','坚果 · 焦糖 · 红茶茶感','高原季风与不断进步的处理实验，让云南成为 Cozy Coffee 风味语言中的东方表达。',8,'active');
