-- user_addresses.gender/label：实体在引用但从未被任何迁移创建（仅本地库手加），全新库缺列。
-- 幂等：列已存在则跳过，兼容曾手工 ALTER 过的开发/生产库。
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_addresses' AND COLUMN_NAME = 'gender'),
    'SELECT 1',
    'ALTER TABLE user_addresses ADD COLUMN gender varchar(20) NULL');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_addresses' AND COLUMN_NAME = 'label'),
    'SELECT 1',
    'ALTER TABLE user_addresses ADD COLUMN label varchar(32) NULL');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
