-- DH6: coffee_products 三价格字段 (price/price_medium/price_large) -> product_sku 表
-- 当前每种杯型价格平铺在 coffee_products 行中，无法独立管理库存和状态
USE cozy_order;

CREATE TABLE product_sku (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL COMMENT 'coffee_products.id',
  sku_code VARCHAR(32) NOT NULL COMMENT 'SKU编码',
  size_type VARCHAR(10) NOT NULL COMMENT '杯型: SMALL/MEDIUM/LARGE',
  price DECIMAL(10,2) NOT NULL COMMENT '该SKU价格',
  stock INT DEFAULT 0 COMMENT '库存（未来启用）',
  enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sku_code (sku_code),
  INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU';

-- 从 coffee_products 迁移现有数据（三列转三行）
INSERT INTO product_sku (product_id, sku_code, size_type, price)
SELECT
  id AS product_id,
  CONCAT('SKU-', id, '-SMALL') AS sku_code,
  'SMALL' AS size_type,
  price
FROM coffee_products
WHERE price IS NOT NULL AND price > 0;

INSERT INTO product_sku (product_id, sku_code, size_type, price)
SELECT
  id, CONCAT('SKU-', id, '-MEDIUM'), 'MEDIUM', price_medium
FROM coffee_products
WHERE price_medium IS NOT NULL AND price_medium > 0;

INSERT INTO product_sku (product_id, sku_code, size_type, price)
SELECT
  id, CONCAT('SKU-', id, '-LARGE'), 'LARGE', price_large
FROM coffee_products
WHERE price_large IS NOT NULL AND price_large > 0;

-- 旧价格字段暂保留（向后兼容），Phase 5+ 应用层迁移后删除：
-- ALTER TABLE coffee_products DROP COLUMN price, DROP COLUMN price_medium, DROP COLUMN price_large;
