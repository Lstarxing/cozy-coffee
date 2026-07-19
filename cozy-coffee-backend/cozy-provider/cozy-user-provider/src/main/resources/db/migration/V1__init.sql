/*
 Navicat Premium Data Transfer

 Source Server         : cozycoffee开发
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : cozy_user

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 10/07/2026 10:16:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `member_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会员编号（随机生成）',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名（登录用）',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '咖啡爱好者' COMMENT '昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '/images/default-avatar.png' COMMENT '头像URL',
  `invite_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户专属邀请码（8位字母数字）',
  `invited_by` bigint NULL DEFAULT NULL COMMENT '邀请人用户ID',
  `invited_at` datetime NULL DEFAULT NULL COMMENT '填写邀请码时间',
  `invite_reward_granted` tinyint(1) NULL DEFAULT 0 COMMENT '邀请奖励是否已发放（被邀请人首单后触发）',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色: user-普通用户, admin-管理员',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT 'active-正常, disabled-禁用',
  `token_version` int NOT NULL DEFAULT 0 COMMENT 'Token版本号，用于禁用时使Token失效',
  `birthday` date NULL DEFAULT NULL COMMENT '用户生日(完整日期,年份可固定为2000或实际年份)',
  `birthday_set_at` datetime NULL DEFAULT NULL COMMENT '生日设置时间',
  `next_birthday_reset_at` datetime NULL DEFAULT NULL COMMENT '下次可修改生日时间(365天后)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `member_code`(`member_code` ASC) USING BTREE,
  UNIQUE INDEX `invite_code`(`invite_code` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_invite_code`(`invite_code` ASC) USING BTREE,
  INDEX `idx_invited_by`(`invited_by` ASC) USING BTREE,
  INDEX `idx_users_role`(`role` ASC) USING BTREE,
  INDEX `idx_users_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 80 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;
-- ----------------------------
-- Function structure for generate_invite_code
-- ----------------------------
DROP FUNCTION IF EXISTS `generate_invite_code`;
delimiter ;;
CREATE FUNCTION `generate_invite_code`()
 RETURNS varchar(8) CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
  DETERMINISTIC
BEGIN
    DECLARE chars VARCHAR(36) DEFAULT 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    DECLARE result VARCHAR(8) DEFAULT '';
    DECLARE i INT DEFAULT 0;
    WHILE i < 8 DO
        SET result = CONCAT(result, SUBSTRING(chars, FLOOR(1 + RAND() * 32), 1));
        SET i = i + 1;
    END WHILE;
    RETURN result;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
