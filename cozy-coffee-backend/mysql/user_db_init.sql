-- =============================================
-- 用户服务独立数据库 (cozy_user)
-- 完全匹配 cozy_coffee.users 表结构
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS cozy_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cozy_user;

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `member_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会员编号（随机生成）',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名（登录用）',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '咖啡爱好者' COMMENT '昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '/images/default-avatar.png' COMMENT '头像URL',
  `invite_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户专属邀请码（8位字母数字）',
  `invited_by` bigint(0) NULL DEFAULT NULL COMMENT '邀请人用户ID',
  `invited_at` datetime(0) NULL DEFAULT NULL COMMENT '填写邀请码时间',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '注册时间',
  `updated_at` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE,
  UNIQUE INDEX `member_code`(`member_code`) USING BTREE,
  UNIQUE INDEX `invite_code`(`invite_code`) USING BTREE,
  INDEX `idx_username`(`username`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  INDEX `idx_invite_code`(`invite_code`) USING BTREE,
  INDEX `idx_invited_by`(`invited_by`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- 生成邀请码的函数
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

SELECT '✅ 用户数据库 cozy_user 创建完成！' AS status;
