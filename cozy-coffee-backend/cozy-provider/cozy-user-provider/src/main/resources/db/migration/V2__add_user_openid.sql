-- users.openid：实体/代码在引用但从未被任何迁移创建（仅本地库手加），全新库缺列。
-- 幂等：列已存在则跳过（生产/开发若曾手工 ALTER 过也不会报重复列）。
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'openid'),
    'SELECT 1',
    'ALTER TABLE users ADD COLUMN openid varchar(64) NULL');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
