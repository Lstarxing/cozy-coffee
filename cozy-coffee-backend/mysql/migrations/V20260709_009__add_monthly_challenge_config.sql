-- DH7: monthly_tasks 7 个 challenge_xxx_claimed 平铺字段 -> 配置表 + 进度表
-- 新增挑战任务时不再需要 ALTER TABLE ADD COLUMN
USE cozy_member;

CREATE TABLE monthly_challenge_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  challenge_code VARCHAR(30) NOT NULL COMMENT '挑战代码: ORDER_TIMES/BREAKFAST/DELIVERY/NEW_PRODUCT',
  challenge_name VARCHAR(50) NOT NULL COMMENT '挑战名称',
  target_value INT NOT NULL COMMENT '目标值',
  reward_points INT NOT NULL COMMENT '奖励积分',
  reward_coupon_type VARCHAR(30) NULL COMMENT '可选：奖励券类型',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_challenge (challenge_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度挑战任务配置表';

-- 初始化现有 4 个挑战数据
INSERT INTO monthly_challenge_config (challenge_code, challenge_name, target_value, reward_points) VALUES
('ORDER_TIMES', '打卡达人', 4, 40),
('BREAKFAST', '晨间唤醒', 3, 60),
('DELIVERY', '外卖尝鲜', 2, 50),
('NEW_PRODUCT', '新品猎人', 3, 80);

CREATE TABLE monthly_challenge_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL COMMENT 'monthly_tasks.id',
  challenge_code VARCHAR(30) NOT NULL COMMENT '挑战代码',
  current_value INT NOT NULL DEFAULT 0 COMMENT '当前进度',
  claimed TINYINT NOT NULL DEFAULT 0 COMMENT '是否已领取',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_challenge (task_id, challenge_code),
  INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度挑战任务进度表';

-- 旧字段暂保留（向后兼容），Phase 5+ 应用层迁移完成后删除：
-- ALTER TABLE monthly_tasks
--   DROP COLUMN challenge_order_claimed,
--   DROP COLUMN challenge_morning_claimed,
--   DROP COLUMN challenge_delivery_claimed,
--   DROP COLUMN challenge_newproduct_claimed;
