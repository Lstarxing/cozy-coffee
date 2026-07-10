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
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (19, '94337998', '3187979459@qq.com', '$2a$10$TsAGe74bqJ6P7/FGs9d.Ceg2gocH85RKcwa/fCFjOffq.LbpoOPrm', '苏瑞鑫', '/images/default-avatar.png', NULL, 22, '2025-12-21 23:51:05', 0, NULL, '3187979459@qq.com', '2025-12-21 16:57:15', '2025-12-31 14:45:09', 'user', 'active', 0, '2002-07-03', '2026-07-08 14:27:34', '2027-07-08 14:27:34');
INSERT INTO `users` VALUES (20, '25864248', 'suruixin123@gmail.com', '$2a$10$SlaGOQ8dw2k6eh37Y9gvQOX3U/eQXSWVz5SLijg3b/ttAjZboZZX.', 'COZY-25864248', '/images/default-avatar.png', NULL, NULL, NULL, 0, '15158820465', 'suruixin123@gmail.com', '2025-12-21 20:51:36', '2025-12-21 20:51:36', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (21, '62702839', '151588865454', '$2a$10$DOdSqVSXFHUGHWE0mZ07yec1RiNFBX3pdj2xgtEsfgY8UXmRn3efW', 'COZY-62702839', '/images/default-avatar.png', NULL, 22, '2025-12-21 23:54:09', 0, '15168587946', '3187979459@qq.com', '2025-12-21 21:17:23', '2025-12-21 21:17:23', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (22, '40783142', '3187979458@qq.com', '$2a$10$KASogloUmIsvun21VyM2b.UOYjyz2MYIcuuWEpjM7vybx53ad3bBu', 'COZY-40783142', '/images/default-avatar.png', '5FQKA8BP', NULL, NULL, 0, '15111111111', '3187979458@qq.com', '2025-12-21 21:48:43', '2025-12-21 23:43:25', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (23, '91250813', '1230204171@zust.edu.cn', '$2a$10$Fj4KoB9ET5ZU9UMyi76nf.ZTOgi6KZv2Ese6xez1CYSvZWvqOf55O', 'COZY-91250813', '/images/default-avatar.png', '5FQKA8BK', 22, '2025-12-21 23:43:29', 0, '15168226973', '1230204171@zust.edu.cn', '2025-12-21 23:16:33', '2025-12-21 23:16:33', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (38, '', 'testadmin', '$2a$10$/RnCH09qWnU5mb4/.J5l0ewYy/jSuVX2m1oPsalb3CSr26m3NjUb.', '测试管理员', '/images/default-avatar.png', NULL, NULL, NULL, 0, '13800000001', NULL, '2025-12-24 14:23:19', '2025-12-24 14:29:30', 'admin', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (41, '73707151', '13812345678', '$2a$10$HxExiP89ZsrfXaMjTlox4eQ7j704GJYaMMX6JDPuREcKJOT9ndxHG', '龙梓豪', '/images/default-avatar.png', 'JKFXBZUU', 43, '2026-01-04 00:21:03', 0, '13812345678', NULL, '2025-12-31 16:01:08', '2025-12-31 16:01:08', 'user', 'active', 0, '1995-12-31', '2025-12-31 23:45:39', '2026-12-31 23:45:39');
INSERT INTO `users` VALUES (43, '75071050', '15345778212', '$2a$10$h7/5zpGE0HabbdmlYa7SYOmonz8tTctUf77kgGrTKjLIxxN.Y0H2C', '李白', '/images/default-avatar.png', 'JVXW26XN', 41, '2026-01-04 00:26:27', 0, '15345778212', NULL, '2026-01-02 11:23:03', '2026-01-08 00:17:55', 'user', 'active', 0, '2000-01-01', '2026-01-08 00:19:41', '2027-01-08 00:19:41');
INSERT INTO `users` VALUES (44, '29639212', '13712345678', '$2a$10$xQfPml6dFyNf7SfRvynt8uK2YpAYm3kbtuGdG8ZH7xe6RWUGyOlOO', 'COZY-29639212', '/images/default-avatar.png', 'WWZ37Y9Y', NULL, NULL, 0, '13712345678', NULL, '2026-01-10 17:32:29', '2026-01-10 17:32:29', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (45, '50250646', '13912345678', '$2a$10$CJOBs9Avxp2/Da9m3OH4gunQggJscxhT4M.W4OthJXSrsg8bHwkBC', 'COZY-50250646', '/images/default-avatar.png', 'VD72SRBK', 44, '2026-01-10 17:59:09', 0, '13912345678', NULL, '2026-01-10 17:59:09', '2026-01-10 17:59:09', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (46, '03244432', '14012345678', '$2a$10$rWvheXT9qlrdsUjWB2BwXuOJDLS6CoLpIoUDWqlPBsZ7JozEg1Tfy', 'COZY-03244432', '/images/default-avatar.png', 'DG9QNDAT', 44, '2026-01-10 18:33:22', 1, '14012345678', NULL, '2026-01-10 18:33:22', '2026-01-10 18:33:22', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (47, '41239858', '14112345678', '$2a$10$B7v.VQrECbEW65oa9L5ig.u4Eml30N5D9elrTsCWRhHfc.cR4Ztca', 'COZY-41239858', '/images/default-avatar.png', 'PMFSZVT3', 44, '2026-01-10 19:07:24', 1, '14112345678', NULL, '2026-01-10 19:07:24', '2026-01-10 19:07:24', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (48, '48780660', '14212345678', '$2a$10$G/zbYfjqGzhQszQmPMFF5enMEUR0PdXtNW48UWwu.hdMrt/Xf4scW', 'COZY-48780660', '/images/default-avatar.png', 'MN8SF7V4', 44, '2026-01-10 19:19:25', 1, '14212345678', NULL, '2026-01-10 19:19:25', '2026-01-10 19:19:25', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (49, '26540979', '14312345678', '$2a$10$PQi2lJZ4p4L/hWIgWptZy.Orwuhr4E2qIyaK0hUa6MXdCxP053yUG', 'COZY-26540979', '/images/default-avatar.png', 'ACK4FN9W', 44, '2026-01-10 19:24:05', 1, '14312345678', NULL, '2026-01-10 19:24:05', '2026-01-10 19:24:05', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (50, '50077937', '14412345678', '$2a$10$fTz5/63fKI5IwzTW3kUUlu09e.EUAMQkiVwyh2GwphGe5SkuED9.S', 'COZY-50077937', '/images/default-avatar.png', 'T967UJ8J', 43, '2026-01-10 22:18:39', 1, '14412345678', NULL, '2026-01-10 22:18:39', '2026-01-10 22:18:39', 'user', 'active', 0, '2000-01-01', '2026-01-10 23:29:53', '2027-01-10 23:29:53');
INSERT INTO `users` VALUES (51, '28603605', '15012345678', '$2a$10$ZilWd3lQ7SRNL1VndCoc9OpF.PpqAZs2ErNNeyLUwTQMmpi5wf0Ee', 'COZY-28603605', '/images/default-avatar.png', 'N3J9WCDK', 43, '2026-01-11 18:07:11', 0, '15012345678', NULL, '2026-01-11 18:07:10', '2026-01-11 18:07:10', 'user', 'active', 0, '2000-01-01', '2026-01-12 14:09:40', '2027-01-12 14:09:40');
INSERT INTO `users` VALUES (52, '53569451', '15212345678', '$2a$10$v3BvlmoW9GxPVXEjmWan9O6Yq7HpZv23IJ.nC2sO/eAd8nd7TS9Iu', 'COZY-53569451', '/images/default-avatar.png', 'NR3F27NK', NULL, NULL, 0, '15212345678', NULL, '2026-01-12 10:45:01', '2026-01-12 10:45:01', 'user', 'active', 0, '2000-01-01', '2026-01-12 13:58:10', '2027-01-12 13:58:10');
INSERT INTO `users` VALUES (53, '02008121', '15312345678', '$2a$10$lsJZWZLaC2gWq1r.nqEOoe4AdLi.3Jd7uNu2KwH/BWEsa8uHNhByK', 'COZY-02008121', '/images/default-avatar.png', '4385XR3R', 69, '2026-01-15 19:37:04', 1, '15312345678', NULL, '2026-01-12 14:10:44', '2026-01-12 14:10:44', 'user', 'active', 0, '2000-01-01', '2026-01-12 14:10:55', '2027-01-12 14:10:55');
INSERT INTO `users` VALUES (54, '57200178', '15412345678', '$2a$10$386v4BM8rNseptty.DQy6..ZYNFkFj8syzA0UtYpw1IR6db0fois2', 'COZY-57200178', '/images/default-avatar.png', 'CFD6QQNE', NULL, NULL, 0, '15412345678', NULL, '2026-01-12 14:47:44', '2026-01-12 14:47:44', 'user', 'active', 0, '2000-01-01', '2026-01-12 14:48:28', '2027-01-12 14:48:28');
INSERT INTO `users` VALUES (55, '38391391', '15512345678', '$2a$10$P6kNS86zXCy/t22C3kUjvOVfw.7SKcUy45BdWbUw3nvun5pW6VyUC', 'COZY-38391391', '/images/default-avatar.png', 'G49WEUB3', NULL, NULL, 0, '15512345678', NULL, '2026-01-12 14:59:06', '2026-01-12 14:59:06', 'user', 'active', 0, '2000-01-01', '2026-01-12 14:59:15', '2027-01-12 14:59:15');
INSERT INTO `users` VALUES (56, '03226693', '15612345678', '$2a$10$kl7jYxjUVpDu0ejGrRr57.ugvslCXTuTylmFTm/kOadYLhqLgHgHq', 'COZY-03226693', '/images/default-avatar.png', '3MT394ER', NULL, NULL, 0, '15612345678', NULL, '2026-01-12 15:42:20', '2026-01-12 15:42:20', 'user', 'active', 0, '2000-01-01', '2026-01-12 16:59:22', '2027-01-12 16:59:22');
INSERT INTO `users` VALUES (57, '52611885', '15712345678', '$2a$10$w7qcEinVz538bQSdDbJ1vurGAaMtMl.78bAodQCNgvz23Ea1p3hOm', 'COZY-52611885', '/images/default-avatar.png', 'USEPZGWH', NULL, NULL, 0, '15712345678', NULL, '2026-01-12 17:18:20', '2026-01-12 17:18:20', 'user', 'active', 0, '2000-01-01', '2026-01-12 17:27:53', '2027-01-12 17:27:53');
INSERT INTO `users` VALUES (58, '80424737', '15812345678', '$2a$10$gcW4KoGhaaZZD1O9eKf1KOvc0t1a4tQChOG1P.OgkQ4z9fCZLP7a.', 'COZY-80424737', '/images/default-avatar.png', 'CRT7RM58', NULL, NULL, 0, '15812345678', NULL, '2026-01-12 18:48:54', '2026-01-12 18:48:54', 'user', 'active', 0, '2000-01-12', '2026-01-12 18:50:35', '2027-01-12 18:50:35');
INSERT INTO `users` VALUES (59, '33130761', '15912345678', '$2a$10$ecZWRRvSZhXmgfTvXkBS0.Tc6/HCE9AL1CqSLJN/jOO/qm8k84gem', 'COZY-33130761', '/images/default-avatar.png', 'DMXBA6MX', NULL, NULL, 0, '15912345678', NULL, '2026-01-12 20:12:34', '2026-01-12 20:12:34', 'user', 'active', 0, '2000-01-18', '2026-01-12 20:44:58', '2027-01-12 20:44:58');
INSERT INTO `users` VALUES (60, '77129162', '16012345678', '$2a$10$LD7rB6iGYhwmOwIoCA4TAOz87gdnGQW2wQ6KNGmbDuI25nvIrXIxO', 'COZY-77129162', '/images/default-avatar.png', '44H8K6M4', NULL, NULL, 0, '16012345678', NULL, '2026-01-12 21:44:00', '2026-01-12 21:44:00', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (61, '30363831', '16112346578', '$2a$10$iYVLaKybriI0P9DUswB8BOs8KPUYpJ7RRFtBgCAnvYncyPF.NIs/O', 'COZY-30363831', '/images/default-avatar.png', 'T5QCERVG', NULL, NULL, 0, '16112346578', NULL, '2026-01-12 22:05:01', '2026-01-12 22:05:01', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (62, '32899392', '16212345678', '$2a$10$Pi0rULR0h9czSb0ZPZ47KOPNRtkt.MP4WqTQGVhycSfWrIY12OgW2', 'COZY-32899392', '/images/default-avatar.png', 'GWGXMT9A', NULL, NULL, 0, '16212345678', NULL, '2026-01-12 22:31:25', '2026-01-12 22:31:25', 'user', 'active', 0, '2000-01-01', '2026-01-13 13:35:28', '2027-01-13 13:35:28');
INSERT INTO `users` VALUES (63, '71042802', '16312345678', '$2a$10$YFuzu7AFTHeoZ6GmShCtquSMS1Zfo0KVoe1Z9LFVTnK7zvNf/blmy', 'COZY-71042802', '/images/default-avatar.png', 'JEAYQWB5', NULL, NULL, 0, '16312345678', NULL, '2026-01-13 14:08:25', '2026-01-13 14:08:25', 'user', 'active', 0, '2000-01-01', '2026-01-13 14:09:38', '2027-01-13 14:09:38');
INSERT INTO `users` VALUES (64, '57126023', '16412345678', '$2a$10$rZxGXk0H1uG9jbpzy9dmhOH9ZJ2PXgwb.xGZ/gP6HpaXhmh97F5xq', 'COZY-57126023', '/images/default-avatar.png', 'D5GDM6G6', NULL, NULL, 0, '16412345678', NULL, '2026-01-13 15:24:51', '2026-01-13 15:24:51', 'user', 'active', 0, '2000-01-01', '2026-01-13 15:53:19', '2027-01-13 15:53:19');
INSERT INTO `users` VALUES (65, '29843276', '16512345678', '$2a$10$WKY0Tu4TkqcRBP86qjoMy.LqsB0xZ3d1VclsgwUTcR5YVbCRZ34WW', 'COZY-29843276', '/images/default-avatar.png', 'TBQ7MS4G', NULL, NULL, 0, '16512345678', NULL, '2026-01-13 16:42:37', '2026-01-13 16:42:37', 'user', 'active', 0, '2000-01-01', '2026-01-13 16:44:08', '2027-01-13 16:44:08');
INSERT INTO `users` VALUES (66, '42665952', '16612345678', '$2a$10$qgTKW3UsWkxKCQZO/.Gm0ulp9BQJXbmSmTvkW2oYfmnxTkUrnqRWy', 'COZY-42665952', '/images/default-avatar.png', 'GH9E7WKU', NULL, NULL, 0, '16612345678', NULL, '2026-01-13 17:25:19', '2026-01-13 17:25:19', 'user', 'active', 0, '2000-01-01', '2026-01-13 18:06:30', '2027-01-13 18:06:30');
INSERT INTO `users` VALUES (67, '88254724', '16712346578', '$2a$10$RTFy92dwmkAMDEQoOyw8P.b3QQZdG8IAR8Lh151aQ5e2g/Rd7UlaO', 'COZY-88254724', '/images/default-avatar.png', 'S96VPV67', NULL, NULL, 0, '16712346578', NULL, '2026-01-13 18:15:50', '2026-01-13 18:15:50', 'user', 'active', 0, '2000-01-01', '2026-01-13 18:18:57', '2027-01-13 18:18:57');
INSERT INTO `users` VALUES (68, '98671172', '16912345678', '$2a$10$F19Har2mszxgRRG8hmBPr.XTTDw62GBhLhslZ.bYzuT5i2nrhNHOG', 'COZY-98671172', '/images/default-avatar.png', 'R6FSMVVW', NULL, NULL, 0, '16912345678', '3187979457@qq.com', '2026-01-13 18:31:38', '2026-01-13 18:31:38', 'user', 'active', 0, '2000-01-01', '2026-01-13 18:34:35', '2027-01-13 18:34:35');
INSERT INTO `users` VALUES (69, '48699802', '17012345678', '$2a$10$/t2fKIwNh94ViNft3Polt.K2Yf03ry.iYzWSnqVRezJGqQd.YlmL2', 'COZY-48699802', '/images/default-avatar.png', '8QMM8BGM', NULL, NULL, 0, '17012345678', NULL, '2026-01-15 17:35:27', '2026-01-15 17:35:27', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (70, '42233959', '17212345678', '$2a$10$9/QNwTvk6JXNMLy2YJ.dB.fFC.TWPMNGCQ0g/L0GZMHixG//S1Pmu', 'COZY-42233959', '/images/default-avatar.png', 'EA52XTAS', NULL, NULL, 0, '17212345678', NULL, '2026-01-15 19:44:35', '2026-01-15 19:44:35', 'user', 'active', 0, '2000-01-01', '2026-01-15 19:53:17', '2027-01-15 19:53:17');
INSERT INTO `users` VALUES (71, '80259643', '17312345678', '$2a$10$t2gQ/gZTtIQ8FelL2nTqgOHJhpSgWs3yuTmDqwLOgKHNKVYhKWmWi', 'COZY-80259643', '/images/default-avatar.png', 'H76PYE6E', NULL, NULL, 0, '17312345678', NULL, '2026-01-15 20:05:08', '2026-01-15 20:05:08', 'user', 'active', 0, '2000-01-01', '2026-01-15 20:06:56', '2027-01-15 20:06:56');
INSERT INTO `users` VALUES (72, '45232227', '17412345678', '$2a$10$ljs9cWF4Ay5bZrZ0awlOCuEb0TtRWOevk7ME8H6RsuY7WKgHI7CYq', 'COZY-45232227', '/images/default-avatar.png', 'WEMXHKM6', NULL, NULL, 0, '17412345678', NULL, '2026-01-15 20:37:42', '2026-01-15 20:37:42', 'user', 'active', 0, '2000-01-01', '2026-01-15 20:46:46', '2027-01-15 20:46:46');
INSERT INTO `users` VALUES (73, '31296189', '17512345678', '$2a$10$M7shiK2we.HNXuEzZXGxG.Cy37SJU/RabFe./SUZzT2bQhE.Dkiyy', 'COZY-31296189', '/images/default-avatar.png', '6HC9YC54', NULL, NULL, 0, '17512345678', NULL, '2026-01-15 21:11:02', '2026-01-15 21:11:02', 'user', 'active', 0, '2000-01-01', '2026-01-15 22:36:53', '2027-01-15 22:36:53');
INSERT INTO `users` VALUES (74, '15813414', '17612345678', '$2a$10$MGfNaRjPHGhBMamnjlu6keznwoZcIeasG3x3pk6nrZ/PUne9TZCny', 'COZY-15813414', '/images/default-avatar.png', 'FUAAR3HR', NULL, NULL, 0, '17612345678', NULL, '2026-01-15 22:46:17', '2026-01-15 22:46:17', 'user', 'active', 0, '2000-01-01', '2026-01-15 23:10:08', '2027-01-15 23:10:08');
INSERT INTO `users` VALUES (75, '45688156', '17712345678', '$2a$10$pmBwiZjEKz8M9Ob.YOn7t.Wp6t0kE0ZboCnRoTT17NWPXWZBTODGq', 'COZY-45688156', '/images/default-avatar.png', 'TBW9GYUM', NULL, NULL, 0, '17712345678', NULL, '2026-01-15 23:11:21', '2026-01-15 23:11:21', 'user', 'active', 0, '2000-01-01', '2026-01-15 23:11:55', '2027-01-15 23:11:55');
INSERT INTO `users` VALUES (76, '83236155', '17812345678', '$2a$10$lDRBdihVghvSgkwoYuMbSOE6pzUem2lWSA5mp2SPn/Yey7DLYlvSu', 'COZY-83236155', '/images/default-avatar.png', 'WPNEFWUD', NULL, NULL, 0, '17812345678', NULL, '2026-01-15 23:21:02', '2026-01-15 23:21:02', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (77, '90339526', '17912345678', '$2a$10$rKQ4LPihEXjuOYdM2ey8y.PJKwHYQUoPfaY.Qnb5aGTSgs2EXhXG.', 'COZY-90339526', '/images/default-avatar.png', 'RUPSDHHN', NULL, NULL, 0, '17912345678', NULL, '2026-01-15 23:43:34', '2026-01-15 23:43:34', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (78, '69783911', '19012345678', '$2a$10$bjtZUF5NjvCb1N0md7JH7umQTX1L817IRneQX8r88jGjJTOy756fu', 'COZY-69783911', '/images/default-avatar.png', 'SHPY9NZD', 79, '2026-01-16 09:04:54', 1, '19012345678', NULL, '2026-01-16 08:24:53', '2026-01-16 08:24:53', 'user', 'active', 0, NULL, NULL, NULL);
INSERT INTO `users` VALUES (79, '49083236', '19112345678', '$2a$10$WC4Syf7yjM8cvkdftVMAcOUTYmdt8wty/MlyaJdiIUpalFcDVqO36', 'COZY-49083236', '/images/default-avatar.png', 'EHDY2VYC', NULL, NULL, 0, '19112345678', NULL, '2026-01-16 08:25:21', '2026-01-16 08:25:21', 'user', 'active', 0, NULL, NULL, NULL);

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
