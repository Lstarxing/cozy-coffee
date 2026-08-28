-- V2 商品体系 · Phase 1A 规则层：加料组两层模型
-- coffee_product_addon_group（组选择约束）+ coffee_product_addon（组内项 + 默认标记 + price_delta）
-- 设计文档：《CozyCoffee V2 商品体系落地设计》Part 2.2 / 2.3

CREATE TABLE IF NOT EXISTS `coffee_product_addon_group` (
  `id`             BIGINT      NOT NULL AUTO_INCREMENT,
  `product_id`     BIGINT      NOT NULL COMMENT 'coffee_products.id',
  `category`       VARCHAR(20) NOT NULL COMMENT '组类别：MILK/SHOT/SYRUP/OTHER',
  `selection_mode` VARCHAR(10) NOT NULL DEFAULT 'SINGLE' COMMENT 'SINGLE=单选 / MULTI=多选',
  `min_select`     INT         NOT NULL DEFAULT 0 COMMENT '最少选择数（MILK=1）',
  `max_select`     INT         NOT NULL DEFAULT 1 COMMENT '最多选择数（MILK=1 / SHOT=1）',
  `sort_order`     INT         NOT NULL DEFAULT 0,
  `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_category` (`product_id`, `category`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品-加料组（规则层，组选择约束）';

-- 组内加料明细：默认项标记在明细上，实际价格增量由 price_delta 决定（默认项恒 0）
CREATE TABLE IF NOT EXISTS `coffee_product_addon` (
  `id`         BIGINT         NOT NULL AUTO_INCREMENT,
  `group_id`   BIGINT         NOT NULL COMMENT 'coffee_product_addon_group.id',
  `addon_id`   BIGINT         NOT NULL COMMENT 'product_addons.id',
  `is_default` TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '组内默认项（如全脂奶 / 标准）',
  `price_delta` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '该商品选择该 Addon 的实际价格增量；默认项恒 0（成本含在商品基础价），禁止负值',
  `sort_order` INT            NOT NULL DEFAULT 0,
  `created_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_addon` (`group_id`, `addon_id`),
  KEY `idx_addon` (`addon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品-加料明细（组内项 + 默认标记 + price_delta）';
