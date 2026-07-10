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
-- Records of member_info
-- ----------------------------
INSERT INTO `member_info` VALUES (41, 41, 'gold', 6418, 3755, 2, '2026-01-05', '2025-12-31 16:01:08', '2026-07-09 15:29:36', 2000, 926.00, '2026-01-01', 150.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (42, 43, 'diamond', 12661, 3707, 8, '2026-01-10', '2026-01-02 11:23:03', '2026-07-09 15:29:36', 6031, 1039.00, '2026-01-01', 0.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (43, 44, 'basic', 350, 350, 0, NULL, '2026-01-10 17:32:29', '2026-01-10 17:59:09', 0, 0.00, NULL, 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (44, 45, 'basic', 280, 280, 0, NULL, '2026-01-10 17:59:09', '2026-01-10 17:59:09', 0, 0.00, NULL, 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (45, 46, 'diamond', 709, 709, 0, NULL, '2026-01-10 18:33:23', '2026-07-09 15:29:36', 4131, 132.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (46, 47, 'gold', 1072, 1072, 0, NULL, '2026-01-10 19:07:24', '2026-07-09 15:29:36', 1823, 323.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (47, 48, 'basic', 332, 332, 1, '2026-01-11', '2026-01-10 19:19:25', '2026-07-09 15:29:36', 50, 50.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (48, 49, 'silver', 561, 561, 1, '2026-01-11', '2026-01-10 19:24:05', '2026-07-09 15:29:36', 671, 171.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (49, 50, 'basic', 797, 797, 0, NULL, '2026-01-10 22:18:40', '2026-07-09 15:29:36', 127, 127.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (50, 51, 'basic', 200, 200, 0, NULL, '2026-01-11 18:07:11', NULL, 0, 0.00, NULL, 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (51, 52, 'basic', 372, 372, 0, NULL, '2026-01-12 10:45:01', '2026-07-09 15:29:36', 52, 52.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (52, 53, 'basic', 223, 223, 0, NULL, '2026-01-12 14:10:44', '2026-07-09 15:29:36', 23, 23.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (53, 54, 'silver', 222, 222, 0, NULL, '2026-01-12 14:47:44', '2026-07-09 15:29:36', 521, 22.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (54, 55, 'silver', 255, 255, 0, NULL, '2026-01-12 14:59:06', '2026-07-09 15:29:36', 520, 55.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (55, 38, 'basic', 0, 0, 0, NULL, '2026-01-12 15:36:47', NULL, 0, 0.00, NULL, 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (56, 56, 'diamond', 461, 461, 0, NULL, '2026-01-12 15:42:20', '2026-07-09 15:29:36', 4048, 99.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (57, 57, 'diamond', 379, 379, 0, NULL, '2026-01-12 17:18:20', '2026-07-09 15:29:36', 4065, 66.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (58, 58, 'gold', 0, 0, 0, NULL, '2026-01-12 18:48:55', '2026-01-12 18:49:18', 1500, 0.00, NULL, 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (59, 59, 'black', 259, 259, 0, NULL, '2026-01-12 20:12:34', '2026-07-09 15:29:36', 9036, 46.00, '2026-01-01', 264.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (60, 60, 'diamond', 326, 326, 0, NULL, '2026-01-12 21:44:00', '2026-07-09 15:29:36', 4021, 22.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (61, 61, 'diamond', 313, 313, 0, NULL, '2026-01-12 22:05:01', '2026-07-09 15:29:36', 4010, 11.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (62, 62, 'black', 387, 387, 1, '2026-01-12', '2026-01-12 22:31:25', '2026-07-09 15:29:36', 9021, 66.00, '2026-01-01', 279.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (63, 63, 'black', 229, 229, 0, NULL, '2026-01-13 14:08:25', '2026-07-09 15:29:36', 9021, 22.00, '2026-01-01', 279.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (64, 64, 'black', 1145, 1145, 0, NULL, '2026-01-13 15:24:51', '2026-07-09 15:29:36', 9043, 44.00, '2026-01-01', 257.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (65, 65, 'diamond', 317, 317, 0, NULL, '2026-01-13 16:42:37', '2026-07-09 15:29:36', 4013, 14.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (66, 66, 'black', 1106, 1106, 0, NULL, '2026-01-13 17:25:19', '2026-07-09 15:29:36', 9013, 14.00, '2026-01-01', 287.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (67, 67, 'black', 2807, 2807, 1, '2026-01-13', '2026-01-13 18:15:50', '2026-07-09 15:29:36', 9021, 22.00, '2026-01-01', 279.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (68, 68, 'black', 2877, 275, 2, '2026-01-15', '2026-01-13 18:31:38', '2026-07-09 15:29:36', 9020, 30.00, '2026-01-01', 280.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (69, 69, 'basic', 12003, 5236, 7, '2026-01-15', '2026-01-15 17:35:27', '2026-01-15 19:39:50', 0, 0.00, NULL, 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (70, 70, 'silver', 496, 496, 0, NULL, '2026-01-15 19:44:35', '2026-07-09 15:29:36', 687, 188.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (71, 71, 'gold', 316, 316, 0, NULL, '2026-01-15 20:05:08', '2026-07-09 15:29:36', 1559, 60.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (72, 72, 'diamond', 326, 326, 0, NULL, '2026-01-15 20:37:43', '2026-07-09 15:29:36', 4021, 22.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (73, 73, 'black', 2040, 2040, 1, '2026-01-15', '2026-01-15 21:11:02', '2026-07-09 15:29:36', 9021, 33.00, '2026-01-01', 279.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (74, 74, 'black', 548, 548, 0, NULL, '2026-01-15 22:46:17', '2026-07-09 15:29:36', 9128, 128.00, '2026-01-01', 172.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (75, 75, 'black', 1403, 1403, 0, NULL, '2026-01-15 23:11:21', '2026-07-09 15:29:36', 10133, 133.00, '2026-01-01', 167.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (76, 76, 'basic', 424, 424, 0, NULL, '2026-01-15 23:21:22', '2026-07-09 15:29:36', 144, 144.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (77, 77, 'basic', 9999, 4399, 0, NULL, '2026-01-15 23:43:34', '2026-01-15 23:44:07', 0, 0.00, NULL, 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (78, 78, 'gold', 268, 268, 0, NULL, '2026-01-16 08:24:53', '2026-07-09 15:29:36', 1510, 11.00, '2026-01-01', 300.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (79, 79, 'black', 3025, 2845, 7, '2026-01-16', '2026-01-16 08:25:21', '2026-07-09 15:29:36', 9079, 80.00, '2026-01-01', 221.00, 'ACTIVE', 0, NULL);
INSERT INTO `member_info` VALUES (80, 19, 'basic', 252, 252, 0, NULL, '2026-07-07 14:18:17', '2026-07-09 15:29:36', 162, 162.00, '2026-07-01', 300.00, 'ACTIVE', 0, NULL);

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
-- Records of monthly_challenge_config
-- ----------------------------
INSERT INTO `monthly_challenge_config` VALUES (1, 'ORDER_TIMES', 'æ‰“å¡è¾¾äºº', 4, 40, NULL, 1, '2026-07-09 16:13:25');
INSERT INTO `monthly_challenge_config` VALUES (2, 'BREAKFAST', 'æ™¨é—´å”¤é†’', 3, 60, NULL, 1, '2026-07-09 16:13:25');
INSERT INTO `monthly_challenge_config` VALUES (3, 'DELIVERY', 'å¤–å–å°é²œ', 2, 50, NULL, 1, '2026-07-09 16:13:25');
INSERT INTO `monthly_challenge_config` VALUES (4, 'NEW_PRODUCT', 'æ–°å“çŒŽäºº', 3, 80, NULL, 1, '2026-07-09 16:13:25');

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
-- Records of monthly_challenge_progress
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
-- Records of monthly_task_orders
-- ----------------------------
INSERT INTO `monthly_task_orders` VALUES (3, 41, '2025-12', 55, 50.00, '2025-12-31 16:47:57');
INSERT INTO `monthly_task_orders` VALUES (4, 41, '2025-12', 56, 250.00, '2025-12-31 17:06:14');
INSERT INTO `monthly_task_orders` VALUES (5, 41, '2025-12', 57, 300.00, '2025-12-31 17:08:44');
INSERT INTO `monthly_task_orders` VALUES (6, 41, '2025-12', 59, 375.00, '2025-12-31 17:09:24');
INSERT INTO `monthly_task_orders` VALUES (7, 41, '2025-12', 58, 375.00, '2025-12-31 17:09:25');
INSERT INTO `monthly_task_orders` VALUES (8, 41, '2026-01', 60, 21.25, '2026-01-01 15:07:13');
INSERT INTO `monthly_task_orders` VALUES (9, 41, '2026-01', 62, 250.00, '2026-01-01 15:08:55');
INSERT INTO `monthly_task_orders` VALUES (10, 41, '2026-01', 61, 230.00, '2026-01-01 15:08:57');
INSERT INTO `monthly_task_orders` VALUES (11, 41, '2026-01', 63, 25.00, '2026-01-01 17:06:05');
INSERT INTO `monthly_task_orders` VALUES (12, 41, '2026-01', 64, 25.00, '2026-01-01 18:02:00');
INSERT INTO `monthly_task_orders` VALUES (13, 41, '2026-01', 65, 25.00, '2026-01-02 10:51:10');
INSERT INTO `monthly_task_orders` VALUES (14, 41, '2026-01', 66, 300.00, '2026-01-02 11:03:20');
INSERT INTO `monthly_task_orders` VALUES (15, 41, '2026-01', 67, 20.00, '2026-01-02 11:03:31');
INSERT INTO `monthly_task_orders` VALUES (16, 43, '2026-01', 68, 300.00, '2026-01-02 11:25:42');
INSERT INTO `monthly_task_orders` VALUES (17, 43, '2026-01', 69, 300.00, '2026-01-02 11:31:02');
INSERT INTO `monthly_task_orders` VALUES (18, 43, '2026-01', 70, 40.00, '2026-01-02 18:11:10');
INSERT INTO `monthly_task_orders` VALUES (19, 43, '2026-01', 71, 41.00, '2026-01-02 18:37:46');
INSERT INTO `monthly_task_orders` VALUES (20, 43, '2026-01', 72, 20.00, '2026-01-02 18:42:22');
INSERT INTO `monthly_task_orders` VALUES (21, 43, '2026-01', 73, 20.00, '2026-01-02 22:12:02');
INSERT INTO `monthly_task_orders` VALUES (22, 43, '2026-01', 75, 20.00, '2026-01-02 22:33:05');
INSERT INTO `monthly_task_orders` VALUES (23, 43, '2026-01', 77, 20.00, '2026-01-02 23:27:33');
INSERT INTO `monthly_task_orders` VALUES (24, 43, '2026-01', 78, 20.00, '2026-01-03 00:09:14');
INSERT INTO `monthly_task_orders` VALUES (25, 43, '2026-01', 79, 25.00, '2026-01-03 00:24:20');
INSERT INTO `monthly_task_orders` VALUES (26, 43, '2026-01', 80, 30.00, '2026-01-03 00:33:32');
INSERT INTO `monthly_task_orders` VALUES (27, 43, '2026-01', 82, 20.00, '2026-01-03 00:42:00');
INSERT INTO `monthly_task_orders` VALUES (28, 43, '2026-01', 83, 20.00, '2026-01-03 00:43:21');
INSERT INTO `monthly_task_orders` VALUES (29, 41, '2026-01', 88, 30.00, '2026-01-05 00:28:17');
INSERT INTO `monthly_task_orders` VALUES (30, 43, '2026-01', 100, 38.00, '2026-01-10 15:09:40');
INSERT INTO `monthly_task_orders` VALUES (31, 43, '2026-01', 102, 22.00, '2026-01-10 15:49:10');
INSERT INTO `monthly_task_orders` VALUES (32, 43, '2026-01', 104, 28.00, '2026-01-10 15:57:55');
INSERT INTO `monthly_task_orders` VALUES (33, 43, '2026-01', 105, 22.00, '2026-01-10 16:00:42');
INSERT INTO `monthly_task_orders` VALUES (34, 50, '2026-01', 107, 19.00, '2026-01-10 22:31:35');
INSERT INTO `monthly_task_orders` VALUES (35, 43, '2026-01', 108, 22.00, '2026-01-10 22:45:29');
INSERT INTO `monthly_task_orders` VALUES (36, 50, '2026-01', 111, 22.00, '2026-01-10 23:10:16');
INSERT INTO `monthly_task_orders` VALUES (37, 50, '2026-01', 113, 50.00, '2026-01-10 23:28:52');
INSERT INTO `monthly_task_orders` VALUES (38, 50, '2026-01', 112, 36.00, '2026-01-10 23:28:54');
INSERT INTO `monthly_task_orders` VALUES (39, 48, '2026-01', 120, 28.00, '2026-01-11 15:52:27');
INSERT INTO `monthly_task_orders` VALUES (40, 48, '2026-01', 127, 22.00, '2026-01-11 16:28:43');
INSERT INTO `monthly_task_orders` VALUES (41, 49, '2026-01', 130, 44.00, '2026-01-11 16:57:34');
INSERT INTO `monthly_task_orders` VALUES (42, 43, '2026-01', 137, 22.00, '2026-01-12 10:22:18');
INSERT INTO `monthly_task_orders` VALUES (43, 43, '2026-01', 140, 9.00, '2026-01-12 10:33:34');
INSERT INTO `monthly_task_orders` VALUES (44, 52, '2026-01', 149, 14.00, '2026-01-12 12:02:32');
INSERT INTO `monthly_task_orders` VALUES (45, 52, '2026-01', 150, 38.00, '2026-01-12 12:02:51');
INSERT INTO `monthly_task_orders` VALUES (46, 55, '2026-01', 151, 11.00, '2026-01-12 15:18:02');
INSERT INTO `monthly_task_orders` VALUES (47, 55, '2026-01', 152, 22.00, '2026-01-12 15:21:10');
INSERT INTO `monthly_task_orders` VALUES (48, 55, '2026-01', 153, 22.00, '2026-01-12 15:32:48');
INSERT INTO `monthly_task_orders` VALUES (49, 54, '2026-01', 154, 22.00, '2026-01-12 15:37:05');
INSERT INTO `monthly_task_orders` VALUES (50, 56, '2026-01', 155, 28.00, '2026-01-12 15:45:07');
INSERT INTO `monthly_task_orders` VALUES (51, 56, '2026-01', 166, 22.00, '2026-01-12 17:13:38');
INSERT INTO `monthly_task_orders` VALUES (52, 57, '2026-01', 187, 66.00, '2026-01-12 17:55:54');
INSERT INTO `monthly_task_orders` VALUES (53, 59, '2026-01', 199, 10.00, '2026-01-12 21:04:31');
INSERT INTO `monthly_task_orders` VALUES (54, 59, '2026-01', 198, 35.50, '2026-01-12 21:04:33');
INSERT INTO `monthly_task_orders` VALUES (55, 56, '2026-01', 201, 49.00, '2026-01-12 21:41:29');
INSERT INTO `monthly_task_orders` VALUES (56, 60, '2026-01', 202, 22.00, '2026-01-12 21:47:32');
INSERT INTO `monthly_task_orders` VALUES (57, 46, '2026-01', 203, 22.00, '2026-01-12 21:52:03');
INSERT INTO `monthly_task_orders` VALUES (58, 61, '2026-01', 204, 11.00, '2026-01-12 22:06:04');
INSERT INTO `monthly_task_orders` VALUES (59, 62, '2026-01', 206, 11.00, '2026-01-12 22:32:04');
INSERT INTO `monthly_task_orders` VALUES (60, 62, '2026-01', 207, 33.00, '2026-01-12 23:04:18');
INSERT INTO `monthly_task_orders` VALUES (61, 62, '2026-01', 208, 22.00, '2026-01-13 13:53:43');
INSERT INTO `monthly_task_orders` VALUES (62, 63, '2026-01', 209, 22.00, '2026-01-13 14:11:00');
INSERT INTO `monthly_task_orders` VALUES (63, 64, '2026-01', 210, 43.50, '2026-01-13 15:51:30');
INSERT INTO `monthly_task_orders` VALUES (64, 65, '2026-01', 211, 14.00, '2026-01-13 16:43:22');
INSERT INTO `monthly_task_orders` VALUES (65, 66, '2026-01', 212, 14.00, '2026-01-13 17:30:59');
INSERT INTO `monthly_task_orders` VALUES (66, 67, '2026-01', 213, 22.00, '2026-01-13 18:18:10');
INSERT INTO `monthly_task_orders` VALUES (67, 68, '2026-01', 215, 30.00, '2026-01-13 18:33:52');
INSERT INTO `monthly_task_orders` VALUES (68, 53, '2026-01', 234, 23.00, '2026-01-15 19:38:25');
INSERT INTO `monthly_task_orders` VALUES (69, 70, '2026-01', 236, 22.00, '2026-01-15 19:59:53');
INSERT INTO `monthly_task_orders` VALUES (70, 71, '2026-01', 237, 60.00, '2026-01-15 20:08:08');
INSERT INTO `monthly_task_orders` VALUES (71, 72, '2026-01', 243, 22.00, '2026-01-15 20:50:49');
INSERT INTO `monthly_task_orders` VALUES (72, 73, '2026-01', 244, 11.00, '2026-01-15 21:11:59');
INSERT INTO `monthly_task_orders` VALUES (73, 73, '2026-01', 253, 22.00, '2026-01-15 22:44:15');
INSERT INTO `monthly_task_orders` VALUES (74, 74, '2026-01', 257, 28.00, '2026-01-15 22:52:13');
INSERT INTO `monthly_task_orders` VALUES (75, 74, '2026-01', 256, 49.00, '2026-01-15 22:52:16');
INSERT INTO `monthly_task_orders` VALUES (76, 74, '2026-01', 255, 9.00, '2026-01-15 22:52:18');
INSERT INTO `monthly_task_orders` VALUES (77, 74, '2026-01', 259, 14.00, '2026-01-15 22:53:16');
INSERT INTO `monthly_task_orders` VALUES (78, 74, '2026-01', 260, 28.00, '2026-01-15 22:53:43');
INSERT INTO `monthly_task_orders` VALUES (79, 70, '2026-01', 263, 18.00, '2026-01-15 23:03:51');
INSERT INTO `monthly_task_orders` VALUES (80, 70, '2026-01', 262, 17.00, '2026-01-15 23:03:54');
INSERT INTO `monthly_task_orders` VALUES (81, 70, '2026-01', 261, 21.00, '2026-01-15 23:04:34');
INSERT INTO `monthly_task_orders` VALUES (82, 70, '2026-01', 266, 36.00, '2026-01-15 23:06:15');
INSERT INTO `monthly_task_orders` VALUES (83, 70, '2026-01', 265, 36.00, '2026-01-15 23:06:18');
INSERT INTO `monthly_task_orders` VALUES (84, 70, '2026-01', 264, 38.00, '2026-01-15 23:06:23');
INSERT INTO `monthly_task_orders` VALUES (85, 75, '2026-01', 267, 9.00, '2026-01-15 23:15:37');
INSERT INTO `monthly_task_orders` VALUES (86, 75, '2026-01', 268, 16.00, '2026-01-15 23:15:40');
INSERT INTO `monthly_task_orders` VALUES (87, 75, '2026-01', 269, 36.00, '2026-01-15 23:15:42');
INSERT INTO `monthly_task_orders` VALUES (88, 75, '2026-01', 270, 36.00, '2026-01-15 23:15:44');
INSERT INTO `monthly_task_orders` VALUES (89, 75, '2026-01', 271, 36.00, '2026-01-15 23:16:30');
INSERT INTO `monthly_task_orders` VALUES (90, 76, '2026-01', 272, 38.00, '2026-01-15 23:22:00');
INSERT INTO `monthly_task_orders` VALUES (91, 76, '2026-01', 273, 39.00, '2026-01-15 23:22:55');
INSERT INTO `monthly_task_orders` VALUES (92, 76, '2026-01', 274, 36.00, '2026-01-15 23:23:12');
INSERT INTO `monthly_task_orders` VALUES (93, 76, '2026-01', 275, 31.00, '2026-01-15 23:24:12');
INSERT INTO `monthly_task_orders` VALUES (94, 46, '2026-01', 278, 36.00, '2026-01-15 23:29:45');
INSERT INTO `monthly_task_orders` VALUES (95, 46, '2026-01', 277, 38.00, '2026-01-15 23:29:50');
INSERT INTO `monthly_task_orders` VALUES (96, 46, '2026-01', 279, 36.00, '2026-01-15 23:30:50');
INSERT INTO `monthly_task_orders` VALUES (97, 47, '2026-01', 281, 33.44, '2026-01-15 23:35:56');
INSERT INTO `monthly_task_orders` VALUES (98, 47, '2026-01', 282, 28.00, '2026-01-15 23:35:58');
INSERT INTO `monthly_task_orders` VALUES (99, 47, '2026-01', 280, 31.68, '2026-01-15 23:36:00');
INSERT INTO `monthly_task_orders` VALUES (100, 47, '2026-01', 283, 22.00, '2026-01-15 23:36:17');
INSERT INTO `monthly_task_orders` VALUES (101, 47, '2026-01', 284, 38.00, '2026-01-15 23:36:36');
INSERT INTO `monthly_task_orders` VALUES (102, 47, '2026-01', 285, 31.00, '2026-01-15 23:37:27');
INSERT INTO `monthly_task_orders` VALUES (103, 47, '2026-01', 286, 31.00, '2026-01-15 23:37:42');
INSERT INTO `monthly_task_orders` VALUES (104, 49, '2026-01', 287, 17.00, '2026-01-15 23:42:01');
INSERT INTO `monthly_task_orders` VALUES (105, 49, '2026-01', 289, 36.00, '2026-01-15 23:42:27');
INSERT INTO `monthly_task_orders` VALUES (106, 49, '2026-01', 288, 38.00, '2026-01-15 23:42:33');
INSERT INTO `monthly_task_orders` VALUES (107, 49, '2026-01', 290, 36.00, '2026-01-15 23:43:02');
INSERT INTO `monthly_task_orders` VALUES (108, 47, '2026-01', 291, 30.00, '2026-01-16 08:16:49');
INSERT INTO `monthly_task_orders` VALUES (109, 47, '2026-01', 292, 28.00, '2026-01-16 08:17:04');
INSERT INTO `monthly_task_orders` VALUES (110, 47, '2026-01', 293, 50.00, '2026-01-16 08:17:19');
INSERT INTO `monthly_task_orders` VALUES (111, 78, '2026-01', 294, 11.00, '2026-01-16 09:05:17');
INSERT INTO `monthly_task_orders` VALUES (112, 79, '2026-01', 297, 36.00, '2026-01-19 22:45:48');
INSERT INTO `monthly_task_orders` VALUES (113, 79, '2026-01', 296, 22.00, '2026-01-19 22:45:49');
INSERT INTO `monthly_task_orders` VALUES (114, 79, '2026-01', 299, 22.00, '2026-01-19 22:49:34');
INSERT INTO `monthly_task_orders` VALUES (115, 19, '2026-07', 303, 22.00, '2026-07-07 14:31:33');
INSERT INTO `monthly_task_orders` VALUES (116, 19, '2026-07', 304, 32.00, '2026-07-07 14:35:12');
INSERT INTO `monthly_task_orders` VALUES (117, 19, '2026-07', 305, 22.00, '2026-07-08 13:28:08');
INSERT INTO `monthly_task_orders` VALUES (118, 19, '2026-07', 306, 30.00, '2026-07-08 13:39:43');
INSERT INTO `monthly_task_orders` VALUES (119, 19, '2026-07', 307, 31.00, '2026-07-08 13:44:51');
INSERT INTO `monthly_task_orders` VALUES (120, 19, '2026-07', 308, 25.00, '2026-07-08 13:47:03');

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
-- Records of monthly_tasks
-- ----------------------------
INSERT INTO `monthly_tasks` VALUES (1, 41, '2025-12', 1350.00, 1, 1, 1, 0, 0, 0, 0, '2025-12-31 16:47:15', '2025-12-31 16:47:56');
INSERT INTO `monthly_tasks` VALUES (2, 41, '2026-01', 926.25, 1, 1, 0, 1, 0, 0, 0, '2026-01-01 02:41:53', '2026-01-01 15:07:13');
INSERT INTO `monthly_tasks` VALUES (3, 18, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-02 11:19:39', NULL);
INSERT INTO `monthly_tasks` VALUES (4, 43, '2026-01', 1039.00, 1, 1, 0, 1, 1, 1, 0, '2026-01-02 11:23:08', '2026-01-02 11:25:41');
INSERT INTO `monthly_tasks` VALUES (5, 44, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-10 17:36:05', NULL);
INSERT INTO `monthly_tasks` VALUES (6, 45, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-10 18:00:02', NULL);
INSERT INTO `monthly_tasks` VALUES (7, 46, '2026-01', 132.00, 0, 0, 0, 1, 0, 0, 0, '2026-01-10 18:33:31', '2026-01-12 21:52:02');
INSERT INTO `monthly_tasks` VALUES (8, 47, '2026-01', 323.12, 0, 0, 0, 1, 1, 1, 1, '2026-01-10 19:07:29', '2026-01-15 23:35:55');
INSERT INTO `monthly_tasks` VALUES (9, 48, '2026-01', 50.00, 0, 0, 0, 1, 0, 0, 0, '2026-01-10 19:19:31', '2026-01-11 15:52:26');
INSERT INTO `monthly_tasks` VALUES (10, 49, '2026-01', 171.00, 0, 0, 0, 1, 0, 1, 1, '2026-01-10 19:24:10', '2026-01-11 16:57:33');
INSERT INTO `monthly_tasks` VALUES (11, 50, '2026-01', 127.00, 0, 0, 0, 1, 0, 1, 1, '2026-01-10 22:18:45', '2026-01-10 22:31:34');
INSERT INTO `monthly_tasks` VALUES (12, 51, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-11 18:07:35', NULL);
INSERT INTO `monthly_tasks` VALUES (13, 52, '2026-01', 52.00, 0, 0, 0, 1, 0, 0, 0, '2026-01-12 10:45:06', '2026-01-12 11:31:26');
INSERT INTO `monthly_tasks` VALUES (14, 53, '2026-01', 23.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 14:10:50', '2026-01-15 19:38:25');
INSERT INTO `monthly_tasks` VALUES (15, 54, '2026-01', 22.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 14:48:06', '2026-01-12 15:37:05');
INSERT INTO `monthly_tasks` VALUES (16, 55, '2026-01', 55.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 14:59:10', '2026-01-12 15:18:02');
INSERT INTO `monthly_tasks` VALUES (17, 38, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 15:36:47', NULL);
INSERT INTO `monthly_tasks` VALUES (18, 56, '2026-01', 99.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 15:42:58', '2026-01-12 15:45:07');
INSERT INTO `monthly_tasks` VALUES (19, 57, '2026-01', 66.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 17:18:33', '2026-01-12 17:55:53');
INSERT INTO `monthly_tasks` VALUES (20, 58, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 18:49:03', NULL);
INSERT INTO `monthly_tasks` VALUES (21, 59, '2026-01', 45.50, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 20:12:51', '2026-01-12 21:04:31');
INSERT INTO `monthly_tasks` VALUES (22, 60, '2026-01', 22.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 21:44:11', '2026-01-12 21:47:32');
INSERT INTO `monthly_tasks` VALUES (23, 61, '2026-01', 11.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 22:05:28', '2026-01-12 22:06:04');
INSERT INTO `monthly_tasks` VALUES (24, 62, '2026-01', 66.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-12 22:31:32', '2026-01-12 22:32:03');
INSERT INTO `monthly_tasks` VALUES (25, 63, '2026-01', 22.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-13 14:08:38', '2026-01-13 14:10:59');
INSERT INTO `monthly_tasks` VALUES (26, 64, '2026-01', 43.50, 0, 0, 0, 0, 0, 0, 0, '2026-01-13 15:24:59', '2026-01-13 15:51:29');
INSERT INTO `monthly_tasks` VALUES (27, 65, '2026-01', 14.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-13 16:42:42', '2026-01-13 16:43:22');
INSERT INTO `monthly_tasks` VALUES (28, 66, '2026-01', 14.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-13 17:25:30', '2026-01-13 17:30:58');
INSERT INTO `monthly_tasks` VALUES (29, 67, '2026-01', 22.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-13 18:16:46', '2026-01-13 18:18:10');
INSERT INTO `monthly_tasks` VALUES (30, 68, '2026-01', 30.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-13 18:33:28', '2026-01-13 18:33:52');
INSERT INTO `monthly_tasks` VALUES (31, 69, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-15 17:35:35', NULL);
INSERT INTO `monthly_tasks` VALUES (32, 70, '2026-01', 188.00, 0, 0, 0, 1, 0, 1, 0, '2026-01-15 19:44:39', '2026-01-15 19:59:53');
INSERT INTO `monthly_tasks` VALUES (33, 71, '2026-01', 60.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-15 20:05:19', '2026-01-15 20:08:07');
INSERT INTO `monthly_tasks` VALUES (34, 72, '2026-01', 22.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-15 20:37:55', '2026-01-15 20:50:49');
INSERT INTO `monthly_tasks` VALUES (35, 73, '2026-01', 33.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-15 21:11:13', '2026-01-15 21:11:58');
INSERT INTO `monthly_tasks` VALUES (36, 74, '2026-01', 128.00, 0, 0, 0, 1, 0, 1, 0, '2026-01-15 22:46:25', '2026-01-15 22:52:13');
INSERT INTO `monthly_tasks` VALUES (37, 75, '2026-01', 133.00, 0, 0, 0, 1, 0, 1, 0, '2026-01-15 23:11:28', '2026-01-15 23:15:37');
INSERT INTO `monthly_tasks` VALUES (38, 76, '2026-01', 144.00, 0, 0, 0, 0, 0, 0, 1, '2026-01-15 23:21:20', '2026-01-15 23:21:59');
INSERT INTO `monthly_tasks` VALUES (39, 77, '2026-01', 0.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-15 23:43:38', NULL);
INSERT INTO `monthly_tasks` VALUES (40, 78, '2026-01', 11.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-16 08:24:58', '2026-01-16 09:05:16');
INSERT INTO `monthly_tasks` VALUES (41, 79, '2026-01', 80.00, 0, 0, 0, 0, 0, 0, 0, '2026-01-16 08:25:27', '2026-01-19 22:45:47');
INSERT INTO `monthly_tasks` VALUES (42, 19, '2026-07', 162.00, 0, 0, 0, 1, 0, 1, 0, '2026-07-07 14:18:17', '2026-07-07 14:31:33');

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
-- Records of points_expiry_notifications
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
-- Records of points_lot_consumptions
-- ----------------------------
INSERT INTO `points_lot_consumptions` VALUES (2, 39, 16, 25, 'redeem', 31, '2025-12-28 01:20:24');
INSERT INTO `points_lot_consumptions` VALUES (3, 39, 19, 2975, 'redeem', 31, '2025-12-28 01:20:24');
INSERT INTO `points_lot_consumptions` VALUES (4, 39, 19, 400, 'redeem', 32, '2025-12-28 08:42:23');
INSERT INTO `points_lot_consumptions` VALUES (5, 39, 19, 400, 'redeem', 33, '2025-12-28 10:28:27');
INSERT INTO `points_lot_consumptions` VALUES (6, 39, 19, 400, 'redeem', 34, '2025-12-28 11:01:19');
INSERT INTO `points_lot_consumptions` VALUES (7, 22, 4, 400, 'redeem', 35, '2025-12-28 11:04:05');
INSERT INTO `points_lot_consumptions` VALUES (8, 22, 4, 400, 'redeem', 36, '2025-12-28 11:12:01');
INSERT INTO `points_lot_consumptions` VALUES (9, 22, 4, 6000, 'redeem', 37, '2025-12-28 11:13:57');
INSERT INTO `points_lot_consumptions` VALUES (10, 22, 4, 1600, 'redeem', 38, '2025-12-28 11:14:16');
INSERT INTO `points_lot_consumptions` VALUES (11, 22, 4, 215, 'redeem', 39, '2025-12-28 12:22:33');
INSERT INTO `points_lot_consumptions` VALUES (12, 22, 17, 1, 'redeem', 39, '2025-12-28 12:22:33');
INSERT INTO `points_lot_consumptions` VALUES (13, 22, 22, 666, 'redeem', 39, '2025-12-28 12:22:33');
INSERT INTO `points_lot_consumptions` VALUES (14, 22, 22, 882, 'redeem', 40, '2025-12-28 12:39:31');
INSERT INTO `points_lot_consumptions` VALUES (15, 22, 22, 882, 'redeem', 41, '2025-12-28 13:15:50');
INSERT INTO `points_lot_consumptions` VALUES (16, 22, 22, 882, 'redeem', 42, '2025-12-28 13:25:14');
INSERT INTO `points_lot_consumptions` VALUES (17, 22, 22, 450, 'redeem', 43, '2025-12-28 13:59:00');
INSERT INTO `points_lot_consumptions` VALUES (18, 22, 22, 252, 'redeem', 44, '2025-12-28 14:39:21');
INSERT INTO `points_lot_consumptions` VALUES (19, 22, 22, 252, 'redeem', 45, '2025-12-28 15:40:17');
INSERT INTO `points_lot_consumptions` VALUES (20, 22, 22, 734, 'redeem', 46, '2025-12-28 15:47:29');
INSERT INTO `points_lot_consumptions` VALUES (21, 22, 23, 43, 'redeem', 46, '2025-12-28 15:47:29');
INSERT INTO `points_lot_consumptions` VALUES (22, 22, 24, 43, 'redeem', 46, '2025-12-28 15:47:29');
INSERT INTO `points_lot_consumptions` VALUES (23, 22, 25, 62, 'redeem', 46, '2025-12-28 15:47:29');
INSERT INTO `points_lot_consumptions` VALUES (24, 22, 25, 900, 'redeem', 47, '2025-12-28 16:00:25');
INSERT INTO `points_lot_consumptions` VALUES (25, 22, 25, 900, 'redeem', 48, '2025-12-28 16:14:35');
INSERT INTO `points_lot_consumptions` VALUES (26, 22, 25, 900, 'redeem', 49, '2025-12-28 16:49:06');
INSERT INTO `points_lot_consumptions` VALUES (27, 22, 25, 238, 'redeem', 50, '2025-12-28 18:33:07');
INSERT INTO `points_lot_consumptions` VALUES (28, 22, 26, 388, 'redeem', 50, '2025-12-28 18:33:07');
INSERT INTO `points_lot_consumptions` VALUES (29, 22, 27, 48, 'redeem', 50, '2025-12-28 18:33:07');
INSERT INTO `points_lot_consumptions` VALUES (30, 22, 28, 226, 'redeem', 50, '2025-12-28 18:33:07');
INSERT INTO `points_lot_consumptions` VALUES (31, 22, 28, 252, 'redeem', 51, '2025-12-28 19:30:43');
INSERT INTO `points_lot_consumptions` VALUES (32, 41, 31, 200, 'redeem', 52, '2025-12-31 18:05:04');
INSERT INTO `points_lot_consumptions` VALUES (33, 41, 34, 50, 'redeem', 52, '2025-12-31 18:05:04');
INSERT INTO `points_lot_consumptions` VALUES (34, 41, 35, 30, 'redeem', 52, '2025-12-31 18:05:04');
INSERT INTO `points_lot_consumptions` VALUES (35, 41, 35, 270, 'redeem', 53, '2025-12-31 18:18:43');
INSERT INTO `points_lot_consumptions` VALUES (36, 41, 36, 10, 'redeem', 53, '2025-12-31 18:18:43');
INSERT INTO `points_lot_consumptions` VALUES (37, 41, 36, 40, 'redeem', 54, '2025-12-31 18:18:45');
INSERT INTO `points_lot_consumptions` VALUES (38, 41, 37, 240, 'redeem', 54, '2025-12-31 18:18:45');
INSERT INTO `points_lot_consumptions` VALUES (39, 41, 37, 10, 'redeem', 55, '2025-12-31 18:27:52');
INSERT INTO `points_lot_consumptions` VALUES (40, 41, 38, 120, 'redeem', 55, '2025-12-31 18:27:52');
INSERT INTO `points_lot_consumptions` VALUES (41, 41, 39, 150, 'redeem', 55, '2025-12-31 18:27:52');
INSERT INTO `points_lot_consumptions` VALUES (42, 41, 39, 150, 'redeem', 56, '2025-12-31 18:27:55');
INSERT INTO `points_lot_consumptions` VALUES (43, 41, 40, 130, 'redeem', 56, '2025-12-31 18:27:55');
INSERT INTO `points_lot_consumptions` VALUES (44, 41, 40, 50, 'redeem', 57, '2025-12-31 18:27:56');
INSERT INTO `points_lot_consumptions` VALUES (45, 41, 41, 230, 'redeem', 57, '2025-12-31 18:27:56');
INSERT INTO `points_lot_consumptions` VALUES (46, 41, 41, 1, 'redeem', 58, '2025-12-31 19:45:32');
INSERT INTO `points_lot_consumptions` VALUES (47, 41, 41, 1, 'redeem', 59, '2025-12-31 19:45:45');
INSERT INTO `points_lot_consumptions` VALUES (48, 41, 41, 1, 'redeem', 60, '2026-01-01 14:20:50');
INSERT INTO `points_lot_consumptions` VALUES (61, 41, 41, 142, 'redeem', 64, '2026-01-01 15:02:02');
INSERT INTO `points_lot_consumptions` VALUES (62, 41, 42, 413, 'redeem', 64, '2026-01-01 15:02:02');
INSERT INTO `points_lot_consumptions` VALUES (63, 41, 43, 368, 'redeem', 64, '2026-01-01 15:02:02');
INSERT INTO `points_lot_consumptions` VALUES (64, 41, 52, 10, 'redeem', 64, '2026-01-01 15:02:02');
INSERT INTO `points_lot_consumptions` VALUES (65, 41, 53, 47, 'redeem', 64, '2026-01-01 15:02:02');
INSERT INTO `points_lot_consumptions` VALUES (66, 43, 65, 200, 'redeem', 65, '2026-01-02 17:44:48');
INSERT INTO `points_lot_consumptions` VALUES (67, 43, 66, 70, 'redeem', 65, '2026-01-02 17:44:48');
INSERT INTO `points_lot_consumptions` VALUES (68, 43, 66, 1, 'redeem', 66, '2026-01-02 18:03:25');
INSERT INTO `points_lot_consumptions` VALUES (69, 43, 66, 319, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (70, 43, 67, 300, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (71, 43, 68, 144, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (72, 43, 69, 430, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (73, 43, 70, 216, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (74, 43, 71, 54, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (75, 43, 72, 55, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (76, 43, 73, 27, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (77, 43, 74, 27, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (78, 43, 75, 27, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (79, 43, 76, 201, 'redeem', 67, '2026-01-02 22:51:46');
INSERT INTO `points_lot_consumptions` VALUES (80, 43, 76, 1800, 'redeem', 68, '2026-01-02 23:11:10');
INSERT INTO `points_lot_consumptions` VALUES (81, 43, 76, 833, 'redeem', 69, '2026-01-02 23:41:49');
INSERT INTO `points_lot_consumptions` VALUES (82, 43, 76, 1700, 'redeem', 70, '2026-01-02 23:53:52');
INSERT INTO `points_lot_consumptions` VALUES (83, 43, 76, 1700, 'redeem', 71, '2026-01-03 00:32:43');
INSERT INTO `points_lot_consumptions` VALUES (84, 43, 76, 102, 'redeem', 72, '2026-01-10 15:01:40');
INSERT INTO `points_lot_consumptions` VALUES (85, 43, 76, 102, 'redeem', 73, '2026-01-10 15:51:34');
INSERT INTO `points_lot_consumptions` VALUES (86, 43, 76, 204, 'redeem', 74, '2026-01-10 16:36:50');
INSERT INTO `points_lot_consumptions` VALUES (87, 43, 76, 298, 'redeem', 75, '2026-01-10 22:45:48');
INSERT INTO `points_lot_consumptions` VALUES (88, 43, 76, 72, 'redeem', 76, '2026-01-11 14:20:38');
INSERT INTO `points_lot_consumptions` VALUES (89, 43, 76, 72, 'redeem', 77, '2026-01-11 17:44:54');
INSERT INTO `points_lot_consumptions` VALUES (90, 68, 191, 47, 'redeem', 78, '2026-01-13 21:31:32');
INSERT INTO `points_lot_consumptions` VALUES (91, 68, 192, 21, 'redeem', 78, '2026-01-13 21:31:32');
INSERT INTO `points_lot_consumptions` VALUES (92, 68, 192, 102, 'redeem', 79, '2026-01-13 21:31:57');
INSERT INTO `points_lot_consumptions` VALUES (93, 68, 192, 128, 'redeem', 80, '2026-01-13 21:31:59');
INSERT INTO `points_lot_consumptions` VALUES (94, 68, 192, 298, 'redeem', 81, '2026-01-13 21:32:01');
INSERT INTO `points_lot_consumptions` VALUES (95, 68, 192, 680, 'redeem', 82, '2026-01-13 21:32:13');
INSERT INTO `points_lot_consumptions` VALUES (96, 68, 192, 459, 'redeem', 83, '2026-01-15 17:19:25');
INSERT INTO `points_lot_consumptions` VALUES (97, 68, 193, 200, 'redeem', 83, '2026-01-15 17:19:25');
INSERT INTO `points_lot_consumptions` VALUES (98, 68, 194, 21, 'redeem', 83, '2026-01-15 17:19:25');
INSERT INTO `points_lot_consumptions` VALUES (99, 68, 194, 510, 'redeem', 84, '2026-01-15 17:22:29');
INSERT INTO `points_lot_consumptions` VALUES (100, 68, 194, 68, 'redeem', 85, '2026-01-15 17:26:56');
INSERT INTO `points_lot_consumptions` VALUES (101, 68, 194, 68, 'redeem', 86, '2026-01-15 17:33:21');
INSERT INTO `points_lot_consumptions` VALUES (102, 69, 197, 80, 'redeem', 87, '2026-01-15 17:38:00');
INSERT INTO `points_lot_consumptions` VALUES (103, 69, 197, 80, 'redeem', 88, '2026-01-15 17:53:16');
INSERT INTO `points_lot_consumptions` VALUES (104, 69, 197, 120, 'redeem', 89, '2026-01-15 17:53:22');
INSERT INTO `points_lot_consumptions` VALUES (105, 69, 197, 150, 'redeem', 90, '2026-01-15 17:53:30');
INSERT INTO `points_lot_consumptions` VALUES (106, 69, 197, 350, 'redeem', 91, '2026-01-15 17:53:38');
INSERT INTO `points_lot_consumptions` VALUES (107, 69, 197, 220, 'redeem', 92, '2026-01-15 17:56:21');
INSERT INTO `points_lot_consumptions` VALUES (108, 69, 198, 580, 'redeem', 92, '2026-01-15 17:56:21');
INSERT INTO `points_lot_consumptions` VALUES (109, 69, 198, 350, 'redeem', 93, '2026-01-15 18:02:45');
INSERT INTO `points_lot_consumptions` VALUES (110, 69, 198, 70, 'redeem', 94, '2026-01-15 18:04:41');
INSERT INTO `points_lot_consumptions` VALUES (111, 69, 199, 730, 'redeem', 94, '2026-01-15 18:04:41');
INSERT INTO `points_lot_consumptions` VALUES (112, 69, 199, 200, 'redeem', 95, '2026-01-15 18:16:38');
INSERT INTO `points_lot_consumptions` VALUES (113, 69, 199, 200, 'redeem', 96, '2026-01-15 18:27:34');
INSERT INTO `points_lot_consumptions` VALUES (114, 69, 199, 1200, 'redeem', 97, '2026-01-15 18:33:11');
INSERT INTO `points_lot_consumptions` VALUES (115, 69, 199, 1200, 'redeem', 98, '2026-01-15 18:41:22');
INSERT INTO `points_lot_consumptions` VALUES (116, 69, 199, 1200, 'redeem', 99, '2026-01-15 18:55:47');
INSERT INTO `points_lot_consumptions` VALUES (117, 69, 199, 1, 'redeem', 100, '2026-01-15 18:56:51');
INSERT INTO `points_lot_consumptions` VALUES (118, 69, 199, 1, 'redeem', 101, '2026-01-15 18:58:22');
INSERT INTO `points_lot_consumptions` VALUES (119, 69, 199, 1, 'redeem', 102, '2026-01-15 19:09:54');
INSERT INTO `points_lot_consumptions` VALUES (120, 69, 199, 1, 'redeem', 103, '2026-01-15 19:10:47');
INSERT INTO `points_lot_consumptions` VALUES (121, 69, 199, 11, 'redeem', 104, '2026-01-15 19:12:08');
INSERT INTO `points_lot_consumptions` VALUES (122, 69, 199, 11, 'redeem', 105, '2026-01-15 19:15:42');
INSERT INTO `points_lot_consumptions` VALUES (123, 69, 199, 11, 'redeem', 106, '2026-01-15 19:28:21');
INSERT INTO `points_lot_consumptions` VALUES (124, 77, 272, 80, 'redeem', 107, '2026-01-15 23:44:19');
INSERT INTO `points_lot_consumptions` VALUES (125, 77, 272, 120, 'redeem', 108, '2026-01-15 23:44:27');
INSERT INTO `points_lot_consumptions` VALUES (126, 77, 272, 350, 'redeem', 109, '2026-01-15 23:44:36');
INSERT INTO `points_lot_consumptions` VALUES (127, 77, 272, 150, 'redeem', 110, '2026-01-15 23:44:38');
INSERT INTO `points_lot_consumptions` VALUES (128, 77, 272, 800, 'redeem', 111, '2026-01-15 23:45:16');
INSERT INTO `points_lot_consumptions` VALUES (129, 77, 272, 3500, 'redeem', 112, '2026-01-15 23:46:32');
INSERT INTO `points_lot_consumptions` VALUES (130, 77, 272, 600, 'redeem', 113, '2026-01-15 23:47:46');
INSERT INTO `points_lot_consumptions` VALUES (131, 79, 278, 2, 'redeem', 114, '2026-01-16 09:19:45');
INSERT INTO `points_lot_consumptions` VALUES (132, 79, 282, 70, 'redeem', 114, '2026-01-16 09:19:45');
INSERT INTO `points_lot_consumptions` VALUES (133, 79, 282, 108, 'redeem', 115, '2026-01-16 09:19:47');

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
-- Records of points_lots
-- ----------------------------
INSERT INTO `points_lots` VALUES (1, 19, 4876, 4876, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (2, 20, 235, 235, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (3, 21, 8502, 8502, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (4, 22, 8615, 0, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (5, 23, 124, 124, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (6, 24, 3102, 3102, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (7, 25, 100, 100, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (8, 26, 100, 100, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (9, 27, 100, 100, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (10, 28, 100, 100, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (11, 29, 100, 100, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (12, 30, 100, 100, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (13, 31, 100, 100, 'migration', NULL, '2026-12-26 21:07:30', '2025-12-26 21:07:30');
INSERT INTO `points_lots` VALUES (16, 39, 25, 0, 'signin', 13, '2026-12-28 00:20:47', '2025-12-28 00:20:47');
INSERT INTO `points_lots` VALUES (17, 22, 1, 0, 'admin_repair', NULL, '2026-12-28 01:18:03', '2025-12-28 01:18:03');
INSERT INTO `points_lots` VALUES (18, 30, 1, 1, 'admin_repair', NULL, '2026-12-28 01:18:03', '2025-12-28 01:18:03');
INSERT INTO `points_lots` VALUES (19, 39, 9000, 4825, 'admin_repair', NULL, '2026-12-28 01:18:03', '2025-12-28 01:18:03');
INSERT INTO `points_lots` VALUES (20, 39, 23, 23, 'order_completed', 21, '2026-12-28 08:57:11', '2025-12-28 08:57:11');
INSERT INTO `points_lots` VALUES (21, 39, 86, 86, 'order_completed', 23, '2026-12-28 09:25:09', '2025-12-28 09:25:09');
INSERT INTO `points_lots` VALUES (22, 22, 5000, 0, 'admin', NULL, '2026-12-28 12:22:10', '2025-12-28 12:22:10');
INSERT INTO `points_lots` VALUES (23, 22, 43, 0, 'order_completed', 35, '2026-12-28 15:26:57', '2025-12-28 15:26:57');
INSERT INTO `points_lots` VALUES (24, 22, 43, 0, 'order_completed', 36, '2026-12-28 15:41:44', '2025-12-28 15:41:44');
INSERT INTO `points_lots` VALUES (25, 22, 3000, 0, 'admin', NULL, '2026-12-28 15:46:49', '2025-12-28 15:46:49');
INSERT INTO `points_lots` VALUES (26, 22, 388, 0, 'order_completed', 38, '2026-12-28 15:52:01', '2025-12-28 15:52:01');
INSERT INTO `points_lots` VALUES (27, 22, 48, 0, 'order_completed', 39, '2026-12-28 15:54:17', '2025-12-28 15:54:17');
INSERT INTO `points_lots` VALUES (28, 22, 3000, 2522, 'admin', NULL, '2026-12-28 18:30:54', '2025-12-28 18:30:54');
INSERT INTO `points_lots` VALUES (29, 22, 144, 144, 'monthly_mission', 4, '2026-12-28 21:14:49', '2025-12-28 21:14:49');
INSERT INTO `points_lots` VALUES (30, 40, 200, 200, 'register', 40, '2026-12-31 15:55:11', '2025-12-31 15:55:11');
INSERT INTO `points_lots` VALUES (31, 41, 200, 0, 'register', 41, '2026-12-31 16:01:08', '2025-12-31 16:01:08');
INSERT INTO `points_lots` VALUES (32, 39, 20, 20, 'signin', 15, '2026-12-31 16:13:31', '2025-12-31 16:13:31');
INSERT INTO `points_lots` VALUES (34, 41, 50, 0, 'order_completed', 54, '2026-12-31 16:38:39', '2025-12-31 16:38:39');
INSERT INTO `points_lots` VALUES (35, 41, 300, 0, 'first_order_bonus', 41, '2026-12-31 16:38:39', '2025-12-31 16:38:39');
INSERT INTO `points_lots` VALUES (36, 41, 50, 0, 'order_completed', 55, '2026-12-31 16:47:57', '2025-12-31 16:47:57');
INSERT INTO `points_lots` VALUES (37, 41, 250, 0, 'order_completed', 56, '2026-12-31 17:06:14', '2025-12-31 17:06:14');
INSERT INTO `points_lots` VALUES (38, 41, 120, 0, 'monthly_task_bonus', 1601967583, '2026-12-31 17:06:14', '2025-12-31 17:06:14');
INSERT INTO `points_lots` VALUES (39, 41, 300, 0, 'order_completed', 57, '2026-12-31 17:08:44', '2025-12-31 17:08:44');
INSERT INTO `points_lots` VALUES (40, 41, 180, 0, 'monthly_task_bonus', 1601970466, '2026-12-31 17:08:44', '2025-12-31 17:08:44');
INSERT INTO `points_lots` VALUES (41, 41, 375, 0, 'order_completed', 59, '2026-12-31 17:09:24', '2025-12-31 17:09:24');
INSERT INTO `points_lots` VALUES (42, 41, 413, 0, 'order_completed', 58, '2026-12-31 17:09:25', '2025-12-31 17:09:25');
INSERT INTO `points_lots` VALUES (43, 41, 368, 0, 'monthly_task_bonus', -1878672013, '2026-12-31 17:09:25', '2025-12-31 17:09:25');
INSERT INTO `points_lots` VALUES (52, 41, 10, 0, 'signin', 24, '2027-01-01 01:24:03', '2026-01-01 01:24:03');
INSERT INTO `points_lots` VALUES (53, 41, 1000, 953, 'admin', NULL, '2027-01-01 15:00:36', '2026-01-01 15:00:36');
INSERT INTO `points_lots` VALUES (54, 41, 28, 28, 'order_completed', 60, '2027-01-01 15:07:13', '2026-01-01 15:07:13');
INSERT INTO `points_lots` VALUES (55, 41, 325, 325, 'order_completed', 62, '2027-01-01 15:08:55', '2026-01-01 15:08:55');
INSERT INTO `points_lots` VALUES (56, 41, 299, 299, 'order_completed', 61, '2027-01-01 15:08:57', '2026-01-01 15:08:57');
INSERT INTO `points_lots` VALUES (57, 41, 138, 138, 'monthly_task_bonus', 61, '2027-01-01 15:08:57', '2026-01-01 15:08:57');
INSERT INTO `points_lots` VALUES (58, 41, 33, 33, 'order_completed', 63, '2027-01-01 17:06:05', '2026-01-01 17:06:05');
INSERT INTO `points_lots` VALUES (59, 41, 34, 34, 'order_completed', 64, '2027-01-01 18:02:00', '2026-01-01 18:02:00');
INSERT INTO `points_lots` VALUES (60, 41, 15, 15, 'signin', 25, '2027-01-02 10:40:29', '2026-01-02 10:40:29');
INSERT INTO `points_lots` VALUES (61, 41, 34, 34, 'order_completed', 65, '2027-01-02 10:51:10', '2026-01-02 10:51:10');
INSERT INTO `points_lots` VALUES (62, 41, 390, 390, 'order_completed', 66, '2027-01-02 11:03:20', '2026-01-02 11:03:20');
INSERT INTO `points_lots` VALUES (63, 41, 216, 216, 'monthly_task_bonus', 66, '2027-01-02 11:03:20', '2026-01-02 11:03:20');
INSERT INTO `points_lots` VALUES (64, 41, 27, 27, 'order_completed', 67, '2027-01-02 11:03:31', '2026-01-02 11:03:31');
INSERT INTO `points_lots` VALUES (65, 43, 200, 0, 'register', 43, '2027-01-02 11:23:04', '2026-01-02 11:23:04');
INSERT INTO `points_lots` VALUES (66, 43, 390, 0, 'order_completed', 68, '2027-01-02 11:25:42', '2026-01-02 11:25:42');
INSERT INTO `points_lots` VALUES (67, 43, 300, 0, 'first_order_bonus', 43, '2027-01-02 11:25:42', '2026-01-02 11:25:42');
INSERT INTO `points_lots` VALUES (68, 43, 144, 0, 'monthly_task_bonus', 68, '2027-01-02 11:25:42', '2026-01-02 11:25:42');
INSERT INTO `points_lots` VALUES (69, 43, 430, 0, 'order_completed', 69, '2027-01-02 11:31:02', '2026-01-02 11:31:02');
INSERT INTO `points_lots` VALUES (70, 43, 216, 0, 'monthly_task_bonus', 69, '2027-01-02 11:31:02', '2026-01-02 11:31:02');
INSERT INTO `points_lots` VALUES (71, 43, 54, 0, 'order_completed', 70, '2027-01-02 18:11:10', '2026-01-02 18:11:10');
INSERT INTO `points_lots` VALUES (72, 43, 55, 0, 'order_completed', 71, '2027-01-02 18:37:46', '2026-01-02 18:37:46');
INSERT INTO `points_lots` VALUES (73, 43, 27, 0, 'order_completed', 72, '2027-01-02 18:42:22', '2026-01-02 18:42:22');
INSERT INTO `points_lots` VALUES (74, 43, 27, 0, 'order_completed', 73, '2027-01-02 22:12:02', '2026-01-02 22:12:02');
INSERT INTO `points_lots` VALUES (75, 43, 27, 0, 'order_completed', 75, '2027-01-02 22:33:05', '2026-01-02 22:33:05');
INSERT INTO `points_lots` VALUES (76, 43, 9999, 2915, 'admin', NULL, '2027-01-02 22:50:57', '2026-01-02 22:50:57');
INSERT INTO `points_lots` VALUES (77, 43, 34, 34, 'order_completed', 77, '2027-01-02 23:27:33', '2026-01-02 23:27:33');
INSERT INTO `points_lots` VALUES (78, 43, 40, 40, 'order_completed', 79, '2027-01-03 00:24:20', '2026-01-03 00:24:20');
INSERT INTO `points_lots` VALUES (79, 43, 47, 47, 'order_completed', 80, '2027-01-03 00:33:32', '2026-01-03 00:33:32');
INSERT INTO `points_lots` VALUES (80, 43, 27, 27, 'order_completed', 82, '2027-01-03 00:42:00', '2026-01-03 00:42:00');
INSERT INTO `points_lots` VALUES (81, 43, 27, 27, 'order_completed', 83, '2027-01-03 00:43:21', '2026-01-03 00:43:21');
INSERT INTO `points_lots` VALUES (82, 43, 2, 2, 'signin', 26, '2027-01-03 21:46:57', '2026-01-03 21:46:57');
INSERT INTO `points_lots` VALUES (83, 43, 2, 2, 'signin', 28, '2027-01-03 22:02:19', '2026-01-03 22:02:19');
INSERT INTO `points_lots` VALUES (84, 43, 2, 2, 'signin', 29, '2027-01-03 22:05:05', '2026-01-03 22:05:05');
INSERT INTO `points_lots` VALUES (85, 43, 2, 2, 'signin', 30, '2027-01-03 22:05:48', '2026-01-03 22:05:48');
INSERT INTO `points_lots` VALUES (86, 43, 2, 2, 'signin', 31, '2027-01-03 22:06:52', '2026-01-03 22:06:52');
INSERT INTO `points_lots` VALUES (87, 43, 2, 2, 'signin', 32, '2027-01-03 22:10:16', '2026-01-03 22:10:16');
INSERT INTO `points_lots` VALUES (88, 43, 2, 2, 'signin', 33, '2027-01-03 22:16:03', '2026-01-03 22:16:03');
INSERT INTO `points_lots` VALUES (89, 43, 2, 2, 'signin', 34, '2027-01-03 22:16:32', '2026-01-03 22:16:32');
INSERT INTO `points_lots` VALUES (90, 43, 2, 2, 'signin', 35, '2027-01-04 00:20:04', '2026-01-04 00:20:04');
INSERT INTO `points_lots` VALUES (91, 41, 2, 2, 'signin', 36, '2027-01-04 17:00:03', '2026-01-04 17:00:03');
INSERT INTO `points_lots` VALUES (92, 41, 51, 51, 'order_completed', 88, '2027-01-05 00:28:17', '2026-01-05 00:28:17');
INSERT INTO `points_lots` VALUES (93, 43, 2, 2, 'signin', 37, '2027-01-05 00:28:23', '2026-01-05 00:28:23');
INSERT INTO `points_lots` VALUES (94, 41, 2, 2, 'signin', 38, '2027-01-05 01:27:00', '2026-01-05 01:27:00');
INSERT INTO `points_lots` VALUES (95, 43, 2, 2, 'signin', 39, '2027-01-07 22:38:44', '2026-01-07 22:38:44');
INSERT INTO `points_lots` VALUES (96, 43, 100, 100, 'birthday_gift', 1918016256, '2027-01-07 23:22:20', '2026-01-07 23:22:20');
INSERT INTO `points_lots` VALUES (97, 43, 100, 100, 'birthday_gift', 1918016256, '2027-01-08 00:19:43', '2026-01-08 00:19:43');
INSERT INTO `points_lots` VALUES (98, 43, 2, 2, 'signin', 40, '2027-01-10 14:50:28', '2026-01-10 14:50:28');
INSERT INTO `points_lots` VALUES (99, 43, 2, 2, 'signin', 42, '2027-01-10 14:52:05', '2026-01-10 14:52:05');
INSERT INTO `points_lots` VALUES (100, 43, 2, 2, 'signin', 43, '2027-01-10 15:00:40', '2026-01-10 15:00:40');
INSERT INTO `points_lots` VALUES (101, 43, 57, 57, 'order_completed', 100, '2027-01-10 15:09:40', '2026-01-10 15:09:40');
INSERT INTO `points_lots` VALUES (102, 43, 33, 33, 'order_completed', 102, '2027-01-10 15:49:10', '2026-01-10 15:49:10');
INSERT INTO `points_lots` VALUES (103, 43, 42, 42, 'order_completed', 104, '2027-01-10 15:57:55', '2026-01-10 15:57:55');
INSERT INTO `points_lots` VALUES (104, 43, 33, 33, 'order_completed', 105, '2027-01-10 16:00:42', '2026-01-10 16:00:42');
INSERT INTO `points_lots` VALUES (105, 44, 200, 200, 'register', 44, '2027-01-10 17:32:29', '2026-01-10 17:32:29');
INSERT INTO `points_lots` VALUES (106, 45, 200, 200, 'register', 45, '2027-01-10 17:59:09', '2026-01-10 17:59:09');
INSERT INTO `points_lots` VALUES (107, 46, 200, 200, 'register', 46, '2027-01-10 18:33:23', '2026-01-10 18:33:23');
INSERT INTO `points_lots` VALUES (108, 47, 200, 200, 'register', 47, '2027-01-10 19:07:24', '2026-01-10 19:07:24');
INSERT INTO `points_lots` VALUES (109, 48, 200, 200, 'register', 48, '2027-01-10 19:19:25', '2026-01-10 19:19:25');
INSERT INTO `points_lots` VALUES (110, 49, 200, 200, 'register', 49, '2027-01-10 19:24:05', '2026-01-10 19:24:05');
INSERT INTO `points_lots` VALUES (111, 50, 200, 200, 'register', 50, '2027-01-10 22:18:40', '2026-01-10 22:18:40');
INSERT INTO `points_lots` VALUES (112, 50, 19, 19, 'order_completed', 107, '2027-01-10 22:31:34', '2026-01-10 22:31:34');
INSERT INTO `points_lots` VALUES (113, 50, 300, 300, 'first_order_bonus', 50, '2027-01-10 22:31:34', '2026-01-10 22:31:34');
INSERT INTO `points_lots` VALUES (114, 43, 33, 33, 'order_completed', 108, '2027-01-10 22:45:28', '2026-01-10 22:45:28');
INSERT INTO `points_lots` VALUES (115, 43, 40, 40, 'challenge_order', 108, '2027-01-10 22:45:29', '2026-01-10 22:45:29');
INSERT INTO `points_lots` VALUES (116, 43, 60, 60, 'challenge_morning', 108, '2027-01-10 22:45:29', '2026-01-10 22:45:29');
INSERT INTO `points_lots` VALUES (117, 43, 50, 50, 'challenge_delivery', 108, '2027-01-10 22:45:29', '2026-01-10 22:45:29');
INSERT INTO `points_lots` VALUES (118, 50, 22, 22, 'order_completed', 111, '2027-01-10 23:10:16', '2026-01-10 23:10:16');
INSERT INTO `points_lots` VALUES (119, 50, 50, 50, 'order_completed', 113, '2027-01-10 23:28:52', '2026-01-10 23:28:52');
INSERT INTO `points_lots` VALUES (120, 50, 40, 40, 'challenge_order', 113, '2027-01-10 23:28:52', '2026-01-10 23:28:52');
INSERT INTO `points_lots` VALUES (121, 50, 50, 50, 'challenge_delivery', 113, '2027-01-10 23:28:52', '2026-01-10 23:28:52');
INSERT INTO `points_lots` VALUES (122, 50, 80, 80, 'challenge_newproduct', 113, '2027-01-10 23:28:52', '2026-01-10 23:28:52');
INSERT INTO `points_lots` VALUES (123, 50, 36, 36, 'order_completed', 112, '2027-01-10 23:28:54', '2026-01-10 23:28:54');
INSERT INTO `points_lots` VALUES (124, 48, 28, 28, 'order_completed', 120, '2027-01-11 15:52:26', '2026-01-11 15:52:26');
INSERT INTO `points_lots` VALUES (125, 48, 40, 40, 'challenge_order', 0, '2027-01-11 16:20:07', '2026-01-11 16:20:07');
INSERT INTO `points_lots` VALUES (126, 48, 40, 40, 'challenge_order', 0, '2027-01-11 16:20:07', '2026-01-11 16:20:07');
INSERT INTO `points_lots` VALUES (127, 48, 40, 40, 'challenge_order', 0, '2027-01-11 16:20:07', '2026-01-11 16:20:07');
INSERT INTO `points_lots` VALUES (128, 48, 22, 22, 'order_completed', 127, '2027-01-11 16:28:43', '2026-01-11 16:28:43');
INSERT INTO `points_lots` VALUES (129, 48, 2, 2, 'signin', 44, '2027-01-11 16:28:50', '2026-01-11 16:28:50');
INSERT INTO `points_lots` VALUES (130, 49, 2, 2, 'signin', 45, '2027-01-11 16:33:14', '2026-01-11 16:33:14');
INSERT INTO `points_lots` VALUES (131, 49, 48, 48, 'order_completed', 130, '2027-01-11 16:57:33', '2026-01-11 16:57:33');
INSERT INTO `points_lots` VALUES (132, 51, 200, 200, 'register', 51, '2027-01-11 18:07:11', '2026-01-11 18:07:11');
INSERT INTO `points_lots` VALUES (133, 43, 29, 29, 'order_completed', 137, '2027-01-12 10:22:18', '2026-01-12 10:22:18');
INSERT INTO `points_lots` VALUES (134, 43, 12, 12, 'order_completed', 140, '2027-01-12 10:33:34', '2026-01-12 10:33:34');
INSERT INTO `points_lots` VALUES (135, 52, 40, 40, 'challenge_order', 0, '2027-01-12 11:31:27', '2026-01-12 11:31:27');
INSERT INTO `points_lots` VALUES (136, 52, 40, 40, 'challenge_order', 0, '2027-01-12 11:31:27', '2026-01-12 11:31:27');
INSERT INTO `points_lots` VALUES (137, 52, 40, 40, 'challenge_order', 0, '2027-01-12 11:31:27', '2026-01-12 11:31:27');
INSERT INTO `points_lots` VALUES (138, 52, 14, 14, 'order_completed', 149, '2027-01-12 12:02:32', '2026-01-12 12:02:32');
INSERT INTO `points_lots` VALUES (139, 52, 200, 200, 'first_order_bonus', 149, '2027-01-12 12:02:32', '2026-01-12 12:02:32');
INSERT INTO `points_lots` VALUES (140, 52, 38, 38, 'order_completed', 150, '2027-01-12 12:02:51', '2026-01-12 12:02:51');
INSERT INTO `points_lots` VALUES (141, 41, 40, 40, 'challenge_order', 0, '2027-01-12 15:17:36', '2026-01-12 15:17:36');
INSERT INTO `points_lots` VALUES (142, 55, 11, 11, 'order_completed', 151, '2027-01-12 15:18:02', '2026-01-12 15:18:02');
INSERT INTO `points_lots` VALUES (143, 55, 200, 200, 'first_order_bonus', 151, '2027-01-12 15:18:02', '2026-01-12 15:18:02');
INSERT INTO `points_lots` VALUES (144, 55, 22, 22, 'order_completed', 152, '2027-01-12 15:21:10', '2026-01-12 15:21:10');
INSERT INTO `points_lots` VALUES (145, 55, 22, 22, 'order_completed', 153, '2027-01-12 15:32:48', '2026-01-12 15:32:48');
INSERT INTO `points_lots` VALUES (146, 54, 22, 22, 'order_completed', 154, '2027-01-12 15:37:05', '2026-01-12 15:37:05');
INSERT INTO `points_lots` VALUES (147, 54, 200, 200, 'first_order_bonus', 154, '2027-01-12 15:37:05', '2026-01-12 15:37:05');
INSERT INTO `points_lots` VALUES (148, 56, 28, 28, 'order_completed', 155, '2027-01-12 15:45:07', '2026-01-12 15:45:07');
INSERT INTO `points_lots` VALUES (149, 56, 200, 200, 'first_order_bonus', 155, '2027-01-12 15:45:07', '2026-01-12 15:45:07');
INSERT INTO `points_lots` VALUES (150, 56, 24, 24, 'order_completed', 166, '2027-01-12 17:13:38', '2026-01-12 17:13:38');
INSERT INTO `points_lots` VALUES (151, 56, 50, 50, 'upgrade_reward', 892793789, '2027-01-12 17:13:38', '2026-01-12 17:13:38');
INSERT INTO `points_lots` VALUES (152, 57, 79, 79, 'order_completed', 187, '2027-01-12 17:55:54', '2026-01-12 17:55:54');
INSERT INTO `points_lots` VALUES (153, 57, 100, 100, 'upgrade_reward', 749638800, '2027-01-12 17:55:54', '2026-01-12 17:55:54');
INSERT INTO `points_lots` VALUES (154, 57, 200, 200, 'first_order_bonus', 187, '2027-01-12 17:55:54', '2026-01-12 17:55:54');
INSERT INTO `points_lots` VALUES (155, 59, 13, 13, 'order_completed', 199, '2027-01-12 21:04:31', '2026-01-12 21:04:31');
INSERT INTO `points_lots` VALUES (156, 59, 200, 200, 'first_order_bonus', 199, '2027-01-12 21:04:31', '2026-01-12 21:04:31');
INSERT INTO `points_lots` VALUES (157, 59, 46, 46, 'order_completed', 198, '2027-01-12 21:04:33', '2026-01-12 21:04:33');
INSERT INTO `points_lots` VALUES (158, 56, 59, 59, 'order_completed', 201, '2027-01-12 21:41:29', '2026-01-12 21:41:29');
INSERT INTO `points_lots` VALUES (159, 56, 100, 100, 'upgrade_reward', 749638801, '2027-01-12 21:41:29', '2026-01-12 21:41:29');
INSERT INTO `points_lots` VALUES (160, 60, 26, 26, 'order_completed', 202, '2027-01-12 21:47:32', '2026-01-12 21:47:32');
INSERT INTO `points_lots` VALUES (161, 60, 100, 100, 'upgrade_reward', 749638776, '2027-01-12 21:47:32', '2026-01-12 21:47:32');
INSERT INTO `points_lots` VALUES (162, 60, 200, 200, 'first_order_bonus', 202, '2027-01-12 21:47:32', '2026-01-12 21:47:32');
INSERT INTO `points_lots` VALUES (163, 46, 26, 26, 'order_completed', 203, '2027-01-12 21:52:03', '2026-01-12 21:52:03');
INSERT INTO `points_lots` VALUES (164, 46, 100, 100, 'upgrade_reward', 749638832, '2027-01-12 21:52:03', '2026-01-12 21:52:03');
INSERT INTO `points_lots` VALUES (165, 46, 200, 200, 'first_order_bonus', 203, '2027-01-12 21:52:03', '2026-01-12 21:52:03');
INSERT INTO `points_lots` VALUES (166, 61, 13, 13, 'order_completed', 204, '2027-01-12 22:06:04', '2026-01-12 22:06:04');
INSERT INTO `points_lots` VALUES (167, 61, 100, 100, 'upgrade_reward', 749638775, '2027-01-12 22:06:04', '2026-01-12 22:06:04');
INSERT INTO `points_lots` VALUES (168, 61, 200, 200, 'first_order_bonus', 204, '2027-01-12 22:06:04', '2026-01-12 22:06:04');
INSERT INTO `points_lots` VALUES (169, 62, 13, 13, 'order_completed', 206, '2027-01-12 22:32:03', '2026-01-12 22:32:03');
INSERT INTO `points_lots` VALUES (170, 62, 100, 100, 'upgrade_reward', 749638774, '2027-01-12 22:32:04', '2026-01-12 22:32:04');
INSERT INTO `points_lots` VALUES (171, 62, 200, 200, 'first_order_bonus', 206, '2027-01-12 22:32:04', '2026-01-12 22:32:04');
INSERT INTO `points_lots` VALUES (172, 62, 2, 2, 'signin', 46, '2027-01-12 23:02:41', '2026-01-12 23:02:41');
INSERT INTO `points_lots` VALUES (173, 62, 43, 43, 'order_completed', 207, '2027-01-12 23:04:18', '2026-01-12 23:04:18');
INSERT INTO `points_lots` VALUES (174, 62, 29, 29, 'order_completed', 208, '2027-01-13 13:53:42', '2026-01-13 13:53:42');
INSERT INTO `points_lots` VALUES (175, 63, 29, 29, 'order_completed', 209, '2027-01-13 14:10:59', '2026-01-13 14:10:59');
INSERT INTO `points_lots` VALUES (176, 63, 200, 200, 'first_order_bonus', 209, '2027-01-13 14:11:00', '2026-01-13 14:11:00');
INSERT INTO `points_lots` VALUES (177, 64, 57, 57, 'order_completed', 210, '2027-01-13 15:51:29', '2026-01-13 15:51:29');
INSERT INTO `points_lots` VALUES (178, 64, 200, 200, 'first_order_bonus', 210, '2027-01-13 15:51:30', '2026-01-13 15:51:30');
INSERT INTO `points_lots` VALUES (179, 64, 888, 888, 'birthday_gift', 573314527, '2027-01-13 15:53:19', '2026-01-13 15:53:19');
INSERT INTO `points_lots` VALUES (180, 65, 17, 17, 'order_completed', 211, '2027-01-13 16:43:22', '2026-01-13 16:43:22');
INSERT INTO `points_lots` VALUES (181, 65, 100, 100, 'upgrade_reward', 749638771, '2027-01-13 16:43:22', '2026-01-13 16:43:22');
INSERT INTO `points_lots` VALUES (182, 65, 200, 200, 'first_order_bonus', 211, '2027-01-13 16:43:22', '2026-01-13 16:43:22');
INSERT INTO `points_lots` VALUES (183, 66, 18, 18, 'order_completed', 212, '2027-01-13 17:30:59', '2026-01-13 17:30:59');
INSERT INTO `points_lots` VALUES (184, 66, 200, 200, 'first_order_bonus', 212, '2027-01-13 17:30:59', '2026-01-13 17:30:59');
INSERT INTO `points_lots` VALUES (185, 66, 888, 888, 'birthday_gift', 516056225, '2027-01-13 18:06:30', '2026-01-13 18:06:30');
INSERT INTO `points_lots` VALUES (186, 67, 2, 2, 'signin', 47, '2027-01-13 18:17:33', '2026-01-13 18:17:33');
INSERT INTO `points_lots` VALUES (187, 67, 29, 29, 'order_completed', 213, '2027-01-13 18:18:10', '2026-01-13 18:18:10');
INSERT INTO `points_lots` VALUES (188, 67, 1688, 1688, 'upgrade_reward', 1195270716, '2027-01-13 18:18:10', '2026-01-13 18:18:10');
INSERT INTO `points_lots` VALUES (189, 67, 200, 200, 'first_order_bonus', 213, '2027-01-13 18:18:10', '2026-01-13 18:18:10');
INSERT INTO `points_lots` VALUES (190, 67, 888, 888, 'birthday_gift', 487427074, '2027-01-13 18:18:57', '2026-01-13 18:18:57');
INSERT INTO `points_lots` VALUES (191, 68, 47, 0, 'order_completed', 215, '2027-01-13 18:33:52', '2026-01-13 18:33:52');
INSERT INTO `points_lots` VALUES (192, 68, 1688, 0, 'upgrade_reward', 1195270715, '2027-01-13 18:33:52', '2026-01-13 18:33:52');
INSERT INTO `points_lots` VALUES (193, 68, 200, 0, 'first_order_bonus', 215, '2027-01-13 18:33:52', '2026-01-13 18:33:52');
INSERT INTO `points_lots` VALUES (194, 68, 888, 221, 'birthday_gift', 458797923, '2027-01-13 18:34:35', '2026-01-13 18:34:35');
INSERT INTO `points_lots` VALUES (195, 68, 2, 2, 'signin', 48, '2027-01-14 19:15:07', '2026-01-14 19:15:07');
INSERT INTO `points_lots` VALUES (196, 68, 2, 2, 'signin', 49, '2027-01-15 16:34:09', '2026-01-15 16:34:09');
INSERT INTO `points_lots` VALUES (197, 69, 1000, 0, 'admin', NULL, '2027-01-15 17:35:50', '2026-01-15 17:35:50');
INSERT INTO `points_lots` VALUES (198, 69, 1000, 0, 'admin', NULL, '2027-01-15 17:56:01', '2026-01-15 17:56:01');
INSERT INTO `points_lots` VALUES (199, 69, 9999, 5232, 'admin', NULL, '2027-01-15 18:04:29', '2026-01-15 18:04:29');
INSERT INTO `points_lots` VALUES (200, 53, 23, 23, 'order_completed', 234, '2027-01-15 19:38:25', '2026-01-15 19:38:25');
INSERT INTO `points_lots` VALUES (201, 53, 200, 200, 'first_order_bonus', 234, '2027-01-15 19:38:25', '2026-01-15 19:38:25');
INSERT INTO `points_lots` VALUES (202, 69, 2, 2, 'signin', 50, '2027-01-15 19:39:22', '2026-01-15 19:39:22');
INSERT INTO `points_lots` VALUES (203, 69, 2, 2, 'signin', 52, '2027-01-15 19:40:04', '2026-01-15 19:40:04');
INSERT INTO `points_lots` VALUES (204, 70, 22, 22, 'order_completed', 236, '2027-01-15 19:59:53', '2026-01-15 19:59:53');
INSERT INTO `points_lots` VALUES (205, 70, 200, 200, 'first_order_bonus', 236, '2027-01-15 19:59:53', '2026-01-15 19:59:53');
INSERT INTO `points_lots` VALUES (206, 71, 66, 66, 'order_completed', 237, '2027-01-15 20:08:08', '2026-01-15 20:08:08');
INSERT INTO `points_lots` VALUES (207, 71, 50, 50, 'upgrade_reward', 892793846, '2027-01-15 20:08:08', '2026-01-15 20:08:08');
INSERT INTO `points_lots` VALUES (208, 71, 200, 200, 'first_order_bonus', 237, '2027-01-15 20:08:08', '2026-01-15 20:08:08');
INSERT INTO `points_lots` VALUES (209, 72, 26, 26, 'order_completed', 243, '2027-01-15 20:50:49', '2026-01-15 20:50:49');
INSERT INTO `points_lots` VALUES (210, 72, 100, 100, 'upgrade_reward', 749638743, '2027-01-15 20:50:49', '2026-01-15 20:50:49');
INSERT INTO `points_lots` VALUES (211, 72, 200, 200, 'first_order_bonus', 243, '2027-01-15 20:50:49', '2026-01-15 20:50:49');
INSERT INTO `points_lots` VALUES (212, 73, 13, 13, 'order_completed', 244, '2027-01-15 21:11:59', '2026-01-15 21:11:59');
INSERT INTO `points_lots` VALUES (213, 73, 100, 100, 'upgrade_reward', 749638742, '2027-01-15 21:11:59', '2026-01-15 21:11:59');
INSERT INTO `points_lots` VALUES (214, 73, 200, 200, 'first_order_bonus', 244, '2027-01-15 21:11:59', '2026-01-15 21:11:59');
INSERT INTO `points_lots` VALUES (215, 73, 2, 2, 'signin', 53, '2027-01-15 22:43:25', '2026-01-15 22:43:25');
INSERT INTO `points_lots` VALUES (216, 73, 37, 37, 'order_completed', 253, '2027-01-15 22:44:15', '2026-01-15 22:44:15');
INSERT INTO `points_lots` VALUES (217, 73, 1688, 1688, 'upgrade_reward', 1195270689, '2027-01-15 22:44:15', '2026-01-15 22:44:15');
INSERT INTO `points_lots` VALUES (218, 74, 200, 200, 'first_order_bonus', 258, '2027-01-15 22:52:09', '2026-01-15 22:52:09');
INSERT INTO `points_lots` VALUES (219, 74, 48, 48, 'order_completed', 257, '2027-01-15 22:52:13', '2026-01-15 22:52:13');
INSERT INTO `points_lots` VALUES (220, 74, 83, 83, 'order_completed', 256, '2027-01-15 22:52:16', '2026-01-15 22:52:16');
INSERT INTO `points_lots` VALUES (221, 74, 15, 15, 'order_completed', 255, '2027-01-15 22:52:18', '2026-01-15 22:52:18');
INSERT INTO `points_lots` VALUES (222, 74, 40, 40, 'challenge_order', 0, '2027-01-15 22:52:18', '2026-01-15 22:52:18');
INSERT INTO `points_lots` VALUES (223, 74, 40, 40, 'challenge_order', 0, '2027-01-15 22:52:18', '2026-01-15 22:52:18');
INSERT INTO `points_lots` VALUES (224, 74, 24, 24, 'order_completed', 259, '2027-01-15 22:53:16', '2026-01-15 22:53:16');
INSERT INTO `points_lots` VALUES (225, 74, 48, 48, 'order_completed', 260, '2027-01-15 22:53:43', '2026-01-15 22:53:43');
INSERT INTO `points_lots` VALUES (226, 74, 50, 50, 'challenge_delivery', 0, '2027-01-15 22:53:44', '2026-01-15 22:53:44');
INSERT INTO `points_lots` VALUES (227, 74, 50, 50, 'challenge_delivery', 0, '2027-01-15 22:53:44', '2026-01-15 22:53:44');
INSERT INTO `points_lots` VALUES (228, 70, 20, 20, 'order_completed', 263, '2027-01-15 23:03:51', '2026-01-15 23:03:51');
INSERT INTO `points_lots` VALUES (229, 70, 19, 19, 'order_completed', 262, '2027-01-15 23:03:54', '2026-01-15 23:03:54');
INSERT INTO `points_lots` VALUES (230, 70, 23, 23, 'order_completed', 261, '2027-01-15 23:04:34', '2026-01-15 23:04:34');
INSERT INTO `points_lots` VALUES (231, 70, 50, 50, 'challenge_delivery', 261, '2027-01-15 23:04:34', '2026-01-15 23:04:34');
INSERT INTO `points_lots` VALUES (232, 70, 40, 40, 'order_completed', 266, '2027-01-15 23:06:15', '2026-01-15 23:06:15');
INSERT INTO `points_lots` VALUES (233, 70, 40, 40, 'challenge_order', 266, '2027-01-15 23:06:15', '2026-01-15 23:06:15');
INSERT INTO `points_lots` VALUES (234, 70, 40, 40, 'order_completed', 265, '2027-01-15 23:06:18', '2026-01-15 23:06:18');
INSERT INTO `points_lots` VALUES (235, 70, 42, 42, 'order_completed', 264, '2027-01-15 23:06:23', '2026-01-15 23:06:23');
INSERT INTO `points_lots` VALUES (236, 75, 888, 888, 'birthday_gift', 342818305, '2027-01-15 23:11:55', '2026-01-15 23:11:55');
INSERT INTO `points_lots` VALUES (237, 75, 15, 15, 'order_completed', 267, '2027-01-15 23:15:37', '2026-01-15 23:15:37');
INSERT INTO `points_lots` VALUES (238, 75, 200, 200, 'first_order_bonus', 267, '2027-01-15 23:15:37', '2026-01-15 23:15:37');
INSERT INTO `points_lots` VALUES (239, 75, 27, 27, 'order_completed', 268, '2027-01-15 23:15:40', '2026-01-15 23:15:40');
INSERT INTO `points_lots` VALUES (240, 75, 61, 61, 'order_completed', 269, '2027-01-15 23:15:42', '2026-01-15 23:15:42');
INSERT INTO `points_lots` VALUES (241, 75, 50, 50, 'challenge_delivery', 269, '2027-01-15 23:15:42', '2026-01-15 23:15:42');
INSERT INTO `points_lots` VALUES (242, 75, 61, 61, 'order_completed', 270, '2027-01-15 23:15:44', '2026-01-15 23:15:44');
INSERT INTO `points_lots` VALUES (243, 75, 61, 61, 'order_completed', 271, '2027-01-15 23:16:30', '2026-01-15 23:16:30');
INSERT INTO `points_lots` VALUES (244, 75, 40, 40, 'challenge_order', 271, '2027-01-15 23:16:30', '2026-01-15 23:16:30');
INSERT INTO `points_lots` VALUES (245, 76, 38, 38, 'order_completed', 272, '2027-01-15 23:21:59', '2026-01-15 23:21:59');
INSERT INTO `points_lots` VALUES (246, 76, 200, 200, 'first_order_bonus', 272, '2027-01-15 23:22:00', '2026-01-15 23:22:00');
INSERT INTO `points_lots` VALUES (247, 76, 39, 39, 'order_completed', 273, '2027-01-15 23:22:55', '2026-01-15 23:22:55');
INSERT INTO `points_lots` VALUES (248, 76, 36, 36, 'order_completed', 274, '2027-01-15 23:23:12', '2026-01-15 23:23:12');
INSERT INTO `points_lots` VALUES (249, 76, 31, 31, 'order_completed', 275, '2027-01-15 23:24:12', '2026-01-15 23:24:12');
INSERT INTO `points_lots` VALUES (250, 76, 80, 80, 'challenge_newproduct', 275, '2027-01-15 23:24:12', '2026-01-15 23:24:12');
INSERT INTO `points_lots` VALUES (251, 46, 47, 47, 'order_completed', 278, '2027-01-15 23:29:45', '2026-01-15 23:29:45');
INSERT INTO `points_lots` VALUES (252, 46, 49, 49, 'order_completed', 277, '2027-01-15 23:29:50', '2026-01-15 23:29:50');
INSERT INTO `points_lots` VALUES (253, 46, 47, 47, 'order_completed', 279, '2027-01-15 23:30:50', '2026-01-15 23:30:50');
INSERT INTO `points_lots` VALUES (254, 46, 40, 40, 'challenge_order', 279, '2027-01-15 23:30:50', '2026-01-15 23:30:50');
INSERT INTO `points_lots` VALUES (255, 47, 40, 40, 'order_completed', 281, '2027-01-15 23:35:56', '2026-01-15 23:35:56');
INSERT INTO `points_lots` VALUES (256, 47, 200, 200, 'first_order_bonus', 281, '2027-01-15 23:35:56', '2026-01-15 23:35:56');
INSERT INTO `points_lots` VALUES (257, 47, 34, 34, 'order_completed', 282, '2027-01-15 23:35:58', '2026-01-15 23:35:58');
INSERT INTO `points_lots` VALUES (258, 47, 38, 38, 'order_completed', 280, '2027-01-15 23:36:00', '2026-01-15 23:36:00');
INSERT INTO `points_lots` VALUES (259, 47, 26, 26, 'order_completed', 283, '2027-01-15 23:36:17', '2026-01-15 23:36:17');
INSERT INTO `points_lots` VALUES (260, 47, 40, 40, 'challenge_order', 283, '2027-01-15 23:36:17', '2026-01-15 23:36:17');
INSERT INTO `points_lots` VALUES (261, 47, 46, 46, 'order_completed', 284, '2027-01-15 23:36:36', '2026-01-15 23:36:36');
INSERT INTO `points_lots` VALUES (262, 47, 37, 37, 'order_completed', 285, '2027-01-15 23:37:27', '2026-01-15 23:37:27');
INSERT INTO `points_lots` VALUES (263, 47, 80, 80, 'challenge_newproduct', 285, '2027-01-15 23:37:27', '2026-01-15 23:37:27');
INSERT INTO `points_lots` VALUES (264, 47, 37, 37, 'order_completed', 286, '2027-01-15 23:37:42', '2026-01-15 23:37:42');
INSERT INTO `points_lots` VALUES (265, 49, 19, 19, 'order_completed', 287, '2027-01-15 23:42:01', '2026-01-15 23:42:01');
INSERT INTO `points_lots` VALUES (266, 49, 50, 50, 'challenge_delivery', 287, '2027-01-15 23:42:01', '2026-01-15 23:42:01');
INSERT INTO `points_lots` VALUES (267, 49, 40, 40, 'order_completed', 289, '2027-01-15 23:42:27', '2026-01-15 23:42:27');
INSERT INTO `points_lots` VALUES (268, 49, 42, 42, 'order_completed', 288, '2027-01-15 23:42:33', '2026-01-15 23:42:33');
INSERT INTO `points_lots` VALUES (269, 49, 40, 40, 'challenge_order', 288, '2027-01-15 23:42:33', '2026-01-15 23:42:33');
INSERT INTO `points_lots` VALUES (270, 49, 40, 40, 'order_completed', 290, '2027-01-15 23:43:02', '2026-01-15 23:43:02');
INSERT INTO `points_lots` VALUES (271, 49, 80, 80, 'challenge_newproduct', 290, '2027-01-15 23:43:02', '2026-01-15 23:43:02');
INSERT INTO `points_lots` VALUES (272, 77, 9999, 4399, 'admin', NULL, '2027-01-15 23:44:07', '2026-01-15 23:44:07');
INSERT INTO `points_lots` VALUES (273, 47, 51, 51, 'order_completed', 291, '2027-01-16 08:16:49', '2026-01-16 08:16:49');
INSERT INTO `points_lots` VALUES (274, 47, 50, 50, 'challenge_delivery', 291, '2027-01-16 08:16:49', '2026-01-16 08:16:49');
INSERT INTO `points_lots` VALUES (275, 47, 48, 48, 'order_completed', 292, '2027-01-16 08:17:04', '2026-01-16 08:17:04');
INSERT INTO `points_lots` VALUES (276, 47, 85, 85, 'order_completed', 293, '2027-01-16 08:17:18', '2026-01-16 08:17:18');
INSERT INTO `points_lots` VALUES (277, 47, 60, 60, 'challenge_morning', 293, '2027-01-16 08:17:19', '2026-01-16 08:17:19');
INSERT INTO `points_lots` VALUES (278, 79, 2, 0, 'signin', 54, '2027-01-16 09:03:36', '2026-01-16 09:03:36');
INSERT INTO `points_lots` VALUES (279, 78, 18, 18, 'order_completed', 294, '2027-01-16 09:05:16', '2026-01-16 09:05:16');
INSERT INTO `points_lots` VALUES (280, 78, 50, 50, 'upgrade_reward', 892793853, '2027-01-16 09:05:17', '2026-01-16 09:05:17');
INSERT INTO `points_lots` VALUES (281, 78, 200, 200, 'first_order_bonus', 294, '2027-01-16 09:05:17', '2026-01-16 09:05:17');
INSERT INTO `points_lots` VALUES (282, 79, 1000, 822, 'admin', NULL, '2027-01-16 09:19:33', '2026-01-16 09:19:33');
INSERT INTO `points_lots` VALUES (283, 79, 61, 61, 'order_completed', 297, '2027-01-19 22:45:47', '2026-01-19 22:45:47');
INSERT INTO `points_lots` VALUES (284, 79, 1688, 1688, 'upgrade_reward', 1195270683, '2027-01-19 22:45:48', '2026-01-19 22:45:48');
INSERT INTO `points_lots` VALUES (285, 79, 200, 200, 'first_order_bonus', 297, '2027-01-19 22:45:48', '2026-01-19 22:45:48');
INSERT INTO `points_lots` VALUES (286, 79, 37, 37, 'order_completed', 296, '2027-01-19 22:45:49', '2026-01-19 22:45:49');
INSERT INTO `points_lots` VALUES (287, 79, 37, 37, 'order_completed', 299, '2027-01-19 22:49:34', '2026-01-19 22:49:34');
INSERT INTO `points_lots` VALUES (288, 19, 22, 22, 'order_completed', 303, '2027-07-07 14:31:33', '2026-07-07 14:31:33');
INSERT INTO `points_lots` VALUES (289, 19, 32, 32, 'order_completed', 304, '2027-07-07 14:35:12', '2026-07-07 14:35:12');
INSERT INTO `points_lots` VALUES (290, 19, 22, 22, 'order_completed', 305, '2027-07-08 13:28:08', '2026-07-08 13:28:08');
INSERT INTO `points_lots` VALUES (291, 19, 40, 40, 'challenge_order', 305, '2027-07-08 13:28:08', '2026-07-08 13:28:08');
INSERT INTO `points_lots` VALUES (292, 19, 30, 30, 'order_completed', 306, '2027-07-08 13:39:43', '2026-07-08 13:39:43');
INSERT INTO `points_lots` VALUES (293, 19, 31, 31, 'order_completed', 307, '2027-07-08 13:44:51', '2026-07-08 13:44:51');
INSERT INTO `points_lots` VALUES (294, 19, 50, 50, 'challenge_delivery', 307, '2027-07-08 13:44:51', '2026-07-08 13:44:51');
INSERT INTO `points_lots` VALUES (295, 19, 25, 25, 'order_completed', 308, '2027-07-08 13:47:03', '2026-07-08 13:47:03');

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
-- Records of points_transactions
-- ----------------------------
INSERT INTO `points_transactions` VALUES (3, 19, -1200, 998800, 'redeem', NULL, '兑换商品: 精选咖啡豆 200g x1', '2025-12-21 17:47:22');
INSERT INTO `points_transactions` VALUES (4, 19, -1200, 997600, 'redeem', NULL, '兑换商品: 精选咖啡豆 200g x1', '2025-12-21 17:48:09');
INSERT INTO `points_transactions` VALUES (5, 19, -800, 996800, 'redeem', NULL, '兑换商品: 限定帆布袋 x1', '2025-12-21 17:48:29');
INSERT INTO `points_transactions` VALUES (6, 19, 800, 997600, 'refund', NULL, '订单取消退还: 限定帆布袋 x1', '2025-12-21 18:04:10');
INSERT INTO `points_transactions` VALUES (7, 19, 1200, 998800, 'refund', NULL, '订单取消退还: 精选咖啡豆 200g x1', '2025-12-21 18:04:22');
INSERT INTO `points_transactions` VALUES (8, 19, 1200, 1000000, 'refund', NULL, '订单取消退还: 精选咖啡豆 200g x1', '2025-12-21 18:10:09');
INSERT INTO `points_transactions` VALUES (9, 20, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-21 20:51:36');
INSERT INTO `points_transactions` VALUES (10, 20, 25, 135, 'profile', NULL, '完善手机号奖励', '2025-12-21 20:53:24');
INSERT INTO `points_transactions` VALUES (11, 20, 50, 185, 'consume', NULL, '消费50元获得积分（1倍加成）', '2025-12-21 20:55:48');
INSERT INTO `points_transactions` VALUES (12, 20, 1000, 1185, 'consume', NULL, '消费1000元获得积分（1倍加成）', '2025-12-21 20:58:58');
INSERT INTO `points_transactions` VALUES (13, 20, -950, 235, 'redeem', NULL, '兑换商品: Cozy 马克杯 x2', '2025-12-21 21:05:28');
INSERT INTO `points_transactions` VALUES (14, 21, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-21 21:17:24');
INSERT INTO `points_transactions` VALUES (15, 21, 10, 110, 'signin', NULL, '每日签到', '2025-12-21 21:18:29');
INSERT INTO `points_transactions` VALUES (16, 21, 50, 160, 'profile', NULL, '完善个人资料（手机号+邮箱）奖励', '2025-12-21 21:19:03');
INSERT INTO `points_transactions` VALUES (17, 21, 50, 210, 'consume', NULL, '消费50元获得积分（1倍加成）', '2025-12-21 21:19:52');
INSERT INTO `points_transactions` VALUES (18, 21, 1000, 1210, 'consume', NULL, '消费1000元获得积分（1倍加成）', '2025-12-21 21:23:14');
INSERT INTO `points_transactions` VALUES (19, 21, 1100, 2310, 'consume', NULL, '消费1000元获得积分（1.1倍加成）', '2025-12-21 21:23:42');
INSERT INTO `points_transactions` VALUES (20, 21, 1100, 3410, 'consume', NULL, '消费1000元获得积分（1.1倍加成）', '2025-12-21 21:23:46');
INSERT INTO `points_transactions` VALUES (21, 21, -1080, 2330, 'redeem', NULL, '兑换商品: 精选咖啡豆 200g x1', '2025-12-21 21:24:11');
INSERT INTO `points_transactions` VALUES (22, 21, 1080, 3410, 'refund', NULL, '订单取消退还: 精选咖啡豆 200g x1', '2025-12-21 21:24:15');
INSERT INTO `points_transactions` VALUES (23, 21, -450, 2960, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-21 21:24:37');
INSERT INTO `points_transactions` VALUES (24, 21, 450, 3410, 'refund', NULL, '订单取消退还: Cozy 马克杯 x1', '2025-12-21 21:24:45');
INSERT INTO `points_transactions` VALUES (25, 21, -270, 3140, 'redeem', NULL, '兑换商品: 美式咖啡兑换券 x1', '2025-12-21 21:25:22');
INSERT INTO `points_transactions` VALUES (26, 21, -270, 2870, 'redeem', NULL, '兑换商品: 美式咖啡兑换券 x1', '2025-12-21 21:25:37');
INSERT INTO `points_transactions` VALUES (27, 21, 270, 3140, 'refund', NULL, '订单取消退还: 美式咖啡兑换券 x1', '2025-12-21 21:26:05');
INSERT INTO `points_transactions` VALUES (28, 21, 270, 3410, 'refund', NULL, '订单取消退还: 美式咖啡兑换券 x1', '2025-12-21 21:26:25');
INSERT INTO `points_transactions` VALUES (29, 21, -450, 2960, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-21 21:26:53');
INSERT INTO `points_transactions` VALUES (30, 21, -450, 2510, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-21 21:27:16');
INSERT INTO `points_transactions` VALUES (31, 21, 450, 2960, 'refund', NULL, '订单取消退还: Cozy 马克杯 x1', '2025-12-21 21:30:31');
INSERT INTO `points_transactions` VALUES (32, 21, 450, 3410, 'refund', NULL, '订单取消退还: Cozy 马克杯 x1', '2025-12-21 21:30:33');
INSERT INTO `points_transactions` VALUES (33, 21, 60, 3470, 'consume', NULL, '消费50元获得积分（1.2倍加成）', '2025-12-21 21:30:50');
INSERT INTO `points_transactions` VALUES (34, 21, 60, 3530, 'consume', NULL, '消费50元获得积分（1.2倍加成）', '2025-12-21 21:31:01');
INSERT INTO `points_transactions` VALUES (35, 21, 60, 3590, 'consume', NULL, '消费50元获得积分（1.2倍加成）', '2025-12-21 21:31:03');
INSERT INTO `points_transactions` VALUES (36, 21, 1200, 4790, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 21:31:10');
INSERT INTO `points_transactions` VALUES (37, 21, 1200, 5990, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 21:31:12');
INSERT INTO `points_transactions` VALUES (38, 21, 1200, 7190, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 21:31:16');
INSERT INTO `points_transactions` VALUES (39, 21, 1200, 8390, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 21:31:18');
INSERT INTO `points_transactions` VALUES (40, 22, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-21 21:48:43');
INSERT INTO `points_transactions` VALUES (41, 22, 50, 150, 'profile', NULL, '完善个人资料（手机号+邮箱）奖励', '2025-12-21 21:49:10');
INSERT INTO `points_transactions` VALUES (42, 22, 1000, 1150, 'consume', NULL, '消费1000元获得积分（1倍加成）', '2025-12-21 21:49:19');
INSERT INTO `points_transactions` VALUES (43, 22, 15, 1165, 'signin', NULL, '每日签到', '2025-12-21 21:49:26');
INSERT INTO `points_transactions` VALUES (44, 22, 1100, 2265, 'consume', NULL, '消费1000元获得积分（1.1倍加成）', '2025-12-21 21:50:31');
INSERT INTO `points_transactions` VALUES (45, 22, 1100, 3365, 'consume', NULL, '消费1000元获得积分（1.1倍加成）', '2025-12-21 21:50:33');
INSERT INTO `points_transactions` VALUES (46, 22, -900, 2465, 'redeem', NULL, '兑换商品: Cozy 马克杯 x2', '2025-12-21 21:52:42');
INSERT INTO `points_transactions` VALUES (47, 22, 900, 3365, 'refund', NULL, '订单取消退还: Cozy 马克杯 x2', '2025-12-21 21:52:57');
INSERT INTO `points_transactions` VALUES (48, 22, 1200, 4565, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 22:09:05');
INSERT INTO `points_transactions` VALUES (49, 22, 1200, 5765, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 22:09:10');
INSERT INTO `points_transactions` VALUES (50, 22, 1200, 6965, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 22:09:14');
INSERT INTO `points_transactions` VALUES (51, 22, 1200, 8165, 'consume', NULL, '消费1000元获得积分（1.2倍加成）', '2025-12-21 22:09:17');
INSERT INTO `points_transactions` VALUES (52, 23, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-21 23:16:33');
INSERT INTO `points_transactions` VALUES (53, 23, 10, 110, 'signin', NULL, '每日签到', '2025-12-21 23:27:43');
INSERT INTO `points_transactions` VALUES (54, 22, 150, 8315, 'invite', NULL, '邀请好友奖励（用户ID:23）', '2025-12-21 23:43:29');
INSERT INTO `points_transactions` VALUES (55, 23, 80, 190, 'invited', NULL, '填写邀请码奖励（邀请人ID:22）', '2025-12-21 23:43:29');
INSERT INTO `points_transactions` VALUES (56, 23, 50, 240, 'profile', NULL, '完善个人资料（手机号+邮箱）奖励', '2025-12-21 23:46:54');
INSERT INTO `points_transactions` VALUES (57, 22, 150, 8465, 'invite', NULL, '邀请好友奖励（用户ID:19）', '2025-12-21 23:51:05');
INSERT INTO `points_transactions` VALUES (58, 19, 80, 1000080, 'invited', NULL, '填写邀请码奖励（邀请人ID:22）', '2025-12-21 23:51:05');
INSERT INTO `points_transactions` VALUES (59, 22, 150, 8615, 'invite', NULL, '邀请好友加入奖励', '2025-12-21 23:54:09');
INSERT INTO `points_transactions` VALUES (60, 21, 80, 8470, 'invited', NULL, '填写有效邀请码奖励', '2025-12-21 23:54:09');
INSERT INTO `points_transactions` VALUES (61, 21, 32, 8502, 'signin', NULL, '每日签到（连续2天）', '2025-12-22 00:02:46');
INSERT INTO `points_transactions` VALUES (62, 19, 32, 8032, 'signin', NULL, '每日签到（连续2天）', '2025-12-22 16:19:04');
INSERT INTO `points_transactions` VALUES (63, 19, 36, 8068, 'consume', NULL, '咖啡消费: 美式咖啡 x1', '2025-12-22 16:22:52');
INSERT INTO `points_transactions` VALUES (64, 19, 36, 8104, 'consume', NULL, '咖啡消费: 美式咖啡 x1', '2025-12-22 18:33:35');
INSERT INTO `points_transactions` VALUES (65, 19, 72, 8176, 'consume', NULL, '咖啡消费: 美式咖啡 x2', '2025-12-22 18:33:51');
INSERT INTO `points_transactions` VALUES (66, 19, 36, 8212, 'consume', NULL, '咖啡消费: 美式咖啡 x1', '2025-12-22 18:35:14');
INSERT INTO `points_transactions` VALUES (67, 19, -360, 7852, 'redeem', NULL, '兑换商品: 拿铁咖啡兑换券 x1', '2025-12-22 18:41:50');
INSERT INTO `points_transactions` VALUES (68, 19, 50, 7902, 'consume', NULL, '咖啡消费: 原味拿铁 x1', '2025-12-22 18:42:08');
INSERT INTO `points_transactions` VALUES (69, 19, 34, 7936, 'signin', NULL, '每日签到（连续3天）', '2025-12-23 10:46:00');
INSERT INTO `points_transactions` VALUES (70, 23, 18, 258, 'consume', NULL, '咖啡消费: 美式咖啡 x1', '2025-12-23 16:22:58');
INSERT INTO `points_transactions` VALUES (71, 23, 18, 276, 'consume', NULL, '咖啡消费: 美式咖啡 x1', '2025-12-23 16:23:27');
INSERT INTO `points_transactions` VALUES (72, 23, 25, 301, 'consume', NULL, '咖啡消费: 原味拿铁 x1', '2025-12-23 16:25:10');
INSERT INTO `points_transactions` VALUES (73, 23, 60, 361, 'consume', NULL, '咖啡消费: 卡布奇诺 x2', '2025-12-23 16:25:26');
INSERT INTO `points_transactions` VALUES (74, 23, 30, 391, 'consume', NULL, '咖啡消费: 卡布奇诺 x1', '2025-12-23 16:25:50');
INSERT INTO `points_transactions` VALUES (75, 23, 30, 421, 'consume', NULL, '咖啡消费: 卡布奇诺 x1', '2025-12-23 16:25:55');
INSERT INTO `points_transactions` VALUES (76, 23, 30, 451, 'consume', NULL, '咖啡消费: 卡布奇诺 x1', '2025-12-23 16:25:59');
INSERT INTO `points_transactions` VALUES (77, 23, 30, 481, 'consume', NULL, '咖啡消费: 卡布奇诺 x1', '2025-12-23 16:26:26');
INSERT INTO `points_transactions` VALUES (78, 23, 30, 511, 'consume', NULL, '咖啡消费: 卡布奇诺 x1', '2025-12-23 16:26:31');
INSERT INTO `points_transactions` VALUES (79, 23, 30, 541, 'consume', NULL, '咖啡消费: 卡布奇诺 x1', '2025-12-23 16:26:35');
INSERT INTO `points_transactions` VALUES (80, 24, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:36:36');
INSERT INTO `points_transactions` VALUES (81, 24, 2, 102, 'admin_adjust', NULL, '加', '2025-12-23 16:38:49');
INSERT INTO `points_transactions` VALUES (82, 25, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:41:52');
INSERT INTO `points_transactions` VALUES (83, 26, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:42:19');
INSERT INTO `points_transactions` VALUES (84, 27, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:42:27');
INSERT INTO `points_transactions` VALUES (85, 28, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:42:37');
INSERT INTO `points_transactions` VALUES (86, 29, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:42:47');
INSERT INTO `points_transactions` VALUES (87, 30, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:42:54');
INSERT INTO `points_transactions` VALUES (88, 31, 100, 100, 'register', NULL, '新用户注册奖励', '2025-12-23 16:43:13');
INSERT INTO `points_transactions` VALUES (89, 24, 1000, 1102, 'admin_adjust', NULL, 'test', '2025-12-23 16:58:26');
INSERT INTO `points_transactions` VALUES (90, 23, 30, 571, 'consume', NULL, '咖啡消费: 卡布奇诺 x1', '2025-12-23 16:59:27');
INSERT INTO `points_transactions` VALUES (91, 23, 18, 589, 'consume', NULL, '咖啡消费: 美式咖啡 x1', '2025-12-23 17:18:52');
INSERT INTO `points_transactions` VALUES (92, 23, 18, 607, 'consume', NULL, '咖啡消费: 美式咖啡 x1', '2025-12-23 17:19:15');
INSERT INTO `points_transactions` VALUES (93, 23, 50, 657, 'consume', NULL, '咖啡消费: 原味拿铁 x2', '2025-12-23 17:19:24');
INSERT INTO `points_transactions` VALUES (94, 23, -500, 157, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-23 17:32:11');
INSERT INTO `points_transactions` VALUES (95, 23, 1000, 1157, 'admin_adjust', NULL, 'jia', '2025-12-23 17:34:00');
INSERT INTO `points_transactions` VALUES (96, 23, -500, 657, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-23 17:34:42');
INSERT INTO `points_transactions` VALUES (97, 31, 10, 110, 'admin_adjust', NULL, '自动化测试', '2025-12-24 14:40:21');
INSERT INTO `points_transactions` VALUES (98, 31, -10, 100, 'admin_adjust', NULL, '自动化测试回滚', '2025-12-24 14:40:21');
INSERT INTO `points_transactions` VALUES (99, 23, 67, 724, 'consume', NULL, '咖啡消费: 生椰拿铁 x2', '2025-12-24 17:57:17');
INSERT INTO `points_transactions` VALUES (100, 23, -300, 424, 'redeem', NULL, '兑换商品: 美式咖啡兑换券 x1', '2025-12-24 18:03:21');
INSERT INTO `points_transactions` VALUES (101, 23, -300, 124, 'redeem', NULL, '兑换商品: 美式咖啡兑换券 x1', '2025-12-24 21:05:16');
INSERT INTO `points_transactions` VALUES (102, 24, 2000, 3102, 'admin_adjust', NULL, '测试黄金会员', '2025-12-25 17:38:58');
INSERT INTO `points_transactions` VALUES (103, 19, -270, 7666, 'redeem', NULL, '兑换商品: 美式咖啡兑换券 x1', '2025-12-25 17:40:58');
INSERT INTO `points_transactions` VALUES (104, 19, -270, 7396, 'redeem', NULL, '兑换商品: 美式咖啡兑换券 x1', '2025-12-25 17:48:39');
INSERT INTO `points_transactions` VALUES (105, 19, -450, 6946, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-25 17:54:36');
INSERT INTO `points_transactions` VALUES (106, 19, -450, 6496, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-25 18:08:30');
INSERT INTO `points_transactions` VALUES (107, 19, -450, 6046, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-25 18:12:56');
INSERT INTO `points_transactions` VALUES (108, 19, -270, 5776, 'redeem', NULL, '兑换商品: 美式咖啡兑换券 x1', '2025-12-25 18:51:10');
INSERT INTO `points_transactions` VALUES (109, 19, -450, 5326, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-25 19:03:15');
INSERT INTO `points_transactions` VALUES (110, 19, -450, 4876, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-25 19:09:24');
INSERT INTO `points_transactions` VALUES (111, 19, 450, 5326, 'refund', NULL, '订单取消退还: Cozy 马克杯 x1', '2025-12-25 19:09:41');
INSERT INTO `points_transactions` VALUES (112, 19, -450, 4876, 'redeem', NULL, '兑换商品: Cozy 马克杯 x1', '2025-12-25 19:10:07');
INSERT INTO `points_transactions` VALUES (113, 30, 1, 101, 'admin_adjust', NULL, '1', '2025-12-27 23:34:59');
INSERT INTO `points_transactions` VALUES (114, 22, 1, 8616, 'admin_adjust', NULL, '1', '2025-12-27 23:35:20');
INSERT INTO `points_transactions` VALUES (115, 39, 9999, 9999, 'admin_adjust', NULL, '测试用途', '2025-12-27 23:38:44');
INSERT INTO `points_transactions` VALUES (116, 39, 2, 10001, 'admin_adjust', NULL, '1', '2025-12-27 23:39:06');
INSERT INTO `points_transactions` VALUES (117, 39, -9999, 2, 'admin_adjust', NULL, '1', '2025-12-27 23:42:22');
INSERT INTO `points_transactions` VALUES (118, 39, 9999, 10001, 'admin_adjust', NULL, '测试用途', '2025-12-27 23:42:42');
INSERT INTO `points_transactions` VALUES (119, 39, 101, 10102, 'admin_adjust', NULL, '1', '2025-12-27 23:52:12');
INSERT INTO `points_transactions` VALUES (120, 39, -100, 10002, 'admin_adjust', NULL, '1', '2025-12-27 23:57:59');
INSERT INTO `points_transactions` VALUES (121, 39, -100, 9902, 'admin_adjust', NULL, '1', '2025-12-27 23:58:06');
INSERT INTO `points_transactions` VALUES (122, 39, -1200, 8702, 'admin_adjust', NULL, '1', '2025-12-27 23:58:12');
INSERT INTO `points_transactions` VALUES (123, 39, -2000, 6702, 'admin_adjust', NULL, '1', '2025-12-27 23:58:25');
INSERT INTO `points_transactions` VALUES (124, 39, -6700, 2, 'admin_adjust', NULL, '1', '2025-12-28 00:06:25');
INSERT INTO `points_transactions` VALUES (125, 39, 25, 9025, 'signin', NULL, '每日签到', '2025-12-28 00:20:47');
INSERT INTO `points_transactions` VALUES (126, 22, 1, 8616, 'admin_repair', NULL, '一致性修复：补齐批次余额', '2025-12-28 01:18:03');
INSERT INTO `points_transactions` VALUES (127, 30, 1, 101, 'admin_repair', NULL, '一致性修复：补齐批次余额', '2025-12-28 01:18:03');
INSERT INTO `points_transactions` VALUES (128, 39, 9000, 9025, 'admin_repair', NULL, '一致性修复：补齐批次余额', '2025-12-28 01:18:03');
INSERT INTO `points_transactions` VALUES (129, 39, -3000, 6025, 'redeem', NULL, '积分兑换扣减', '2025-12-28 01:20:24');
INSERT INTO `points_transactions` VALUES (130, 39, -400, 5625, 'redeem', NULL, '积分兑换扣减', '2025-12-28 08:42:23');
INSERT INTO `points_transactions` VALUES (131, 39, 23, 5648, 'order_completed', NULL, '咖啡订单完成: CF202512280856043195', '2025-12-28 08:57:11');
INSERT INTO `points_transactions` VALUES (132, 39, 86, 5734, 'order_completed', NULL, '咖啡订单完成: CF202512280924362855', '2025-12-28 09:25:09');
INSERT INTO `points_transactions` VALUES (133, 39, -400, 5334, 'redeem', NULL, '积分兑换扣减', '2025-12-28 10:28:27');
INSERT INTO `points_transactions` VALUES (134, 39, -400, 4934, 'redeem', NULL, '积分兑换扣减', '2025-12-28 11:01:19');
INSERT INTO `points_transactions` VALUES (135, 22, -400, 8216, 'redeem', NULL, '积分兑换扣减', '2025-12-28 11:04:05');
INSERT INTO `points_transactions` VALUES (136, 22, -400, 7816, 'redeem', NULL, '积分兑换扣减', '2025-12-28 11:12:01');
INSERT INTO `points_transactions` VALUES (137, 22, -6000, 1816, 'redeem', NULL, '积分兑换扣减', '2025-12-28 11:13:57');
INSERT INTO `points_transactions` VALUES (138, 22, -1600, 216, 'redeem', NULL, '积分兑换扣减', '2025-12-28 11:14:16');
INSERT INTO `points_transactions` VALUES (139, 22, 5000, 5216, 'admin', NULL, '测试用途', '2025-12-28 12:22:10');
INSERT INTO `points_transactions` VALUES (140, 22, -882, 4334, 'redeem', NULL, '积分兑换扣减', '2025-12-28 12:22:33');
INSERT INTO `points_transactions` VALUES (141, 22, -882, 3452, 'redeem', NULL, '积分兑换扣减', '2025-12-28 12:39:31');
INSERT INTO `points_transactions` VALUES (142, 22, -882, 2570, 'redeem', NULL, '积分兑换扣减', '2025-12-28 13:15:50');
INSERT INTO `points_transactions` VALUES (143, 22, -882, 1688, 'redeem', NULL, '积分兑换扣减', '2025-12-28 13:25:14');
INSERT INTO `points_transactions` VALUES (144, 22, -450, 1238, 'redeem', NULL, '积分兑换扣减', '2025-12-28 13:59:00');
INSERT INTO `points_transactions` VALUES (145, 22, -252, 986, 'redeem', NULL, '积分兑换扣减', '2025-12-28 14:39:21');
INSERT INTO `points_transactions` VALUES (146, 22, 43, 1029, 'order_completed', NULL, '咖啡订单完成: CF202512281525182965', '2025-12-28 15:26:57');
INSERT INTO `points_transactions` VALUES (147, 22, -252, 777, 'redeem', NULL, '积分兑换扣减', '2025-12-28 15:40:17');
INSERT INTO `points_transactions` VALUES (148, 22, 43, 820, 'order_completed', NULL, '咖啡订单完成: CF202512281540439236', '2025-12-28 15:41:44');
INSERT INTO `points_transactions` VALUES (149, 22, 3000, 3820, 'admin', NULL, 'admin', '2025-12-28 15:46:49');
INSERT INTO `points_transactions` VALUES (150, 22, -882, 2938, 'redeem', NULL, '积分兑换扣减', '2025-12-28 15:47:29');
INSERT INTO `points_transactions` VALUES (151, 22, 388, 3326, 'order_completed', NULL, '咖啡订单完成: CF202512281551228067', '2025-12-28 15:52:01');
INSERT INTO `points_transactions` VALUES (152, 22, 48, 3374, 'order_completed', NULL, '咖啡订单完成: CF202512281552244378', '2025-12-28 15:54:17');
INSERT INTO `points_transactions` VALUES (153, 22, -900, 2474, 'redeem', NULL, '积分兑换扣减', '2025-12-28 16:00:25');
INSERT INTO `points_transactions` VALUES (154, 22, -900, 1574, 'redeem', NULL, '积分兑换扣减', '2025-12-28 16:14:35');
INSERT INTO `points_transactions` VALUES (155, 22, -900, 674, 'redeem', NULL, '积分兑换扣减', '2025-12-28 16:49:06');
INSERT INTO `points_transactions` VALUES (156, 22, 3000, 3674, 'admin', NULL, '测试用途', '2025-12-28 18:30:54');
INSERT INTO `points_transactions` VALUES (157, 22, -900, 2774, 'redeem', NULL, '积分兑换扣减', '2025-12-28 18:33:07');
INSERT INTO `points_transactions` VALUES (158, 22, -252, 2522, 'redeem', NULL, '积分兑换扣减', '2025-12-28 19:30:43');
INSERT INTO `points_transactions` VALUES (159, 22, 144, 2666, 'monthly_mission', NULL, '月度任务奖励（满300元）', '2025-12-28 21:14:49');
INSERT INTO `points_transactions` VALUES (160, 40, 200, 200, 'register', 40, '新用户注册奖励', '2025-12-31 15:55:11');
INSERT INTO `points_transactions` VALUES (161, 41, 200, 200, 'register', 41, '新用户注册奖励', '2025-12-31 16:01:08');
INSERT INTO `points_transactions` VALUES (162, 39, 20, 4954, 'signin', NULL, '每日签到', '2025-12-31 16:13:31');
INSERT INTO `points_transactions` VALUES (164, 41, 50, 250, 'order_completed', NULL, '咖啡订单完成: CF202512311638161546', '2025-12-31 16:38:39');
INSERT INTO `points_transactions` VALUES (165, 41, 300, 550, 'first_order_bonus', NULL, '首单奖励', '2025-12-31 16:38:39');
INSERT INTO `points_transactions` VALUES (166, 41, 50, 600, 'order_completed', NULL, '咖啡订单完成: CF202512311647066338', '2025-12-31 16:47:57');
INSERT INTO `points_transactions` VALUES (167, 41, 300, 900, 'admin', NULL, NULL, '2025-12-31 17:05:02');
INSERT INTO `points_transactions` VALUES (168, 41, 250, 1150, 'order_completed', NULL, '咖啡订单完成: CF202512311705575749', '2025-12-31 17:06:14');
INSERT INTO `points_transactions` VALUES (169, 41, 120, 1270, 'monthly_task_bonus', NULL, '月度任务满300元奖励', '2025-12-31 17:06:14');
INSERT INTO `points_transactions` VALUES (170, 41, 300, 1570, 'order_completed', NULL, '咖啡订单完成: CF202512311708371364', '2025-12-31 17:08:44');
INSERT INTO `points_transactions` VALUES (171, 41, 180, 1750, 'monthly_task_bonus', NULL, '月度任务满600元奖励', '2025-12-31 17:08:44');
INSERT INTO `points_transactions` VALUES (172, 41, 375, 2125, 'order_completed', NULL, '咖啡订单完成: CF202512311709178683', '2025-12-31 17:09:24');
INSERT INTO `points_transactions` VALUES (173, 41, 413, 2538, 'order_completed', NULL, '咖啡订单完成: CF202512311709104819', '2025-12-31 17:09:25');
INSERT INTO `points_transactions` VALUES (174, 41, 368, 2906, 'monthly_task_bonus', NULL, '月度任务满1000元奖励', '2025-12-31 17:09:25');
INSERT INTO `points_transactions` VALUES (175, 41, 10, 2916, 'admin', NULL, NULL, '2025-12-31 17:14:10');
INSERT INTO `points_transactions` VALUES (176, 41, 10, 2926, 'admin', NULL, NULL, '2025-12-31 17:14:16');
INSERT INTO `points_transactions` VALUES (177, 41, 10, 2936, 'manual_test', NULL, NULL, '2025-12-31 17:23:44');
INSERT INTO `points_transactions` VALUES (178, 41, -280, 2656, 'redeem', NULL, '积分兑换扣减', '2025-12-31 18:05:04');
INSERT INTO `points_transactions` VALUES (179, 41, -280, 2376, 'redeem', NULL, '积分兑换扣减', '2025-12-31 18:18:43');
INSERT INTO `points_transactions` VALUES (180, 41, -280, 2096, 'redeem', NULL, '积分兑换扣减', '2025-12-31 18:18:45');
INSERT INTO `points_transactions` VALUES (181, 41, -280, 1816, 'redeem', NULL, '积分兑换扣减', '2025-12-31 18:27:52');
INSERT INTO `points_transactions` VALUES (182, 41, -280, 1536, 'redeem', NULL, '积分兑换扣减', '2025-12-31 18:27:55');
INSERT INTO `points_transactions` VALUES (183, 41, -280, 1256, 'redeem', NULL, '积分兑换扣减', '2025-12-31 18:27:56');
INSERT INTO `points_transactions` VALUES (184, 41, -1, 1255, 'redeem', NULL, '积分兑换扣减', '2025-12-31 19:45:32');
INSERT INTO `points_transactions` VALUES (185, 41, -1, 1254, 'redeem', NULL, '积分兑换扣减', '2025-12-31 19:45:45');
INSERT INTO `points_transactions` VALUES (194, 41, 10, 2002, 'signin', NULL, '每日签到（连续8天）', '2026-01-01 01:24:03');
INSERT INTO `points_transactions` VALUES (195, 41, -1, 2001, 'redeem', NULL, '积分兑换扣减', '2026-01-01 14:20:50');
INSERT INTO `points_transactions` VALUES (196, 41, 100, 2101, 'manual_test', NULL, NULL, '2026-01-01 14:34:58');
INSERT INTO `points_transactions` VALUES (197, 41, 1000, 3101, 'admin', NULL, '1', '2026-01-01 15:00:36');
INSERT INTO `points_transactions` VALUES (198, 41, -980, 2121, 'redeem', NULL, '积分兑换扣减', '2026-01-01 15:02:02');
INSERT INTO `points_transactions` VALUES (199, 41, 28, 2149, 'order_completed', NULL, '咖啡订单完成: CF202512311807112287', '2026-01-01 15:07:13');
INSERT INTO `points_transactions` VALUES (200, 41, 325, 2474, 'order_completed', NULL, '咖啡订单完成: CF202601011508425032', '2026-01-01 15:08:55');
INSERT INTO `points_transactions` VALUES (201, 41, 299, 2773, 'order_completed', NULL, '咖啡订单完成: CF202601011508308596', '2026-01-01 15:08:57');
INSERT INTO `points_transactions` VALUES (202, 41, 138, 2911, 'monthly_task_bonus', NULL, '月度任务满300元奖励', '2026-01-01 15:08:57');
INSERT INTO `points_transactions` VALUES (203, 41, 33, 2944, 'order_completed', NULL, '咖啡订单完成: CF202601011705547437', '2026-01-01 17:06:05');
INSERT INTO `points_transactions` VALUES (204, 41, 34, 2978, 'order_completed', NULL, '咖啡订单完成: CF202601011800558706', '2026-01-01 18:02:00');
INSERT INTO `points_transactions` VALUES (205, 41, 15, 2993, 'signin', NULL, '每日签到（连续9天）', '2026-01-02 10:40:29');
INSERT INTO `points_transactions` VALUES (206, 41, 34, 3027, 'order_completed', NULL, '咖啡订单完成: CF202601021050433025', '2026-01-02 10:51:10');
INSERT INTO `points_transactions` VALUES (207, 41, 390, 3417, 'order_completed', NULL, '咖啡订单完成: CF202601021102011086', '2026-01-02 11:03:20');
INSERT INTO `points_transactions` VALUES (208, 41, 216, 3633, 'monthly_task_bonus', NULL, '月度任务满600元奖励', '2026-01-02 11:03:20');
INSERT INTO `points_transactions` VALUES (209, 41, 27, 3660, 'order_completed', NULL, '咖啡订单完成: CF202601021102412631', '2026-01-02 11:03:31');
INSERT INTO `points_transactions` VALUES (210, 43, 200, 200, 'register', 43, '新用户注册奖励', '2026-01-02 11:23:04');
INSERT INTO `points_transactions` VALUES (211, 43, 390, 590, 'order_completed', NULL, '咖啡订单完成: CF202601021125219552', '2026-01-02 11:25:42');
INSERT INTO `points_transactions` VALUES (212, 43, 300, 890, 'first_order_bonus', NULL, '首单奖励', '2026-01-02 11:25:42');
INSERT INTO `points_transactions` VALUES (213, 43, 144, 1034, 'monthly_task_bonus', NULL, '月度任务满300元奖励', '2026-01-02 11:25:42');
INSERT INTO `points_transactions` VALUES (214, 43, 430, 1464, 'order_completed', NULL, '咖啡订单完成: CF202601021130484338', '2026-01-02 11:31:02');
INSERT INTO `points_transactions` VALUES (215, 43, 216, 1680, 'monthly_task_bonus', NULL, '月度任务满600元奖励', '2026-01-02 11:31:02');
INSERT INTO `points_transactions` VALUES (216, 43, -270, 1410, 'redeem', NULL, '积分兑换扣减', '2026-01-02 17:44:48');
INSERT INTO `points_transactions` VALUES (217, 43, -1, 1409, 'redeem', NULL, '积分兑换扣减', '2026-01-02 18:03:25');
INSERT INTO `points_transactions` VALUES (218, 43, 54, 1463, 'order_completed', NULL, '咖啡订单完成: CF202601021810274657', '2026-01-02 18:11:10');
INSERT INTO `points_transactions` VALUES (219, 43, 55, 1518, 'order_completed', NULL, '咖啡订单完成: CF202601021836521772', '2026-01-02 18:37:46');
INSERT INTO `points_transactions` VALUES (220, 43, 27, 1545, 'order_completed', NULL, '咖啡订单完成: CF202601021841438238', '2026-01-02 18:42:22');
INSERT INTO `points_transactions` VALUES (221, 43, 27, 1572, 'order_completed', NULL, '咖啡订单完成: CF202601021846473402', '2026-01-02 22:12:02');
INSERT INTO `points_transactions` VALUES (222, 43, 27, 1599, 'order_completed', NULL, '咖啡订单完成: CF202601022232555262', '2026-01-02 22:33:05');
INSERT INTO `points_transactions` VALUES (223, 43, 9999, 11598, 'admin', NULL, '1', '2026-01-02 22:50:58');
INSERT INTO `points_transactions` VALUES (224, 43, -1800, 9798, 'redeem', NULL, '积分兑换扣减', '2026-01-02 22:51:46');
INSERT INTO `points_transactions` VALUES (225, 43, -1800, 7998, 'redeem', NULL, '积分兑换扣减', '2026-01-02 23:11:10');
INSERT INTO `points_transactions` VALUES (226, 43, 34, 8032, 'order_completed', NULL, '咖啡订单完成: CF202601022248521485', '2026-01-02 23:27:33');
INSERT INTO `points_transactions` VALUES (227, 43, -833, 7199, 'redeem', NULL, '积分兑换扣减', '2026-01-02 23:41:49');
INSERT INTO `points_transactions` VALUES (228, 43, -1700, 5499, 'redeem', NULL, '积分兑换扣减', '2026-01-02 23:53:52');
INSERT INTO `points_transactions` VALUES (229, 43, 40, 5539, 'order_completed', NULL, '咖啡订单完成: CF202601030024093293', '2026-01-03 00:24:20');
INSERT INTO `points_transactions` VALUES (230, 43, -1700, 3839, 'redeem', NULL, '积分兑换扣减', '2026-01-03 00:32:43');
INSERT INTO `points_transactions` VALUES (231, 43, 47, 3886, 'order_completed', NULL, '咖啡订单完成: CF202601030032549885', '2026-01-03 00:33:32');
INSERT INTO `points_transactions` VALUES (232, 43, 27, 3913, 'order_completed', NULL, '咖啡订单完成: CF202601030041209801', '2026-01-03 00:42:00');
INSERT INTO `points_transactions` VALUES (233, 43, 27, 3940, 'order_completed', NULL, '咖啡订单完成: CF202601030043134808', '2026-01-03 00:43:21');
INSERT INTO `points_transactions` VALUES (234, 43, 2, 3942, 'signin', NULL, '每日签到', '2026-01-03 21:46:57');
INSERT INTO `points_transactions` VALUES (235, 43, 2, 3944, 'signin', NULL, '每日签到（连续7天）', '2026-01-03 22:02:19');
INSERT INTO `points_transactions` VALUES (236, 43, 2, 3946, 'signin', NULL, '每日签到（连续8天）', '2026-01-03 22:05:05');
INSERT INTO `points_transactions` VALUES (237, 43, 2, 3948, 'signin', NULL, '每日签到（连续7天）', '2026-01-03 22:05:48');
INSERT INTO `points_transactions` VALUES (238, 43, 2, 3950, 'signin', NULL, '每日签到（连续8天）', '2026-01-03 22:06:52');
INSERT INTO `points_transactions` VALUES (239, 43, 2, 3952, 'signin', NULL, '每日签到', '2026-01-03 22:10:16');
INSERT INTO `points_transactions` VALUES (240, 43, 2, 3954, 'signin', NULL, '每日签到（连续7天）', '2026-01-03 22:16:03');
INSERT INTO `points_transactions` VALUES (241, 43, 2, 3956, 'signin', NULL, '每日签到（连续8天）', '2026-01-03 22:16:32');
INSERT INTO `points_transactions` VALUES (242, 43, 2, 3958, 'signin', NULL, '每日签到（连续9天）', '2026-01-04 00:20:04');
INSERT INTO `points_transactions` VALUES (243, 41, 2, 3662, 'signin', NULL, '每日签到', '2026-01-04 17:00:03');
INSERT INTO `points_transactions` VALUES (244, 41, 51, 3713, 'order_completed', NULL, '咖啡订单完成: CF202601050019544626', '2026-01-05 00:28:17');
INSERT INTO `points_transactions` VALUES (245, 43, 2, 3960, 'signin', NULL, '每日签到（连续10天）', '2026-01-05 00:28:23');
INSERT INTO `points_transactions` VALUES (246, 41, 2, 3715, 'signin', NULL, '每日签到（连续2天）', '2026-01-05 01:27:00');
INSERT INTO `points_transactions` VALUES (247, 43, 2, 3962, 'signin', NULL, '每日签到', '2026-01-07 22:38:44');
INSERT INTO `points_transactions` VALUES (248, 43, 100, 4062, 'birthday_gift', NULL, '生日快乐！黑金会员获赠100积分及专属生日大礼包', '2026-01-07 23:22:20');
INSERT INTO `points_transactions` VALUES (249, 43, 100, 4162, 'birthday_gift', NULL, '生日快乐！黑金会员获赠100积分及专属生日大礼包', '2026-01-08 00:19:43');
INSERT INTO `points_transactions` VALUES (250, 43, 2, 4164, 'signin', NULL, '每日签到', '2026-01-10 14:50:28');
INSERT INTO `points_transactions` VALUES (251, 43, 2, 4166, 'signin', NULL, '每日签到（连续7天）', '2026-01-10 14:52:05');
INSERT INTO `points_transactions` VALUES (252, 43, 2, 4168, 'signin', NULL, '每日签到（连续8天）', '2026-01-10 15:00:40');
INSERT INTO `points_transactions` VALUES (253, 43, -102, 4066, 'redeem', NULL, '积分兑换扣减', '2026-01-10 15:01:40');
INSERT INTO `points_transactions` VALUES (254, 43, 57, 4123, 'order_completed', NULL, '咖啡订单完成: CF202601101502282746', '2026-01-10 15:09:40');
INSERT INTO `points_transactions` VALUES (255, 43, 33, 4156, 'order_completed', NULL, '咖啡订单完成: CF202601101548061660', '2026-01-10 15:49:10');
INSERT INTO `points_transactions` VALUES (256, 43, -102, 4054, 'redeem', NULL, '积分兑换扣减', '2026-01-10 15:51:34');
INSERT INTO `points_transactions` VALUES (257, 43, 42, 4096, 'order_completed', NULL, '咖啡订单完成: CF202601101556347502', '2026-01-10 15:57:55');
INSERT INTO `points_transactions` VALUES (258, 43, 33, 4129, 'order_completed', NULL, '咖啡订单完成: CF202601101600252427', '2026-01-10 16:00:42');
INSERT INTO `points_transactions` VALUES (259, 43, -204, 3925, 'redeem', NULL, '积分兑换扣减', '2026-01-10 16:36:50');
INSERT INTO `points_transactions` VALUES (260, 44, 200, 200, 'register', 44, '新用户注册奖励', '2026-01-10 17:32:29');
INSERT INTO `points_transactions` VALUES (261, 45, 200, 200, 'register', 45, '新用户注册奖励', '2026-01-10 17:59:09');
INSERT INTO `points_transactions` VALUES (262, 44, 150, 350, 'invite', NULL, '邀请好友注册奖励', '2026-01-10 17:59:09');
INSERT INTO `points_transactions` VALUES (263, 45, 80, 280, 'invited', NULL, '填写好友邀请码奖励', '2026-01-10 17:59:09');
INSERT INTO `points_transactions` VALUES (264, 46, 200, 200, 'register', 46, '新用户注册奖励', '2026-01-10 18:33:23');
INSERT INTO `points_transactions` VALUES (265, 47, 200, 200, 'register', 47, '新用户注册奖励', '2026-01-10 19:07:24');
INSERT INTO `points_transactions` VALUES (266, 48, 200, 200, 'register', 48, '新用户注册奖励', '2026-01-10 19:19:25');
INSERT INTO `points_transactions` VALUES (267, 49, 200, 200, 'register', 49, '新用户注册奖励', '2026-01-10 19:24:05');
INSERT INTO `points_transactions` VALUES (268, 50, 200, 200, 'register', 50, '新用户注册奖励', '2026-01-10 22:18:40');
INSERT INTO `points_transactions` VALUES (269, 50, 19, 219, 'order_completed', NULL, '咖啡订单完成: CF202601102231022777', '2026-01-10 22:31:34');
INSERT INTO `points_transactions` VALUES (270, 50, 300, 519, 'first_order_bonus', NULL, '首单奖励', '2026-01-10 22:31:34');
INSERT INTO `points_transactions` VALUES (271, 43, 33, 3958, 'order_completed', NULL, '咖啡订单完成: CF202601102244411826', '2026-01-10 22:45:28');
INSERT INTO `points_transactions` VALUES (272, 43, 40, 3998, 'challenge_order', NULL, '挑战任务【打卡达人】完成奖励', '2026-01-10 22:45:29');
INSERT INTO `points_transactions` VALUES (273, 43, 60, 4058, 'challenge_morning', NULL, '挑战任务【晨间唤醒】完成奖励', '2026-01-10 22:45:29');
INSERT INTO `points_transactions` VALUES (274, 43, 50, 4108, 'challenge_delivery', NULL, '挑战任务【外卖尝鲜】完成奖励', '2026-01-10 22:45:29');
INSERT INTO `points_transactions` VALUES (275, 43, -298, 3810, 'redeem', NULL, '积分兑换扣减', '2026-01-10 22:45:48');
INSERT INTO `points_transactions` VALUES (276, 50, 22, 541, 'order_completed', NULL, '咖啡订单完成: CF202601102309599058', '2026-01-10 23:10:16');
INSERT INTO `points_transactions` VALUES (277, 50, 50, 591, 'order_completed', NULL, '咖啡订单完成: CF202601102328426989', '2026-01-10 23:28:52');
INSERT INTO `points_transactions` VALUES (278, 50, 40, 631, 'challenge_order', NULL, '挑战任务【打卡达人】完成奖励', '2026-01-10 23:28:52');
INSERT INTO `points_transactions` VALUES (279, 50, 50, 681, 'challenge_delivery', NULL, '挑战任务【外卖尝鲜】完成奖励', '2026-01-10 23:28:52');
INSERT INTO `points_transactions` VALUES (280, 50, 80, 761, 'challenge_newproduct', NULL, '挑战任务【新品猎人】完成奖励', '2026-01-10 23:28:52');
INSERT INTO `points_transactions` VALUES (281, 50, 36, 797, 'order_completed', NULL, '咖啡订单完成: CF202601102328292959', '2026-01-10 23:28:54');
INSERT INTO `points_transactions` VALUES (282, 50, 0, 797, 'birthday_gift', NULL, '生日快乐！获赠买一送一券', '2026-01-10 23:29:53');
INSERT INTO `points_transactions` VALUES (283, 43, 0, 3810, 'monthly_benefit_202601', NULL, '领取202601月度等级权益', '2026-01-11 14:00:03');
INSERT INTO `points_transactions` VALUES (284, 43, -72, 3738, 'redeem', NULL, '积分兑换扣减', '2026-01-11 14:20:38');
INSERT INTO `points_transactions` VALUES (285, 49, 0, 200, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:silver', '2026-01-11 14:33:21');
INSERT INTO `points_transactions` VALUES (286, 48, 0, 200, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-11 14:44:15');
INSERT INTO `points_transactions` VALUES (287, 48, 28, 228, 'order_completed', NULL, '咖啡订单完成: CF202601111551506364', '2026-01-11 15:52:26');
INSERT INTO `points_transactions` VALUES (288, 48, 40, 268, 'challenge_order', NULL, '打卡达人自动补发', '2026-01-11 16:20:07');
INSERT INTO `points_transactions` VALUES (289, 48, 40, 308, 'challenge_order', NULL, '打卡达人自动补发(Info)', '2026-01-11 16:20:07');
INSERT INTO `points_transactions` VALUES (290, 48, 40, 308, 'challenge_order', NULL, '打卡达人自动补发(Info)', '2026-01-11 16:20:07');
INSERT INTO `points_transactions` VALUES (291, 48, 22, 330, 'order_completed', NULL, '咖啡订单完成: CF202601111628285012', '2026-01-11 16:28:43');
INSERT INTO `points_transactions` VALUES (292, 48, 2, 332, 'signin', NULL, '每日签到', '2026-01-11 16:28:50');
INSERT INTO `points_transactions` VALUES (293, 49, 2, 202, 'signin', NULL, '每日签到', '2026-01-11 16:33:14');
INSERT INTO `points_transactions` VALUES (294, 49, 48, 250, 'order_completed', NULL, '咖啡订单完成: CF202601111657105031', '2026-01-11 16:57:33');
INSERT INTO `points_transactions` VALUES (295, 47, 0, 200, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:gold', '2026-01-11 17:05:46');
INSERT INTO `points_transactions` VALUES (296, 43, -72, 3666, 'redeem', NULL, '积分兑换扣减', '2026-01-11 17:44:54');
INSERT INTO `points_transactions` VALUES (297, 51, 200, 200, 'register', 51, '新用户注册奖励', '2026-01-11 18:07:11');
INSERT INTO `points_transactions` VALUES (298, 51, 0, 200, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-11 18:07:59');
INSERT INTO `points_transactions` VALUES (299, 43, 29, 3695, 'order_completed', NULL, '咖啡订单完成: CF202601121021458245', '2026-01-12 10:22:18');
INSERT INTO `points_transactions` VALUES (300, 43, 12, 3707, 'order_completed', NULL, '咖啡订单完成: CF202601121033266103', '2026-01-12 10:33:34');
INSERT INTO `points_transactions` VALUES (301, 52, 40, 40, 'challenge_order', NULL, '打卡达人自动补发', '2026-01-12 11:31:27');
INSERT INTO `points_transactions` VALUES (302, 52, 40, 80, 'challenge_order', NULL, '打卡达人自动补发(Info)', '2026-01-12 11:31:27');
INSERT INTO `points_transactions` VALUES (303, 52, 40, 120, 'challenge_order', NULL, '打卡达人自动补发(Info)', '2026-01-12 11:31:27');
INSERT INTO `points_transactions` VALUES (304, 52, 0, 120, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-12 11:50:08');
INSERT INTO `points_transactions` VALUES (305, 52, 14, 134, 'order_completed', NULL, '咖啡订单完成: CF202601121202196631', '2026-01-12 12:02:32');
INSERT INTO `points_transactions` VALUES (306, 52, 200, 334, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 12:02:32');
INSERT INTO `points_transactions` VALUES (307, 52, 38, 372, 'order_completed', NULL, '咖啡订单完成: CF202601121202435017', '2026-01-12 12:02:51');
INSERT INTO `points_transactions` VALUES (308, 52, 0, 372, 'birthday_gift', NULL, '生日快乐！获赠单饮品5折券', '2026-01-12 13:58:11');
INSERT INTO `points_transactions` VALUES (309, 51, 0, 200, 'birthday_gift', NULL, '生日快乐！获赠单饮品5折券(限标准杯)', '2026-01-12 14:09:40');
INSERT INTO `points_transactions` VALUES (310, 53, 0, 0, 'birthday_gift', NULL, '生日快乐！获赠单饮品5折券(限标准杯)', '2026-01-12 14:10:55');
INSERT INTO `points_transactions` VALUES (311, 53, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-12 14:15:48');
INSERT INTO `points_transactions` VALUES (312, 54, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-12 14:48:19');
INSERT INTO `points_transactions` VALUES (313, 54, 0, 0, 'birthday_gift', NULL, '生日快乐！获赠单饮品5折券(限标准杯)', '2026-01-12 14:48:28');
INSERT INTO `points_transactions` VALUES (314, 55, 0, 0, 'birthday_gift', NULL, '生日快乐！获赠单饮品5折券(限标准杯)', '2026-01-12 14:59:15');
INSERT INTO `points_transactions` VALUES (315, 55, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-12 15:16:29');
INSERT INTO `points_transactions` VALUES (316, 41, 40, 3755, 'challenge_order', NULL, '打卡达人自动补发(Info)', '2026-01-12 15:17:36');
INSERT INTO `points_transactions` VALUES (317, 55, 11, 11, 'order_completed', NULL, '咖啡订单完成: CF202601121517135927', '2026-01-12 15:18:02');
INSERT INTO `points_transactions` VALUES (318, 55, 200, 211, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 15:18:02');
INSERT INTO `points_transactions` VALUES (319, 55, 22, 233, 'order_completed', NULL, '咖啡订单完成: CF202601121520575419', '2026-01-12 15:21:10');
INSERT INTO `points_transactions` VALUES (320, 55, 22, 255, 'order_completed', NULL, '咖啡订单完成: CF202601121532342910', '2026-01-12 15:32:48');
INSERT INTO `points_transactions` VALUES (321, 54, 22, 22, 'order_completed', NULL, '咖啡订单完成: CF202601121536327829', '2026-01-12 15:37:05');
INSERT INTO `points_transactions` VALUES (322, 54, 200, 222, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 15:37:05');
INSERT INTO `points_transactions` VALUES (323, 56, 28, 28, 'order_completed', NULL, '咖啡订单完成: CF202601121545014245', '2026-01-12 15:45:07');
INSERT INTO `points_transactions` VALUES (324, 56, 200, 228, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 15:45:07');
INSERT INTO `points_transactions` VALUES (325, 56, 0, 228, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:silver', '2026-01-12 15:45:58');
INSERT INTO `points_transactions` VALUES (326, 56, 0, 228, 'birthday_gift', NULL, '生日快乐！白银会员获赠买一赠一券', '2026-01-12 16:59:23');
INSERT INTO `points_transactions` VALUES (327, 56, 24, 252, 'order_completed', NULL, '咖啡订单完成: CF202601121713326416', '2026-01-12 17:13:38');
INSERT INTO `points_transactions` VALUES (328, 56, 50, 302, 'upgrade_reward', NULL, '黄金晋升奖励', '2026-01-12 17:13:38');
INSERT INTO `points_transactions` VALUES (329, 57, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:gold', '2026-01-12 17:20:00');
INSERT INTO `points_transactions` VALUES (330, 57, 0, 0, 'birthday_gift', NULL, '生日快乐！黄金会员获赠标准饮品免单券', '2026-01-12 17:27:53');
INSERT INTO `points_transactions` VALUES (331, 57, 79, 79, 'order_completed', NULL, '咖啡订单完成: CF202601121755333526', '2026-01-12 17:55:54');
INSERT INTO `points_transactions` VALUES (332, 57, 100, 179, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-12 17:55:54');
INSERT INTO `points_transactions` VALUES (333, 57, 200, 379, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 17:55:54');
INSERT INTO `points_transactions` VALUES (334, 58, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:gold', '2026-01-12 18:49:42');
INSERT INTO `points_transactions` VALUES (335, 58, 0, 0, 'birthday_gift', NULL, '生日快乐！黄金会员获赠标准饮品免单券', '2026-01-12 18:50:35');
INSERT INTO `points_transactions` VALUES (336, 59, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:diamond', '2026-01-12 20:14:47');
INSERT INTO `points_transactions` VALUES (337, 59, 0, 0, 'birthday_gift', NULL, '生日快乐！钻石会员获赠全通兑免单券及蛋糕5折券', '2026-01-12 20:44:58');
INSERT INTO `points_transactions` VALUES (338, 59, 13, 13, 'order_completed', NULL, '咖啡订单完成: CF202601122103369760', '2026-01-12 21:04:31');
INSERT INTO `points_transactions` VALUES (339, 59, 200, 213, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 21:04:31');
INSERT INTO `points_transactions` VALUES (340, 59, 46, 259, 'order_completed', NULL, '咖啡订单完成: CF202601122103115467', '2026-01-12 21:04:33');
INSERT INTO `points_transactions` VALUES (341, 56, 59, 361, 'order_completed', NULL, '咖啡订单完成: CF202601122140587436', '2026-01-12 21:41:29');
INSERT INTO `points_transactions` VALUES (342, 56, 100, 461, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-12 21:41:29');
INSERT INTO `points_transactions` VALUES (343, 60, 26, 26, 'order_completed', NULL, '咖啡订单完成: CF202601122147266014', '2026-01-12 21:47:32');
INSERT INTO `points_transactions` VALUES (344, 60, 100, 126, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-12 21:47:32');
INSERT INTO `points_transactions` VALUES (345, 60, 200, 326, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 21:47:32');
INSERT INTO `points_transactions` VALUES (346, 46, 26, 226, 'order_completed', NULL, '咖啡订单完成: CF202601122151563403', '2026-01-12 21:52:03');
INSERT INTO `points_transactions` VALUES (347, 46, 100, 326, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-12 21:52:03');
INSERT INTO `points_transactions` VALUES (348, 46, 200, 526, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 21:52:03');
INSERT INTO `points_transactions` VALUES (349, 61, 13, 13, 'order_completed', NULL, '咖啡订单完成: CF202601122205561695', '2026-01-12 22:06:04');
INSERT INTO `points_transactions` VALUES (350, 61, 100, 113, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-12 22:06:04');
INSERT INTO `points_transactions` VALUES (351, 61, 200, 313, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 22:06:04');
INSERT INTO `points_transactions` VALUES (352, 61, 0, 313, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:diamond', '2026-01-12 22:08:20');
INSERT INTO `points_transactions` VALUES (353, 62, 13, 13, 'order_completed', NULL, '咖啡订单完成: CF202601122231587943', '2026-01-12 22:32:04');
INSERT INTO `points_transactions` VALUES (354, 62, 100, 113, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-12 22:32:04');
INSERT INTO `points_transactions` VALUES (355, 62, 200, 313, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-12 22:32:04');
INSERT INTO `points_transactions` VALUES (356, 62, 2, 315, 'signin', NULL, '每日签到', '2026-01-12 23:02:41');
INSERT INTO `points_transactions` VALUES (357, 62, 43, 358, 'order_completed', NULL, '咖啡订单完成: CF202601122304044388', '2026-01-12 23:04:18');
INSERT INTO `points_transactions` VALUES (358, 62, 0, 358, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:diamond', '2026-01-13 13:34:22');
INSERT INTO `points_transactions` VALUES (359, 62, 0, 358, 'birthday_gift', NULL, '生日快乐！钻石会员获赠全通兑免单券及蛋糕5折券', '2026-01-13 13:35:28');
INSERT INTO `points_transactions` VALUES (360, 62, 29, 387, 'order_completed', NULL, '咖啡订单完成: CF202601131353331909', '2026-01-13 13:53:43');
INSERT INTO `points_transactions` VALUES (361, 63, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:diamond', '2026-01-13 14:09:18');
INSERT INTO `points_transactions` VALUES (362, 63, 0, 0, 'birthday_gift', NULL, '生日快乐！钻石会员获赠全通兑免单券及蛋糕5折券', '2026-01-13 14:09:38');
INSERT INTO `points_transactions` VALUES (363, 63, 29, 29, 'order_completed', NULL, '咖啡订单完成: CF202601131410502348', '2026-01-13 14:10:59');
INSERT INTO `points_transactions` VALUES (364, 63, 200, 229, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-13 14:11:00');
INSERT INTO `points_transactions` VALUES (365, 64, 57, 57, 'order_completed', NULL, '咖啡订单完成: CF202601131551224770', '2026-01-13 15:51:29');
INSERT INTO `points_transactions` VALUES (366, 64, 200, 257, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-13 15:51:30');
INSERT INTO `points_transactions` VALUES (367, 64, 0, 257, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:black', '2026-01-13 15:52:39');
INSERT INTO `points_transactions` VALUES (368, 64, 888, 1145, 'birthday_gift', NULL, '🎂 生日快乐！黑金会员专属888积分贺礼已发放', '2026-01-13 15:53:19');
INSERT INTO `points_transactions` VALUES (369, 65, 17, 17, 'order_completed', NULL, '咖啡订单完成: CF202601131643162970', '2026-01-13 16:43:22');
INSERT INTO `points_transactions` VALUES (370, 65, 100, 117, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-13 16:43:22');
INSERT INTO `points_transactions` VALUES (371, 65, 200, 317, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-13 16:43:22');
INSERT INTO `points_transactions` VALUES (372, 65, 0, 317, 'birthday_gift', NULL, '生日快乐！钻石会员获赠全通兑免单券及蛋糕5折券', '2026-01-13 16:44:08');
INSERT INTO `points_transactions` VALUES (373, 65, 0, 317, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:diamond', '2026-01-13 16:49:17');
INSERT INTO `points_transactions` VALUES (374, 66, 18, 18, 'order_completed', NULL, '咖啡订单完成: CF202601131730515117', '2026-01-13 17:30:59');
INSERT INTO `points_transactions` VALUES (375, 66, 200, 218, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-13 17:30:59');
INSERT INTO `points_transactions` VALUES (376, 66, 888, 1106, 'birthday_gift', NULL, '🎂 生日快乐！黑金会员专属888积分贺礼已发放', '2026-01-13 18:06:30');
INSERT INTO `points_transactions` VALUES (377, 67, 2, 2, 'signin', NULL, '每日签到', '2026-01-13 18:17:33');
INSERT INTO `points_transactions` VALUES (378, 67, 0, 2, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-13 18:17:43');
INSERT INTO `points_transactions` VALUES (379, 67, 29, 31, 'order_completed', NULL, '咖啡订单完成: CF202601131818048261', '2026-01-13 18:18:10');
INSERT INTO `points_transactions` VALUES (380, 67, 1688, 1719, 'upgrade_reward', NULL, '黑金晋升奖励', '2026-01-13 18:18:10');
INSERT INTO `points_transactions` VALUES (381, 67, 200, 1919, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-13 18:18:10');
INSERT INTO `points_transactions` VALUES (382, 67, 888, 2807, 'birthday_gift', NULL, '🎂 生日快乐！黑金会员专属888积分贺礼已发放', '2026-01-13 18:18:57');
INSERT INTO `points_transactions` VALUES (383, 68, 47, 47, 'order_completed', NULL, '咖啡订单完成: CF202601131833435831', '2026-01-13 18:33:52');
INSERT INTO `points_transactions` VALUES (384, 68, 1688, 1735, 'upgrade_reward', NULL, '黑金晋升奖励', '2026-01-13 18:33:52');
INSERT INTO `points_transactions` VALUES (385, 68, 200, 1935, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-13 18:33:52');
INSERT INTO `points_transactions` VALUES (386, 68, 888, 2823, 'birthday_gift', NULL, '🎂 生日快乐！黑金会员专属888积分贺礼已发放', '2026-01-13 18:34:35');
INSERT INTO `points_transactions` VALUES (387, 68, -68, 2755, 'redeem', NULL, '积分兑换扣减', '2026-01-13 21:31:32');
INSERT INTO `points_transactions` VALUES (388, 68, -102, 2653, 'redeem', NULL, '积分兑换扣减', '2026-01-13 21:31:57');
INSERT INTO `points_transactions` VALUES (389, 68, -128, 2525, 'redeem', NULL, '积分兑换扣减', '2026-01-13 21:31:59');
INSERT INTO `points_transactions` VALUES (390, 68, -298, 2227, 'redeem', NULL, '积分兑换扣减', '2026-01-13 21:32:01');
INSERT INTO `points_transactions` VALUES (391, 68, -680, 1547, 'redeem', NULL, '积分兑换扣减', '2026-01-13 21:32:13');
INSERT INTO `points_transactions` VALUES (392, 68, 0, 1547, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:black', '2026-01-14 00:21:46');
INSERT INTO `points_transactions` VALUES (393, 68, 2, 1549, 'signin', NULL, '每日签到', '2026-01-14 19:15:07');
INSERT INTO `points_transactions` VALUES (394, 68, 50, 1599, 'profile', NULL, '完善个人资料（手机号+邮箱）奖励', '2026-01-15 16:33:27');
INSERT INTO `points_transactions` VALUES (395, 68, 2, 1601, 'signin', NULL, '每日签到（连续2天）', '2026-01-15 16:34:09');
INSERT INTO `points_transactions` VALUES (396, 68, -680, 921, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:19:25');
INSERT INTO `points_transactions` VALUES (397, 68, -510, 411, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:22:29');
INSERT INTO `points_transactions` VALUES (398, 68, -68, 343, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:26:56');
INSERT INTO `points_transactions` VALUES (399, 68, -68, 275, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:33:21');
INSERT INTO `points_transactions` VALUES (400, 69, 1000, 1000, 'admin', NULL, '1', '2026-01-15 17:35:50');
INSERT INTO `points_transactions` VALUES (401, 69, 0, 1000, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-15 17:36:34');
INSERT INTO `points_transactions` VALUES (402, 69, -80, 920, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:38:00');
INSERT INTO `points_transactions` VALUES (403, 69, -80, 840, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:53:16');
INSERT INTO `points_transactions` VALUES (404, 69, -120, 720, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:53:22');
INSERT INTO `points_transactions` VALUES (405, 69, -150, 570, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:53:30');
INSERT INTO `points_transactions` VALUES (406, 69, -350, 220, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:53:38');
INSERT INTO `points_transactions` VALUES (407, 69, 1000, 1220, 'admin', NULL, '1', '2026-01-15 17:56:01');
INSERT INTO `points_transactions` VALUES (408, 69, -800, 420, 'redeem', NULL, '积分兑换扣减', '2026-01-15 17:56:21');
INSERT INTO `points_transactions` VALUES (409, 69, -350, 70, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:02:45');
INSERT INTO `points_transactions` VALUES (410, 69, 9999, 10069, 'admin', NULL, '1', '2026-01-15 18:04:29');
INSERT INTO `points_transactions` VALUES (411, 69, -800, 9269, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:04:41');
INSERT INTO `points_transactions` VALUES (412, 69, -200, 9069, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:16:38');
INSERT INTO `points_transactions` VALUES (413, 69, -200, 8869, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:27:34');
INSERT INTO `points_transactions` VALUES (414, 69, -1200, 7669, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:33:11');
INSERT INTO `points_transactions` VALUES (415, 69, -1200, 6469, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:41:22');
INSERT INTO `points_transactions` VALUES (416, 69, -1200, 5269, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:55:47');
INSERT INTO `points_transactions` VALUES (417, 69, -1, 5268, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:56:51');
INSERT INTO `points_transactions` VALUES (418, 69, -1, 5267, 'redeem', NULL, '积分兑换扣减', '2026-01-15 18:58:22');
INSERT INTO `points_transactions` VALUES (419, 69, -1, 5266, 'redeem', NULL, '积分兑换扣减', '2026-01-15 19:09:54');
INSERT INTO `points_transactions` VALUES (420, 69, -1, 5265, 'redeem', NULL, '积分兑换扣减', '2026-01-15 19:10:47');
INSERT INTO `points_transactions` VALUES (421, 69, -11, 5254, 'redeem', NULL, '积分兑换扣减', '2026-01-15 19:12:08');
INSERT INTO `points_transactions` VALUES (422, 69, -11, 5243, 'redeem', NULL, '积分兑换扣减', '2026-01-15 19:15:42');
INSERT INTO `points_transactions` VALUES (423, 69, -11, 5232, 'redeem', NULL, '积分兑换扣减', '2026-01-15 19:28:21');
INSERT INTO `points_transactions` VALUES (424, 53, 23, 23, 'order_completed', NULL, '咖啡订单完成: CF202601151938066306', '2026-01-15 19:38:25');
INSERT INTO `points_transactions` VALUES (425, 53, 200, 223, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-15 19:38:25');
INSERT INTO `points_transactions` VALUES (426, 69, 2, 5234, 'signin', NULL, '每日签到', '2026-01-15 19:39:22');
INSERT INTO `points_transactions` VALUES (427, 69, 2, 5236, 'signin', NULL, '每日签到（连续7天）', '2026-01-15 19:40:04');
INSERT INTO `points_transactions` VALUES (428, 70, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:basic', '2026-01-15 19:50:37');
INSERT INTO `points_transactions` VALUES (429, 70, 0, 0, 'birthday_gift', NULL, '生日快乐！获赠单饮品5折券(限标准杯)', '2026-01-15 19:53:17');
INSERT INTO `points_transactions` VALUES (430, 70, 22, 22, 'order_completed', NULL, '咖啡订单完成: CF202601151959341014', '2026-01-15 19:59:53');
INSERT INTO `points_transactions` VALUES (431, 70, 200, 222, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-15 19:59:53');
INSERT INTO `points_transactions` VALUES (432, 71, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:silver', '2026-01-15 20:05:58');
INSERT INTO `points_transactions` VALUES (433, 71, 0, 0, 'birthday_gift', NULL, '生日快乐！白银会员获赠买一赠一券', '2026-01-15 20:06:56');
INSERT INTO `points_transactions` VALUES (434, 71, 66, 66, 'order_completed', NULL, '咖啡订单完成: CF202601152007559255', '2026-01-15 20:08:08');
INSERT INTO `points_transactions` VALUES (435, 71, 50, 116, 'upgrade_reward', NULL, '黄金晋升奖励', '2026-01-15 20:08:08');
INSERT INTO `points_transactions` VALUES (436, 71, 200, 316, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-15 20:08:08');
INSERT INTO `points_transactions` VALUES (437, 72, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:gold', '2026-01-15 20:38:23');
INSERT INTO `points_transactions` VALUES (438, 72, 0, 0, 'birthday_gift', NULL, '生日快乐！黄金会员获赠标准饮品免单券', '2026-01-15 20:46:46');
INSERT INTO `points_transactions` VALUES (439, 72, 26, 26, 'order_completed', NULL, '咖啡订单完成: CF202601152050409050', '2026-01-15 20:50:49');
INSERT INTO `points_transactions` VALUES (440, 72, 100, 126, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-15 20:50:49');
INSERT INTO `points_transactions` VALUES (441, 72, 200, 326, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-15 20:50:49');
INSERT INTO `points_transactions` VALUES (442, 73, 13, 13, 'order_completed', NULL, '咖啡订单完成: CF202601152111471458', '2026-01-15 21:11:59');
INSERT INTO `points_transactions` VALUES (443, 73, 100, 113, 'upgrade_reward', NULL, '钻石晋升奖励', '2026-01-15 21:11:59');
INSERT INTO `points_transactions` VALUES (444, 73, 200, 313, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-15 21:11:59');
INSERT INTO `points_transactions` VALUES (445, 73, 0, 313, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:diamond', '2026-01-15 21:23:11');
INSERT INTO `points_transactions` VALUES (446, 73, 0, 313, 'birthday_gift', NULL, '生日快乐！钻石会员获赠优选饮品免单券及蛋糕5折券', '2026-01-15 22:36:53');
INSERT INTO `points_transactions` VALUES (447, 73, 2, 315, 'signin', NULL, '每日签到', '2026-01-15 22:43:25');
INSERT INTO `points_transactions` VALUES (448, 73, 37, 352, 'order_completed', NULL, '咖啡订单完成: CF202601152244043816', '2026-01-15 22:44:15');
INSERT INTO `points_transactions` VALUES (449, 73, 1688, 2040, 'upgrade_reward', NULL, '黑金晋升奖励', '2026-01-15 22:44:15');
INSERT INTO `points_transactions` VALUES (450, 74, 0, 0, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:black', '2026-01-15 22:47:41');
INSERT INTO `points_transactions` VALUES (451, 74, 200, 200, 'first_order_bonus', NULL, '新用户首单奖励', '2026-01-15 22:52:09');
INSERT INTO `points_transactions` VALUES (452, 74, 48, 248, 'order_completed', NULL, '咖啡订单完成: CF202601152251444742', '2026-01-15 22:52:13');
INSERT INTO `points_transactions` VALUES (453, 74, 83, 331, 'order_completed', NULL, '咖啡订单完成: CF202601152250422950', '2026-01-15 22:52:16');
INSERT INTO `points_transactions` VALUES (454, 74, 15, 346, 'order_completed', NULL, '咖啡订单完成: CF202601152250164813', '2026-01-15 22:52:18');
INSERT INTO `points_transactions` VALUES (455, 74, 40, 386, 'challenge_order', NULL, '打卡达人自动补发(Info)', '2026-01-15 22:52:18');
INSERT INTO `points_transactions` VALUES (456, 74, 40, 426, 'challenge_order', NULL, '打卡达人自动补发(Info)', '2026-01-15 22:52:18');
INSERT INTO `points_transactions` VALUES (457, 74, 24, 450, 'order_completed', NULL, '咖啡订单完成: CF202601152253101214', '2026-01-15 22:53:16');
INSERT INTO `points_transactions` VALUES (458, 74, 48, 498, 'order_completed', NULL, '咖啡订单完成: CF202601152253358715', '2026-01-15 22:53:43');
INSERT INTO `points_transactions` VALUES (459, 74, 50, 548, 'challenge_delivery', NULL, '外卖尝鲜自动补发(Info)', '2026-01-15 22:53:44');
INSERT INTO `points_transactions` VALUES (460, 74, 50, 548, 'challenge_delivery', NULL, '外卖尝鲜自动补发(Info)', '2026-01-15 22:53:44');
INSERT INTO `points_transactions` VALUES (461, 70, 20, 242, 'order_completed', 263, '咖啡订单完成: CF202601152303432464', '2026-01-15 23:03:51');
INSERT INTO `points_transactions` VALUES (462, 70, 19, 261, 'order_completed', 262, '咖啡订单完成: CF202601152303344037', '2026-01-15 23:03:54');
INSERT INTO `points_transactions` VALUES (463, 70, 23, 284, 'order_completed', 261, '咖啡订单完成: CF202601152303277340', '2026-01-15 23:04:34');
INSERT INTO `points_transactions` VALUES (464, 70, 50, 334, 'challenge_delivery', 261, '挑战任务【外卖尝鲜】完成奖励', '2026-01-15 23:04:34');
INSERT INTO `points_transactions` VALUES (465, 70, 40, 374, 'order_completed', 266, '咖啡订单完成: CF202601152306112123', '2026-01-15 23:06:15');
INSERT INTO `points_transactions` VALUES (466, 70, 40, 414, 'challenge_order', 266, '挑战任务【打卡达人】完成奖励', '2026-01-15 23:06:15');
INSERT INTO `points_transactions` VALUES (467, 70, 40, 454, 'order_completed', 265, '咖啡订单完成: CF202601152306055817', '2026-01-15 23:06:18');
INSERT INTO `points_transactions` VALUES (468, 70, 42, 496, 'order_completed', 264, '咖啡订单完成: CF202601152305574912', '2026-01-15 23:06:23');
INSERT INTO `points_transactions` VALUES (469, 75, 888, 888, 'birthday_gift', 342818305, '🎂 生日快乐！黑金会员专属888积分贺礼已发放', '2026-01-15 23:11:55');
INSERT INTO `points_transactions` VALUES (470, 75, 15, 903, 'order_completed', 267, '咖啡订单完成: CF202601152314433727', '2026-01-15 23:15:37');
INSERT INTO `points_transactions` VALUES (471, 75, 200, 1103, 'first_order_bonus', 267, '新用户首单奖励', '2026-01-15 23:15:37');
INSERT INTO `points_transactions` VALUES (472, 75, 27, 1130, 'order_completed', 268, '咖啡订单完成: CF202601152314511144', '2026-01-15 23:15:40');
INSERT INTO `points_transactions` VALUES (473, 75, 61, 1191, 'order_completed', 269, '咖啡订单完成: CF202601152315047083', '2026-01-15 23:15:42');
INSERT INTO `points_transactions` VALUES (474, 75, 50, 1241, 'challenge_delivery', 269, '挑战任务【外卖尝鲜】完成奖励', '2026-01-15 23:15:42');
INSERT INTO `points_transactions` VALUES (475, 75, 61, 1302, 'order_completed', 270, '咖啡订单完成: CF202601152315128170', '2026-01-15 23:15:44');
INSERT INTO `points_transactions` VALUES (476, 75, 61, 1363, 'order_completed', 271, '咖啡订单完成: CF202601152316254014', '2026-01-15 23:16:30');
INSERT INTO `points_transactions` VALUES (477, 75, 40, 1403, 'challenge_order', 271, '挑战任务【打卡达人】完成奖励', '2026-01-15 23:16:30');
INSERT INTO `points_transactions` VALUES (478, 76, 38, 38, 'order_completed', 272, '咖啡订单完成: CF202601152321194640', '2026-01-15 23:21:59');
INSERT INTO `points_transactions` VALUES (479, 76, 200, 238, 'first_order_bonus', 272, '新用户首单奖励', '2026-01-15 23:22:00');
INSERT INTO `points_transactions` VALUES (480, 76, 39, 277, 'order_completed', 273, '咖啡订单完成: CF202601152322484901', '2026-01-15 23:22:55');
INSERT INTO `points_transactions` VALUES (481, 76, 36, 313, 'order_completed', 274, '咖啡订单完成: CF202601152323076666', '2026-01-15 23:23:12');
INSERT INTO `points_transactions` VALUES (482, 76, 31, 344, 'order_completed', 275, '咖啡订单完成: CF202601152324056987', '2026-01-15 23:24:12');
INSERT INTO `points_transactions` VALUES (483, 76, 80, 424, 'challenge_newproduct', 275, '挑战任务【新品猎人】完成奖励', '2026-01-15 23:24:12');
INSERT INTO `points_transactions` VALUES (484, 46, 47, 573, 'order_completed', 278, '咖啡订单完成: CF202601152329396575', '2026-01-15 23:29:45');
INSERT INTO `points_transactions` VALUES (485, 46, 49, 622, 'order_completed', 277, '咖啡订单完成: CF202601152329327572', '2026-01-15 23:29:50');
INSERT INTO `points_transactions` VALUES (486, 46, 47, 669, 'order_completed', 279, '咖啡订单完成: CF202601152330437758', '2026-01-15 23:30:50');
INSERT INTO `points_transactions` VALUES (487, 46, 40, 709, 'challenge_order', 279, '挑战任务【打卡达人】完成奖励', '2026-01-15 23:30:50');
INSERT INTO `points_transactions` VALUES (488, 47, 40, 240, 'order_completed', 281, '咖啡订单完成: CF202601152335382875', '2026-01-15 23:35:56');
INSERT INTO `points_transactions` VALUES (489, 47, 200, 440, 'first_order_bonus', 281, '新用户首单奖励', '2026-01-15 23:35:56');
INSERT INTO `points_transactions` VALUES (490, 47, 34, 474, 'order_completed', 282, '咖啡订单完成: CF202601152335492623', '2026-01-15 23:35:58');
INSERT INTO `points_transactions` VALUES (491, 47, 38, 512, 'order_completed', 280, '咖啡订单完成: CF202601152335309396', '2026-01-15 23:36:00');
INSERT INTO `points_transactions` VALUES (492, 47, 26, 538, 'order_completed', 283, '咖啡订单完成: CF202601152336112505', '2026-01-15 23:36:17');
INSERT INTO `points_transactions` VALUES (493, 47, 40, 578, 'challenge_order', 283, '挑战任务【打卡达人】完成奖励', '2026-01-15 23:36:17');
INSERT INTO `points_transactions` VALUES (494, 47, 46, 624, 'order_completed', 284, '咖啡订单完成: CF202601152336319009', '2026-01-15 23:36:36');
INSERT INTO `points_transactions` VALUES (495, 47, 37, 661, 'order_completed', 285, '咖啡订单完成: CF202601152337201212', '2026-01-15 23:37:27');
INSERT INTO `points_transactions` VALUES (496, 47, 80, 741, 'challenge_newproduct', 285, '挑战任务【新品猎人】完成奖励', '2026-01-15 23:37:27');
INSERT INTO `points_transactions` VALUES (497, 47, 37, 778, 'order_completed', 286, '咖啡订单完成: CF202601152337349419', '2026-01-15 23:37:42');
INSERT INTO `points_transactions` VALUES (498, 49, 19, 269, 'order_completed', 287, '咖啡订单完成: CF202601152341557295', '2026-01-15 23:42:01');
INSERT INTO `points_transactions` VALUES (499, 49, 50, 319, 'challenge_delivery', 287, '挑战任务【外卖尝鲜】完成奖励', '2026-01-15 23:42:01');
INSERT INTO `points_transactions` VALUES (500, 49, 40, 359, 'order_completed', 289, '咖啡订单完成: CF202601152342227644', '2026-01-15 23:42:27');
INSERT INTO `points_transactions` VALUES (501, 49, 42, 401, 'order_completed', 288, '咖啡订单完成: CF202601152342152497', '2026-01-15 23:42:33');
INSERT INTO `points_transactions` VALUES (502, 49, 40, 441, 'challenge_order', 288, '挑战任务【打卡达人】完成奖励', '2026-01-15 23:42:33');
INSERT INTO `points_transactions` VALUES (503, 49, 40, 481, 'order_completed', 290, '咖啡订单完成: CF202601152342557176', '2026-01-15 23:43:02');
INSERT INTO `points_transactions` VALUES (504, 49, 80, 561, 'challenge_newproduct', 290, '挑战任务【新品猎人】完成奖励', '2026-01-15 23:43:02');
INSERT INTO `points_transactions` VALUES (505, 77, 9999, 9999, 'admin', NULL, '1', '2026-01-15 23:44:07');
INSERT INTO `points_transactions` VALUES (506, 77, -80, 9919, 'redeem', NULL, '积分兑换扣减', '2026-01-15 23:44:19');
INSERT INTO `points_transactions` VALUES (507, 77, -120, 9799, 'redeem', NULL, '积分兑换扣减', '2026-01-15 23:44:27');
INSERT INTO `points_transactions` VALUES (508, 77, -350, 9449, 'redeem', NULL, '积分兑换扣减', '2026-01-15 23:44:36');
INSERT INTO `points_transactions` VALUES (509, 77, -150, 9299, 'redeem', NULL, '积分兑换扣减', '2026-01-15 23:44:38');
INSERT INTO `points_transactions` VALUES (510, 77, -800, 8499, 'redeem', NULL, '积分兑换扣减', '2026-01-15 23:45:16');
INSERT INTO `points_transactions` VALUES (511, 77, -3500, 4999, 'redeem', NULL, '积分兑换扣减', '2026-01-15 23:46:32');
INSERT INTO `points_transactions` VALUES (512, 77, -600, 4399, 'redeem', NULL, '积分兑换扣减', '2026-01-15 23:47:46');
INSERT INTO `points_transactions` VALUES (513, 47, 51, 829, 'order_completed', 291, '咖啡订单完成: CF202601160815433698', '2026-01-16 08:16:49');
INSERT INTO `points_transactions` VALUES (514, 47, 50, 879, 'challenge_delivery', 291, '挑战任务【外卖尝鲜】完成奖励', '2026-01-16 08:16:49');
INSERT INTO `points_transactions` VALUES (515, 47, 48, 927, 'order_completed', 292, '咖啡订单完成: CF202601160816598434', '2026-01-16 08:17:04');
INSERT INTO `points_transactions` VALUES (516, 47, 85, 1012, 'order_completed', 293, '咖啡订单完成: CF202601160817123508', '2026-01-16 08:17:18');
INSERT INTO `points_transactions` VALUES (517, 47, 60, 1072, 'challenge_morning', 293, '挑战任务【晨间唤醒】完成奖励', '2026-01-16 08:17:19');
INSERT INTO `points_transactions` VALUES (518, 79, 2, 2, 'signin', NULL, '每日签到（连续7天）', '2026-01-16 09:03:36');
INSERT INTO `points_transactions` VALUES (519, 78, 18, 18, 'order_completed', 294, '咖啡订单完成: CF202601160905058232', '2026-01-16 09:05:16');
INSERT INTO `points_transactions` VALUES (520, 78, 50, 68, 'upgrade_reward', 892793853, '黄金晋升奖励', '2026-01-16 09:05:17');
INSERT INTO `points_transactions` VALUES (521, 78, 200, 268, 'first_order_bonus', 294, '新用户首单奖励', '2026-01-16 09:05:17');
INSERT INTO `points_transactions` VALUES (522, 79, 1000, 1002, 'admin', NULL, '1', '2026-01-16 09:19:33');
INSERT INTO `points_transactions` VALUES (523, 79, -72, 930, 'redeem', NULL, '积分兑换扣减', '2026-01-16 09:19:45');
INSERT INTO `points_transactions` VALUES (524, 79, -108, 822, 'redeem', NULL, '积分兑换扣减', '2026-01-16 09:19:47');
INSERT INTO `points_transactions` VALUES (525, 79, 61, 883, 'order_completed', 297, '咖啡订单完成: CF202601192245181812', '2026-01-19 22:45:47');
INSERT INTO `points_transactions` VALUES (526, 79, 1688, 2571, 'upgrade_reward', 1195270683, '黑金晋升奖励', '2026-01-19 22:45:48');
INSERT INTO `points_transactions` VALUES (527, 79, 200, 2771, 'first_order_bonus', 297, '新用户首单奖励', '2026-01-19 22:45:48');
INSERT INTO `points_transactions` VALUES (528, 79, 37, 2808, 'order_completed', 296, '咖啡订单完成: CF202601192245114213', '2026-01-19 22:45:49');
INSERT INTO `points_transactions` VALUES (529, 79, 37, 2845, 'order_completed', 299, '咖啡订单完成: CF202601192247574105', '2026-01-19 22:49:34');
INSERT INTO `points_transactions` VALUES (530, 79, 0, 2845, 'monthly_benefit_202601', NULL, '领取202601月度等级权益:black', '2026-01-19 22:51:54');
INSERT INTO `points_transactions` VALUES (531, 19, 22, 22, 'order_completed', 303, '咖啡订单完成: CF202607071431168114', '2026-07-07 14:31:33');
INSERT INTO `points_transactions` VALUES (532, 19, 32, 54, 'order_completed', 304, '咖啡订单完成: CF202607071435065714', '2026-07-07 14:35:12');
INSERT INTO `points_transactions` VALUES (533, 19, 22, 76, 'order_completed', 305, '咖啡订单完成: CF202607081328015064', '2026-07-08 13:28:08');
INSERT INTO `points_transactions` VALUES (534, 19, 40, 116, 'challenge_order', 305, '挑战任务【打卡达人】完成奖励', '2026-07-08 13:28:08');
INSERT INTO `points_transactions` VALUES (535, 19, 30, 146, 'order_completed', 306, '咖啡订单完成: CF202607081339358960', '2026-07-08 13:39:43');
INSERT INTO `points_transactions` VALUES (536, 19, 31, 177, 'order_completed', 307, '咖啡订单完成: CF202607081344419793', '2026-07-08 13:44:51');
INSERT INTO `points_transactions` VALUES (537, 19, 50, 227, 'challenge_delivery', 307, '挑战任务【外卖尝鲜】完成奖励', '2026-07-08 13:44:51');
INSERT INTO `points_transactions` VALUES (538, 19, 25, 252, 'order_completed', 308, '咖啡订单完成: CF202607081346552288', '2026-07-08 13:47:03');
INSERT INTO `points_transactions` VALUES (539, 19, 0, 252, 'birthday_gift', NULL, '生日快乐！获赠单饮品5折券(限标准杯)', '2026-07-08 14:27:34');

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
-- Records of signin_records
-- ----------------------------
INSERT INTO `signin_records` VALUES (5, 19, '2025-12-21', 1, 10, '2025-12-21 17:00:23');
INSERT INTO `signin_records` VALUES (6, 20, '2025-12-21', 1, 10, '2025-12-21 20:52:00');
INSERT INTO `signin_records` VALUES (7, 21, '2025-12-21', 1, 10, '2025-12-21 21:18:29');
INSERT INTO `signin_records` VALUES (8, 22, '2025-12-21', 1, 799, '2025-12-21 21:49:26');
INSERT INTO `signin_records` VALUES (9, 23, '2025-12-21', 1, 10, '2025-12-21 23:27:43');
INSERT INTO `signin_records` VALUES (10, 21, '2025-12-22', 2, 32, '2025-12-22 00:02:46');
INSERT INTO `signin_records` VALUES (11, 19, '2025-12-22', 2, 32, '2025-12-22 16:19:04');
INSERT INTO `signin_records` VALUES (12, 19, '2025-12-23', 3, 34, '2025-12-23 10:46:00');
INSERT INTO `signin_records` VALUES (13, 39, '2025-12-28', 1, 25, '2025-12-28 00:20:47');
INSERT INTO `signin_records` VALUES (14, 22, '2025-12-28', 1, 0, '2025-12-28 11:09:44');
INSERT INTO `signin_records` VALUES (15, 39, '2025-12-31', 1, 20, '2025-12-31 16:13:31');
INSERT INTO `signin_records` VALUES (24, 41, '2026-01-01', 8, 10, '2026-01-01 01:24:03');
INSERT INTO `signin_records` VALUES (25, 41, '2026-01-02', 9, 15, '2026-01-02 10:40:29');
INSERT INTO `signin_records` VALUES (34, 43, '2026-01-03', 8, 2, '2026-01-03 22:16:32');
INSERT INTO `signin_records` VALUES (35, 43, '2026-01-04', 9, 2, '2026-01-04 00:20:04');
INSERT INTO `signin_records` VALUES (36, 41, '2026-01-04', 1, 2, '2026-01-04 17:00:03');
INSERT INTO `signin_records` VALUES (37, 43, '2026-01-05', 10, 2, '2026-01-05 00:28:23');
INSERT INTO `signin_records` VALUES (38, 41, '2026-01-05', 2, 2, '2026-01-05 01:27:00');
INSERT INTO `signin_records` VALUES (39, 43, '2026-01-07', 1, 2, '2026-01-07 22:38:44');
INSERT INTO `signin_records` VALUES (43, 43, '2026-01-10', 8, 2, '2026-01-10 15:00:40');
INSERT INTO `signin_records` VALUES (44, 48, '2026-01-11', 1, 2, '2026-01-11 16:28:50');
INSERT INTO `signin_records` VALUES (45, 49, '2026-01-11', 1, 2, '2026-01-11 16:33:14');
INSERT INTO `signin_records` VALUES (46, 62, '2026-01-12', 1, 2, '2026-01-12 23:02:41');
INSERT INTO `signin_records` VALUES (47, 67, '2026-01-13', 1, 2, '2026-01-13 18:17:33');
INSERT INTO `signin_records` VALUES (48, 68, '2026-01-14', 1, 2, '2026-01-14 19:15:07');
INSERT INTO `signin_records` VALUES (49, 68, '2026-01-15', 2, 2, '2026-01-15 16:34:09');
INSERT INTO `signin_records` VALUES (52, 69, '2026-01-15', 7, 2, '2026-01-15 19:40:04');
INSERT INTO `signin_records` VALUES (53, 73, '2026-01-15', 1, 2, '2026-01-15 22:43:25');
INSERT INTO `signin_records` VALUES (54, 79, '2026-01-16', 7, 2, '2026-01-16 09:03:36');

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
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES ('level_thresholds', '{\"silver\": 500, \"gold\": 1500, \"diamond\": 4000, \"black\": 9000}', '会员等级门槛', '2026-01-07 23:36:00');
INSERT INTO `system_config` VALUES ('points_rate', '{\"basic\": 1.0, \"silver\": 1.1, \"gold\": 1.2, \"diamond\": 1.3, \"black\": 1.5}', '会员消费积分倍率', '2026-01-07 23:35:33');
INSERT INTO `system_config` VALUES ('signin_rewards', '[2,2,2,2,2,2,2]', '签到奖励规则（第1-7天）', '2026-01-07 23:35:22');

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
-- Records of user_addresses
-- ----------------------------
INSERT INTO `user_addresses` VALUES (67, 19, '苏瑞鑫', '15168226911', '浙江省', '温州市', '文成县', '玉海街道', 1, '2025-12-21 17:38:07', '2025-12-21 17:38:07');
INSERT INTO `user_addresses` VALUES (69, 20, '苏瑞鑫', '15164141411', '安徽省', '宿州市', '埇桥区', '玉海街道', 1, '2025-12-21 21:05:20', '2025-12-21 21:05:20');
INSERT INTO `user_addresses` VALUES (70, 21, '苏瑞鑫', '15164141411', '安徽省', '黄山市', '屯溪区', '玉海街道', 1, '2025-12-21 21:24:10', '2025-12-21 21:24:10');
INSERT INTO `user_addresses` VALUES (71, 22, '苏瑞鑫', '15164141411', '安徽省', '宿州市', '埇桥区', '玉海街道', 1, '2025-12-21 21:52:40', '2025-12-21 21:52:40');
INSERT INTO `user_addresses` VALUES (72, 19, '苏瑞鑫', '15168226973', '江苏省', '扬州市', '邗江区', '玉海街道', 0, '2025-12-22 18:34:11', '2025-12-22 18:34:11');
INSERT INTO `user_addresses` VALUES (73, 23, '凌英', '13918999906', '辽宁省', '福都市', '兴海县', '饶桥6号', 0, '2025-12-23 17:31:53', '2025-12-23 17:31:53');
INSERT INTO `user_addresses` VALUES (75, 43, '苏瑞鑫', '15168226973', '安徽省', '六安市', '市辖区', '玉海街道', 0, '2026-01-02 23:33:05', '2026-01-02 23:33:05');
INSERT INTO `user_addresses` VALUES (76, 41, '苏瑞鑫', '15168226973', '安徽省', '宿州市', '市辖区', '玉海街道', 0, '2026-01-04 19:07:35', '2026-01-04 19:07:35');
INSERT INTO `user_addresses` VALUES (77, 50, '苏瑞鑫', '14412345678', '福建省', '福州市', '平潭县', '1', 0, '2026-01-10 23:02:13', '2026-01-10 23:02:13');
INSERT INTO `user_addresses` VALUES (78, 49, '苏瑞鑫', '15112345678', '浙江省', '温州市', '乐清市', 'Oregon State University', 0, '2026-01-11 14:34:22', '2026-01-11 14:34:22');
INSERT INTO `user_addresses` VALUES (79, 52, '苏瑞鑫', '15168226973', '福建省', '福州市', '永泰县', '玉海街道', 0, '2026-01-12 11:03:23', '2026-01-12 11:03:23');
INSERT INTO `user_addresses` VALUES (80, 56, '苏瑞鑫', '15612345678', '安徽省', '宿州市', '砀山县', 'Oregon State University', 0, '2026-01-12 15:46:56', '2026-01-12 15:46:56');
INSERT INTO `user_addresses` VALUES (81, 59, '苏瑞鑫', '15212345678', '福建省', '福州市', '平潭县', 'Oregon State University', 1, '2026-01-12 20:44:08', '2026-01-12 20:44:08');
INSERT INTO `user_addresses` VALUES (82, 62, '苏瑞鑫', '19957147138', '福建省', '厦门市', '思明区', 'Oregon State University', 0, '2026-01-12 23:03:54', '2026-01-12 23:03:54');
INSERT INTO `user_addresses` VALUES (83, 67, '苏瑞鑫', '16812345678', '福建省', '厦门市', '思明区', 'Oregon State University', 0, '2026-01-13 18:19:37', '2026-01-13 18:19:37');
INSERT INTO `user_addresses` VALUES (85, 71, '苏瑞鑫', '15168226973', '福建省', '厦门市', '海沧区', '玉海街道', 0, '2026-01-15 20:07:46', '2026-01-15 20:07:46');
INSERT INTO `user_addresses` VALUES (86, 74, '苏瑞鑫', '15168226973', '福建省', '厦门市', '思明区', '玉海街道', 0, '2026-01-15 22:51:20', '2026-01-15 22:51:20');
INSERT INTO `user_addresses` VALUES (87, 70, '苏瑞鑫', '15312345678', '福建省', '厦门市', '思明区', 'Oregon State University', 0, '2026-01-15 23:03:24', '2026-01-15 23:03:24');
INSERT INTO `user_addresses` VALUES (88, 75, '苏瑞鑫', '15168226973', '福建省', '福州市', '平潭县', '玉海街道', 0, '2026-01-15 23:14:37', '2026-01-15 23:14:37');
INSERT INTO `user_addresses` VALUES (89, 76, '苏瑞鑫', '15112345678', '浙江省', '杭州市', '淳安县', 'Oregon State University', 0, '2026-01-15 23:22:45', '2026-01-15 23:22:45');
INSERT INTO `user_addresses` VALUES (90, 47, '苏瑞鑫', '15112345678', '浙江省', '杭州市', '建德市', 'Oregon State University', 0, '2026-01-15 23:37:18', '2026-01-15 23:37:18');
INSERT INTO `user_addresses` VALUES (91, 77, '苏瑞鑫', '15168226973', '福建省', '厦门市', '思明区', '玉海街道', 0, '2026-01-15 23:46:11', '2026-01-15 23:46:11');
INSERT INTO `user_addresses` VALUES (92, 79, '苏瑞鑫', '15112345678', '安徽省', '宿州市', '埇桥区', 'Oregon State University', 0, '2026-01-16 09:20:44', '2026-01-16 09:20:44');

-- ----------------------------
-- View structure for v_points_balance_check
-- ----------------------------
DROP VIEW IF EXISTS `v_points_balance_check`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_points_balance_check` AS select `m`.`user_id` AS `user_id`,`m`.`current_points` AS `member_balance`,coalesce(sum(`l`.`remaining`),0) AS `lots_balance`,(`m`.`current_points` - coalesce(sum(`l`.`remaining`),0)) AS `diff`,count(`l`.`id`) AS `lot_count` from (`member_info` `m` left join `points_lots` `l` on(((`l`.`user_id` = `m`.`user_id`) and (`l`.`remaining` > 0)))) group by `m`.`user_id`,`m`.`current_points` having (`diff` <> 0);

SET FOREIGN_KEY_CHECKS = 1;
