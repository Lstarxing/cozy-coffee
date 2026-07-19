/*
 Navicat Premium Data Transfer

 Source Server         : cozycoffee开发
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : cozy_member

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 10/07/2026 10:15:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for member_info
-- ----------------------------
DROP TABLE IF EXISTS `member_info`;
CREATE TABLE `member_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `member_level` enum('basic','silver','gold','diamond','black') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'basic' COMMENT '会员等级',
  `total_points` int NULL DEFAULT 0 COMMENT '累计积分（用于等级判定）',
  `current_points` int NULL DEFAULT 0 COMMENT '可用积分（可消费）',
  `consecutive_sign_days` int NULL DEFAULT 0 COMMENT '连续签到天数',
  `last_signin_date` date NULL DEFAULT NULL COMMENT '最后签到日期',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `exp_total` int NOT NULL DEFAULT 0 COMMENT 'EXP总值（仅升级用）',
  `monthly_spent` decimal(10, 2) NULL DEFAULT NULL COMMENT '褰撴湀娑堣垂閲戦?锛堜粎榛戝崱鐢ㄦ埛锛',
  `monthly_spent_month` date NULL DEFAULT NULL COMMENT 'å½“å‰æœˆåº¦æ¶ˆè´¹ç»Ÿè®¡æœˆä»½ (DATE, æœˆåˆ)',
  `monthly_accelerate_remaining` decimal(10, 2) NULL DEFAULT 300.00 COMMENT '鍔犻?鍖呭墿浣欓?搴',
  `member_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ACTIVE',
  `annual_exp` int NULL DEFAULT 0,
  `last_settlement_year` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_member_level`(`member_level` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 189 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会员信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for monthly_challenge_config
-- ----------------------------
DROP TABLE IF EXISTS `monthly_challenge_config`;
CREATE TABLE `monthly_challenge_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `challenge_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æŒ‘æˆ˜ä»£ç : ORDER_TIMES/BREAKFAST/DELIVERY/NEW_PRODUCT',
  `challenge_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æŒ‘æˆ˜åç§°',
  `target_value` int NOT NULL COMMENT 'ç›®æ ‡å€¼',
  `reward_points` int NOT NULL COMMENT 'å¥–åŠ±ç§¯åˆ†',
  `reward_coupon_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'å¯é€‰ï¼šå¥–åŠ±åˆ¸ç±»åž‹',
  `enabled` tinyint NULL DEFAULT 1 COMMENT 'æ˜¯å¦å¯ç”¨',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_challenge`(`challenge_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'æœˆåº¦æŒ‘æˆ˜ä»»åŠ¡é…ç½®è¡¨' ROW_FORMAT = Dynamic;

-- ----------------------------

-- ----------------------------
-- Table structure for monthly_challenge_progress
-- ----------------------------
DROP TABLE IF EXISTS `monthly_challenge_progress`;
CREATE TABLE `monthly_challenge_progress`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT 'monthly_tasks.id',
  `challenge_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æŒ‘æˆ˜ä»£ç ',
  `current_value` int NOT NULL DEFAULT 0 COMMENT 'å½“å‰è¿›åº¦',
  `claimed` tinyint NOT NULL DEFAULT 0 COMMENT 'æ˜¯å¦å·²é¢†å–',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_challenge`(`task_id` ASC, `challenge_code` ASC) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'æœˆåº¦æŒ‘æˆ˜ä»»åŠ¡è¿›åº¦è¡¨' ROW_FORMAT = Dynamic;

-- ----------------------------

-- ----------------------------
-- Table structure for monthly_task_orders
-- ----------------------------
DROP TABLE IF EXISTS `monthly_task_orders`;
CREATE TABLE `monthly_task_orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务月份(YYYY-MM)',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '订单实付金额',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order`(`order_id` ASC) USING BTREE,
  INDEX `idx_user_month`(`user_id` ASC, `task_month` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 121 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '月度任务订单去重表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for monthly_tasks
-- ----------------------------
DROP TABLE IF EXISTS `monthly_tasks`;
CREATE TABLE `monthly_tasks`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务月份(YYYY-MM,如2025-12)',
  `current_spent` decimal(10, 2) NOT NULL COMMENT '当月实付金额(pay_amount累计)',
  `reward_300_claimed` tinyint(1) NULL DEFAULT 0 COMMENT '满300奖励是否已发放',
  `reward_600_claimed` tinyint(1) NULL DEFAULT 0 COMMENT '满600奖励是否已发放',
  `reward_1000_claimed` tinyint(1) NULL DEFAULT 0 COMMENT '满1000奖励是否已发放',
  `challenge_order_claimed` tinyint(1) NULL DEFAULT 0 COMMENT '打卡达人(4次下单)奖励已领取',
  `challenge_morning_claimed` tinyint(1) NULL DEFAULT 0 COMMENT '晨间唤醒(3次10点前)奖励已领取',
  `challenge_delivery_claimed` tinyint(1) NULL DEFAULT 0 COMMENT '外卖尝鲜(2笔外卖)奖励已领取',
  `challenge_newproduct_claimed` tinyint(1) NULL DEFAULT 0 COMMENT '新品猎人(3款新品)奖励已领取',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_month`(`user_id` ASC, `task_month` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_task_month`(`task_month` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '月度任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for points_expiry_notifications
-- ----------------------------
DROP TABLE IF EXISTS `points_expiry_notifications`;
CREATE TABLE `points_expiry_notifications`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `lot_id` bigint NOT NULL COMMENT '积分批次ID',
  `remind_days` int NOT NULL COMMENT '提前提醒天数: 30/7/1',
  `sent_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_lot_remind`(`lot_id` ASC, `remind_days` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分到期提醒去重表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for points_lot_consumptions
-- ----------------------------
DROP TABLE IF EXISTS `points_lot_consumptions`;
CREATE TABLE `points_lot_consumptions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `lot_id` bigint NOT NULL COMMENT '批次ID',
  `consume_amount` int NOT NULL COMMENT '消耗积分',
  `consume_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消耗类型：redeem',
  `consume_id` bigint NULL DEFAULT NULL COMMENT '关联ID（兑换订单ID）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_consume_lot`(`consume_type` ASC, `consume_id` ASC, `lot_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_lot_id`(`lot_id` ASC) USING BTREE,
  INDEX `idx_consume`(`consume_type` ASC, `consume_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 219 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分扣减明细表（FIFO扣减凭证）' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for points_lots
-- ----------------------------
DROP TABLE IF EXISTS `points_lots`;
CREATE TABLE `points_lots`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `initial_amount` int NOT NULL COMMENT '初始积分',
  `remaining` int NOT NULL COMMENT '剩余积分',
  `source_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型：order_completed/signin/register/admin/migration',
  `source_id` bigint NULL DEFAULT NULL COMMENT '关联ID（订单ID/签到ID等）',
  `expires_at` datetime NOT NULL COMMENT '到期时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_expires`(`user_id` ASC, `expires_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_expires`(`expires_at` ASC) USING BTREE,
  INDEX `idx_source`(`source_type` ASC, `source_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 446 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分批次表（FIFO）' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for points_transactions
-- ----------------------------
DROP TABLE IF EXISTS `points_transactions`;
CREATE TABLE `points_transactions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `change_amount` int NOT NULL COMMENT '变动积分（正数为增加，负数为减少）',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `source_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型：signin签到/register注册/admin管理员操作',
  `source_id` bigint NULL DEFAULT NULL COMMENT '关联ID(用于幂等,如userId/orderId等)',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_reward`(`user_id` ASC, `source_type` ASC, `source_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_source_type`(`source_type` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 670 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for signin_records
-- ----------------------------
DROP TABLE IF EXISTS `signin_records`;
CREATE TABLE `signin_records`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `signin_date` date NOT NULL COMMENT '签到日期',
  `consecutive_days` int NOT NULL COMMENT '签到时的连续天数',
  `points_earned` int NOT NULL COMMENT '获得积分',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `signin_date` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_signin_date`(`signin_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '签到记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `config_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置值（JSON格式）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '配置描述',
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for user_addresses
-- ----------------------------
DROP TABLE IF EXISTS `user_addresses`;
CREATE TABLE `user_addresses`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货人电话',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '城市',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '区/县',
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '详细地址',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否默认地址：0否 1是',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 93 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户收货地址表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- View structure for v_points_balance_check
-- ----------------------------
DROP VIEW IF EXISTS `v_points_balance_check`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_points_balance_check` AS select `m`.`user_id` AS `user_id`,`m`.`current_points` AS `member_balance`,coalesce(sum(`l`.`remaining`),0) AS `lots_balance`,(`m`.`current_points` - coalesce(sum(`l`.`remaining`),0)) AS `diff`,count(`l`.`id`) AS `lot_count` from (`member_info` `m` left join `points_lots` `l` on(((`l`.`user_id` = `m`.`user_id`) and (`l`.`remaining` > 0)))) group by `m`.`user_id`,`m`.`current_points` having (`diff` <> 0);

SET FOREIGN_KEY_CHECKS = 1;
