/*
 Navicat Premium Data Transfer

 Source Server         : cozycoffee开发
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : cozy_order

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 10/07/2026 10:15:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for coffee_products
-- ----------------------------
DROP TABLE IF EXISTS `coffee_products`;
CREATE TABLE `coffee_products`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品描述',
  `price` decimal(10, 2) NOT NULL COMMENT '价格（元）',
  `price_medium` decimal(10, 2) NULL DEFAULT NULL COMMENT '中杯价格',
  `price_large` decimal(10, 2) NULL DEFAULT NULL COMMENT '大杯价格',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'coffee' COMMENT '分类：coffee/dessert/other',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'active' COMMENT '状态：active/inactive',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_new_product` tinyint(1) NULL DEFAULT 0 COMMENT '是否为新品: 0-否, 1-是',
  `size_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'MEDIUM_LARGE' COMMENT '杯型配置：DEFAULT/MEDIUM_LARGE/ALL_SIZES',
  `sugar_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'FREE_CHOICE' COMMENT '甜度配置：FREE_CHOICE/NO_SUGAR_ONLY/MIN_LESS_SWEET',
  `temp_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ALL_OK' COMMENT '温度配置：ALL_OK/COLD_ONLY/HOT_ONLY/NO_HOT',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_products_new`(`is_new_product` ASC) USING BTREE,
  INDEX `idx_sku_config`(`size_type` ASC, `sugar_type` ASC, `temp_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咖啡商品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of coffee_products
-- ----------------------------
INSERT INTO `coffee_products` VALUES (18, '额外浓缩', '增加一份浓缩咖啡液', 5.00, NULL, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E5%8A%A0%E6%96%99%E9%85%8D%E6%96%99/%E9%A2%9D%E5%A4%96%E6%B5%93%E7%BC%A9.png', 'addon', 'inactive', 0, '2026-01-04 14:29:35', '2026-07-08 17:48:26', 0, 'DEFAULT', 'NO_SUGAR_ONLY', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (19, 'Cozy 美式', '选用埃塞俄比亚拼配豆，中深烘焙，干净明亮。', 22.00, 22.00, 25.00, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/Cozy%E7%BE%8E%E5%BC%8F.png', 'espresso', 'active', 0, '2026-01-04 14:54:34', '2026-07-08 17:48:26', 0, 'MEDIUM_LARGE', 'FREE_CHOICE', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (20, '经典拿铁', '采用 4.0 蛋白质含量鲜牛乳，奶香与咖香完美融合。', 28.00, 28.00, 32.00, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/%E7%BB%8F%E5%85%B8%E6%8B%BF%E9%93%81.png', 'espresso', 'active', 0, '2026-01-04 15:44:45', '2026-07-08 17:48:26', 0, 'MEDIUM_LARGE', 'FREE_CHOICE', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (21, '澳白', '更加浓郁的咖啡体，极致丝滑。', 30.00, 30.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/%E6%BE%B3%E7%99%BD.png', 'espresso', 'active', 0, '2026-01-04 16:05:18', '2026-07-08 17:48:26', 0, 'DEFAULT', 'FREE_CHOICE', 'HOT_ONLY');
INSERT INTO `coffee_products` VALUES (22, '生椰拿铁', '店内 Top 1 爆款，天然植物乳。', 32.00, 32.00, 35.00, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/%E7%94%9F%E6%A4%B0%E6%8B%BF%E9%93%81.png', 'espresso', 'active', 0, '2026-01-04 16:05:48', '2026-07-08 17:48:26', 0, 'MEDIUM_LARGE', 'FREE_CHOICE', 'NO_HOT');
INSERT INTO `coffee_products` VALUES (23, '燕麦拿铁', 'OATLY 燕麦奶，素食及乳糖不耐受友好。', 32.00, 32.00, 35.00, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/%E7%87%95%E9%BA%A6%E6%8B%BF%E9%93%81.png', 'espresso', 'active', 0, '2026-01-04 16:06:33', '2026-07-08 17:48:26', 0, 'MEDIUM_LARGE', 'FREE_CHOICE', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (24, '拿铁金·桂花特调', '品牌色定制款，含干桂花与特制糖浆。', 38.00, 38.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E5%AD%A3%E8%8A%82%E9%99%90%E5%AE%9Aor%E7%89%B9%E8%B0%83/%E6%8B%BF%E9%93%81%E9%87%91%C2%B7%E6%A1%82%E8%8A%B1%E7%89%B9%E8%B0%83.png', 'signature', 'active', 0, '2026-01-04 16:12:11', '2026-07-08 17:48:26', 1, 'DEFAULT', 'MIN_LESS_SWEET', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (25, '冰摇荔枝咖啡', '清爽果香，夏日消暑首选。', 36.00, 36.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E5%AD%A3%E8%8A%82%E9%99%90%E5%AE%9Aor%E7%89%B9%E8%B0%83/%E5%86%B0%E6%91%87%E8%8D%94%E6%9E%9D%E5%92%96%E5%95%A1.png', 'signature', 'active', 0, '2026-01-04 16:12:41', '2026-07-08 17:48:26', 1, 'DEFAULT', 'MIN_LESS_SWEET', 'COLD_ONLY');
INSERT INTO `coffee_products` VALUES (26, '手冲精品咖啡 (SOE)', '瑰夏/花魁等名豆，限黑金会员 8.5 折。', 50.00, 50.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E5%AD%A3%E8%8A%82%E9%99%90%E5%AE%9Aor%E7%89%B9%E8%B0%83/%E6%89%8B%E5%86%B2%E7%B2%BE%E5%93%81%E5%92%96%E5%95%A1%20(SOE).png', 'soe', 'active', 0, '2026-01-04 16:13:17', '2026-07-08 17:48:26', 0, 'DEFAULT', 'NO_SUGAR_ONLY', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (27, '海盐焦糖牛角包', '适合搭配美式，中和甜度。', 18.00, 18.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E7%83%98%E5%9F%B9%E7%94%9C%E5%93%81/%E6%B5%B7%E7%9B%90%E7%84%A6%E7%B3%96%E7%89%9B%E8%A7%92%E5%8C%85.png', 'bakery', 'active', 0, '2026-01-04 16:14:16', '2026-07-08 17:48:26', 0, 'DEFAULT', 'NO_SUGAR_ONLY', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (28, '巴斯克芝士蛋糕', '浓郁奶香，下午茶点单王。', 35.00, 35.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E7%83%98%E5%9F%B9%E7%94%9C%E5%93%81/%E5%B7%B4%E6%96%AF%E5%85%8B%E8%8A%9D%E5%A3%AB%E8%9B%8B%E7%B3%95.png', 'bakery', 'active', 0, '2026-01-04 16:14:43', '2026-07-08 17:48:26', 0, 'DEFAULT', 'MIN_LESS_SWEET', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (29, '提拉米苏 (Cozy版)', '融入店内浓缩咖啡液，入口即化。', 38.00, 38.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E7%83%98%E5%9F%B9%E7%94%9C%E5%93%81/%E6%8F%90%E6%8B%89%E7%B1%B3%E8%8B%8F%20(Cozy%E7%89%88).png', 'bakery', 'active', 0, '2026-01-04 16:15:11', '2026-07-08 17:48:26', 0, 'DEFAULT', 'NO_SUGAR_ONLY', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (30, '手工燕麦曲奇', '低成本加购项，适合凑单。', 12.00, 12.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E7%83%98%E5%9F%B9%E7%94%9C%E5%93%81/%E6%89%8B%E5%B7%A5%E7%87%95%E9%BA%A6%E6%9B%B2%E5%A5%87.png', 'bakery', 'active', 0, '2026-01-04 16:17:53', '2026-07-08 17:48:26', 0, 'DEFAULT', 'NO_SUGAR_ONLY', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (31, '卡布奇诺', '绵密厚奶泡（Foam），口感蓬松，撒有少许可可粉。', 28.00, 28.00, 32.00, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/%E5%8D%A1%E5%B8%83%E5%A5%87%E8%AF%BA%20(Cappuccino).png', 'espresso', 'active', 0, '2026-01-05 00:15:14', '2026-07-08 17:48:26', 0, 'MEDIUM_LARGE', 'FREE_CHOICE', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (32, '焦糖玛奇朵', '香草糖浆打底，覆盖绵密奶泡，淋上如网状般的焦糖酱。', 33.00, 33.00, 37.00, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/%E7%84%A6%E7%B3%96%E7%8E%9B%E5%A5%87%E6%9C%B5%20(Caramel%20Macchiato).png', 'espresso', 'active', 0, '2026-01-05 00:15:40', '2026-07-08 17:48:26', 0, 'MEDIUM_LARGE', 'MIN_LESS_SWEET', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (33, '摩卡', '融合比利时黑巧克力酱与鲜奶，顶层覆盖鲜奶油。', 32.00, 32.00, 36.00, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/%E6%91%A9%E5%8D%A1%20(Caff%C3%A8%20Mocha).png', 'espresso', 'active', 0, '2026-01-05 00:16:05', '2026-07-08 17:48:26', 0, 'MEDIUM_LARGE', 'MIN_LESS_SWEET', 'ALL_OK');
INSERT INTO `coffee_products` VALUES (34, 'Dirty (脏咖)', '冰博克厚乳 x 热浓缩，大口饮用体验冷热交融。', 32.00, 32.00, NULL, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E7%82%B9%E5%8D%95%E5%95%86%E5%93%81/%E6%84%8F%E5%BC%8F%E6%B5%93%E7%BC%A9%E5%92%96%E5%95%A1/Dirty%20(%E8%84%8F%E5%92%96).png', 'espresso', 'active', 0, '2026-01-05 00:17:03', '2026-07-08 17:48:26', 0, 'DEFAULT', 'NO_SUGAR_ONLY', 'COLD_ONLY');

-- ----------------------------
-- Table structure for message_outbox
-- ----------------------------
DROP TABLE IF EXISTS `message_outbox`;
CREATE TABLE `message_outbox`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_id` bigint NOT NULL,
  `message_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` json NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_aggregate_type`(`aggregate_id` ASC, `message_type` ASC) USING BTREE,
  INDEX `idx_status_next_retry`(`status` ASC, `next_retry_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of message_outbox
-- ----------------------------
INSERT INTO `message_outbox` VALUES (1, 309, 'coupon_rollback', 'cozy-order-events', 'order_cancelled', '{\"userId\": 19, \"orderId\": 309, \"occurredAt\": \"2026-07-08T14:28:15.3493483\", \"addonCouponIds\": [], \"appliedCouponId\": 323}', 'SENT', 0, '2026-07-08 14:28:15', '2026-07-08 14:28:15', '2026-07-08 14:28:15');

-- ----------------------------
-- Table structure for order_addon_coupons
-- ----------------------------
DROP TABLE IF EXISTS `order_addon_coupons`;
CREATE TABLE `order_addon_coupons`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT 'è®¢å•ID',
  `coupon_id` bigint NOT NULL COMMENT 'é™„åŠ åˆ¸ID (cozy_mall.user_coupons.id)',
  `coupon_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'åˆ¸ç å†—ä½™ï¼Œæ–¹ä¾¿æŸ¥è¯¢é¿å…è·¨åº“JOIN',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_coupon`(`order_id` ASC, `coupon_id` ASC) USING BTREE COMMENT 'é˜²é‡å¤ç»‘å®š',
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'è®¢å•é™„åŠ åˆ¸å…³è”è¡¨' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_addon_coupons
-- ----------------------------

-- ----------------------------
-- Table structure for pickup_code_counter
-- ----------------------------
DROP TABLE IF EXISTS `pickup_code_counter`;
CREATE TABLE `pickup_code_counter`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT 1,
  `business_date` date NOT NULL,
  `last_seq` int NOT NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_store_business`(`store_id` ASC, `business_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pickup_code_counter
-- ----------------------------
INSERT INTO `pickup_code_counter` VALUES (1, 1, '2025-12-23', 14, NULL, '2025-12-23 17:19:23');
INSERT INTO `pickup_code_counter` VALUES (2, 1, '2025-12-24', 1, NULL, '2025-12-24 17:57:16');
INSERT INTO `pickup_code_counter` VALUES (3, 1, '2025-12-28', 32, NULL, '2025-12-28 18:32:51');
INSERT INTO `pickup_code_counter` VALUES (4, 1, '2025-12-31', 8, NULL, '2025-12-31 18:07:11');
INSERT INTO `pickup_code_counter` VALUES (5, 1, '2026-01-01', 4, NULL, '2026-01-01 18:00:55');
INSERT INTO `pickup_code_counter` VALUES (6, 1, '2026-01-02', 19, NULL, '2026-01-03 00:43:13');
INSERT INTO `pickup_code_counter` VALUES (7, 1, '2026-01-04', 8, '2026-01-04 23:11:27', '2026-01-05 00:36:49');
INSERT INTO `pickup_code_counter` VALUES (8, 1, '2026-01-05', 4, '2026-01-05 10:30:55', '2026-01-05 19:31:08');
INSERT INTO `pickup_code_counter` VALUES (9, 1, '2026-01-06', 2, '2026-01-06 12:26:02', '2026-01-06 12:26:51');
INSERT INTO `pickup_code_counter` VALUES (10, 1, '2026-01-10', 15, '2026-01-10 14:59:25', '2026-01-10 23:28:42');
INSERT INTO `pickup_code_counter` VALUES (11, 1, '2026-01-11', 23, '2026-01-11 12:21:38', '2026-01-11 18:15:48');
INSERT INTO `pickup_code_counter` VALUES (12, 1, '2026-01-12', 71, '2026-01-12 10:21:45', '2026-01-12 23:04:03');
INSERT INTO `pickup_code_counter` VALUES (13, 1, '2026-01-13', 21, '2026-01-13 13:53:34', '2026-01-13 21:39:56');
INSERT INTO `pickup_code_counter` VALUES (14, 1, '2026-01-14', 1, '2026-01-14 19:13:07', '2026-01-14 19:13:06');
INSERT INTO `pickup_code_counter` VALUES (15, 1, '2026-01-15', 61, '2026-01-15 17:37:47', '2026-01-15 23:42:55');
INSERT INTO `pickup_code_counter` VALUES (16, 1, '2026-01-16', 5, '2026-01-16 08:15:43', '2026-01-16 09:21:10');
INSERT INTO `pickup_code_counter` VALUES (17, 1, '2026-01-19', 5, '2026-01-19 22:45:12', '2026-01-19 23:38:10');
INSERT INTO `pickup_code_counter` VALUES (18, 1, '2026-07-07', 4, '2026-07-07 14:20:35', '2026-07-07 14:35:06');
INSERT INTO `pickup_code_counter` VALUES (19, 1, '2026-07-08', 5, '2026-07-08 13:28:02', '2026-07-08 14:28:04');

-- ----------------------------
-- Table structure for product_addons
-- ----------------------------
DROP TABLE IF EXISTS `product_addons`;
CREATE TABLE `product_addons`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加料名称',
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加料代码: EXTRA_SHOT, OAT_MILK',
  `price` decimal(10, 2) NOT NULL COMMENT '加料价格',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'OTHER' COMMENT '加料类型: SHOT, MILK, SYRUP, OTHER',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'active' COMMENT 'active/inactive',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '加料/配料表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of product_addons
-- ----------------------------
INSERT INTO `product_addons` VALUES (1, '额外浓缩', 'EXTRA_SHOT', 5.00, 'SHOT', '增加一份浓缩咖啡', 'active', 1, '2026-01-04 12:52:00', '2026-01-04 12:52:00');
INSERT INTO `product_addons` VALUES (2, '换燕麦奶', 'OAT_MILK', 6.00, 'MILK', 'OATLY燕麦奶替换', 'active', 2, '2026-01-04 12:52:00', '2026-01-04 12:52:00');
INSERT INTO `product_addons` VALUES (3, '换椰奶', 'COCONUT_MILK', 5.00, 'MILK', '生椰乳替换', 'active', 3, '2026-01-04 12:52:00', '2026-01-04 12:52:00');
INSERT INTO `product_addons` VALUES (4, '加奶泡', 'EXTRA_FOAM', 3.00, 'OTHER', '增加奶泡层', 'active', 4, '2026-01-04 12:52:00', '2026-01-04 12:52:00');
INSERT INTO `product_addons` VALUES (5, '香草糖浆', 'VANILLA_SYRUP', 4.00, 'SYRUP', '添加香草风味', 'active', 5, '2026-01-04 12:52:00', '2026-01-04 12:52:00');
INSERT INTO `product_addons` VALUES (6, '焦糖糖浆', 'CARAMEL_SYRUP', 4.00, 'SYRUP', '添加焦糖风味', 'active', 6, '2026-01-04 12:52:00', '2026-01-04 12:52:00');

-- ----------------------------
-- Table structure for product_sku
-- ----------------------------
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT 'coffee_products.id',
  `sku_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SKUç¼–ç ',
  `size_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'æ¯åž‹: SMALL/MEDIUM/LARGE',
  `price` decimal(10, 2) NOT NULL COMMENT 'è¯¥SKUä»·æ ¼',
  `stock` int NULL DEFAULT 0 COMMENT 'åº“å­˜ï¼ˆæœªæ¥å¯ç”¨ï¼‰',
  `enabled` tinyint NULL DEFAULT 1,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sku_code`(`sku_code` ASC) USING BTREE,
  INDEX `idx_product`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'å•†å“SKU' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_sku
-- ----------------------------
INSERT INTO `product_sku` VALUES (1, 18, 'SKU-18-SMALL', 'SMALL', 5.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (2, 19, 'SKU-19-SMALL', 'SMALL', 22.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (3, 20, 'SKU-20-SMALL', 'SMALL', 28.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (4, 21, 'SKU-21-SMALL', 'SMALL', 30.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (5, 22, 'SKU-22-SMALL', 'SMALL', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (6, 23, 'SKU-23-SMALL', 'SMALL', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (7, 24, 'SKU-24-SMALL', 'SMALL', 38.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (8, 25, 'SKU-25-SMALL', 'SMALL', 36.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (9, 26, 'SKU-26-SMALL', 'SMALL', 50.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (10, 27, 'SKU-27-SMALL', 'SMALL', 18.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (11, 28, 'SKU-28-SMALL', 'SMALL', 35.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (12, 29, 'SKU-29-SMALL', 'SMALL', 38.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (13, 30, 'SKU-30-SMALL', 'SMALL', 12.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (14, 31, 'SKU-31-SMALL', 'SMALL', 28.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (15, 32, 'SKU-32-SMALL', 'SMALL', 33.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (16, 33, 'SKU-33-SMALL', 'SMALL', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (17, 34, 'SKU-34-SMALL', 'SMALL', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (32, 19, 'SKU-19-MEDIUM', 'MEDIUM', 22.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (33, 20, 'SKU-20-MEDIUM', 'MEDIUM', 28.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (34, 21, 'SKU-21-MEDIUM', 'MEDIUM', 30.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (35, 22, 'SKU-22-MEDIUM', 'MEDIUM', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (36, 23, 'SKU-23-MEDIUM', 'MEDIUM', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (37, 24, 'SKU-24-MEDIUM', 'MEDIUM', 38.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (38, 25, 'SKU-25-MEDIUM', 'MEDIUM', 36.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (39, 26, 'SKU-26-MEDIUM', 'MEDIUM', 50.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (40, 27, 'SKU-27-MEDIUM', 'MEDIUM', 18.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (41, 28, 'SKU-28-MEDIUM', 'MEDIUM', 35.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (42, 29, 'SKU-29-MEDIUM', 'MEDIUM', 38.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (43, 30, 'SKU-30-MEDIUM', 'MEDIUM', 12.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (44, 31, 'SKU-31-MEDIUM', 'MEDIUM', 28.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (45, 32, 'SKU-32-MEDIUM', 'MEDIUM', 33.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (46, 33, 'SKU-33-MEDIUM', 'MEDIUM', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (47, 34, 'SKU-34-MEDIUM', 'MEDIUM', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (63, 19, 'SKU-19-LARGE', 'LARGE', 25.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (64, 20, 'SKU-20-LARGE', 'LARGE', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (65, 22, 'SKU-22-LARGE', 'LARGE', 35.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (66, 23, 'SKU-23-LARGE', 'LARGE', 35.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (67, 31, 'SKU-31-LARGE', 'LARGE', 32.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (68, 32, 'SKU-32-LARGE', 'LARGE', 37.00, 0, 1, '2026-07-09 16:16:34');
INSERT INTO `product_sku` VALUES (69, 33, 'SKU-33-LARGE', 'LARGE', 36.00, 0, 1, '2026-07-09 16:16:34');

-- ----------------------------
-- Table structure for shop_order_items
-- ----------------------------
DROP TABLE IF EXISTS `shop_order_items`;
CREATE TABLE `shop_order_items`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称（快照）',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价（快照）',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `item_amount` decimal(10, 2) NOT NULL COMMENT '小计（=unit_price*quantity）',
  `cup_size` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '杯型',
  `sugar_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '糖度',
  `temperature` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '温度',
  `coffee_strength` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '浓度',
  `options_json` json NULL COMMENT '扩展选项',
  `addons_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '加料JSON: [{\"id\":1,\"name\":\"额外浓缩\",\"price\":5}]',
  `addons_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '加料总金额',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 414 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单项表（一单多商品）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of shop_order_items
-- ----------------------------
INSERT INTO `shop_order_items` VALUES (1, 1, 1, '美式咖啡', 18.00, 1, 18.00, 'medium', 'LESS', 'iced', 'normal', NULL, NULL, 0.00, '2025-12-22 16:22:52');
INSERT INTO `shop_order_items` VALUES (2, 2, 1, '美式咖啡', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (3, 3, 1, '美式咖啡', 18.00, 2, 36.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (4, 4, 1, '美式咖啡', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (5, 5, 2, '原味拿铁', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (6, 6, 1, '美式咖啡', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (7, 7, 1, '美式咖啡', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (8, 8, 2, '原味拿铁', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (9, 9, 3, '卡布奇诺', 30.00, 2, 60.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (10, 10, 3, '卡布奇诺', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (11, 11, 3, '卡布奇诺', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (12, 12, 3, '卡布奇诺', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (13, 13, 3, '卡布奇诺', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (14, 14, 3, '卡布奇诺', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (15, 15, 3, '卡布奇诺', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (16, 16, 3, '卡布奇诺', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, NULL);
INSERT INTO `shop_order_items` VALUES (17, 17, 1, '美式咖啡', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-23 17:18:52');
INSERT INTO `shop_order_items` VALUES (18, 18, 1, '美式咖啡', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-23 17:19:15');
INSERT INTO `shop_order_items` VALUES (19, 19, 2, '原味拿铁', 25.00, 2, 50.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-23 17:19:24');
INSERT INTO `shop_order_items` VALUES (20, 20, 6, '生椰拿铁', 28.00, 2, 56.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-24 17:57:17');
INSERT INTO `shop_order_items` VALUES (32, 21, 1, '摩卡', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 08:56:04');
INSERT INTO `shop_order_items` VALUES (33, 22, 1, '摩卡', 18.00, 2, 36.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-28 09:06:44');
INSERT INTO `shop_order_items` VALUES (34, 22, 5, '抹茶拿铁', 30.00, 1, 30.00, 'large', NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 09:06:44');
INSERT INTO `shop_order_items` VALUES (35, 23, 1, '摩卡', 18.00, 2, 36.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-28 09:24:36');
INSERT INTO `shop_order_items` VALUES (36, 23, 5, '抹茶拿铁', 30.00, 1, 30.00, 'large', NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 09:24:36');
INSERT INTO `shop_order_items` VALUES (37, 24, 1, '摩卡', 18.00, 1, 18.00, 'medium', NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 11:23:47');
INSERT INTO `shop_order_items` VALUES (38, 25, 8, '原味咖啡', 20.00, 1, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 12:46:11');
INSERT INTO `shop_order_items` VALUES (39, 26, 8, '原味咖啡', 20.00, 1, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 12:51:25');
INSERT INTO `shop_order_items` VALUES (40, 27, 8, '原味咖啡', 20.00, 1, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 13:04:22');
INSERT INTO `shop_order_items` VALUES (41, 28, 8, '原味咖啡', 20.00, 1, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 13:13:31');
INSERT INTO `shop_order_items` VALUES (42, 29, 8, '原味咖啡', 20.00, 1, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 13:37:29');
INSERT INTO `shop_order_items` VALUES (43, 30, 8, '原味咖啡', 20.00, 1, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 13:53:14');
INSERT INTO `shop_order_items` VALUES (44, 31, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 13:57:51');
INSERT INTO `shop_order_items` VALUES (45, 32, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 13:59:20');
INSERT INTO `shop_order_items` VALUES (46, 33, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 14:04:45');
INSERT INTO `shop_order_items` VALUES (47, 33, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 14:04:45');
INSERT INTO `shop_order_items` VALUES (48, 34, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 14:42:28');
INSERT INTO `shop_order_items` VALUES (49, 35, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 15:25:18');
INSERT INTO `shop_order_items` VALUES (50, 36, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 15:40:44');
INSERT INTO `shop_order_items` VALUES (51, 37, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 15:48:34');
INSERT INTO `shop_order_items` VALUES (52, 38, 16, '测试黑金加速卡', 228.00, 1, 228.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 15:51:23');
INSERT INTO `shop_order_items` VALUES (53, 39, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 15:52:24');
INSERT INTO `shop_order_items` VALUES (54, 40, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:20:28');
INSERT INTO `shop_order_items` VALUES (55, 41, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:20:50');
INSERT INTO `shop_order_items` VALUES (56, 42, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:21:40');
INSERT INTO `shop_order_items` VALUES (57, 43, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:25:48');
INSERT INTO `shop_order_items` VALUES (58, 44, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:41:21');
INSERT INTO `shop_order_items` VALUES (59, 45, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:41:52');
INSERT INTO `shop_order_items` VALUES (60, 46, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:51:04');
INSERT INTO `shop_order_items` VALUES (61, 47, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:51:25');
INSERT INTO `shop_order_items` VALUES (62, 48, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 17:52:10');
INSERT INTO `shop_order_items` VALUES (63, 49, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 18:27:54');
INSERT INTO `shop_order_items` VALUES (64, 50, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 18:29:52');
INSERT INTO `shop_order_items` VALUES (65, 51, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 18:30:02');
INSERT INTO `shop_order_items` VALUES (66, 52, 5, '抹茶拿铁', 30.00, 1, 30.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-28 18:32:52');
INSERT INTO `shop_order_items` VALUES (68, 54, 1, '摩卡', 25.00, 2, 50.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 16:38:16');
INSERT INTO `shop_order_items` VALUES (69, 55, 1, '摩卡', 25.00, 2, 50.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 16:47:07');
INSERT INTO `shop_order_items` VALUES (70, 56, 1, '摩卡', 25.00, 10, 250.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 17:05:57');
INSERT INTO `shop_order_items` VALUES (71, 57, 1, '摩卡', 25.00, 10, 250.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 17:08:37');
INSERT INTO `shop_order_items` VALUES (72, 57, 1, '摩卡', 25.00, 2, 50.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 17:08:37');
INSERT INTO `shop_order_items` VALUES (73, 58, 1, '摩卡', 25.00, 10, 250.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 17:09:11');
INSERT INTO `shop_order_items` VALUES (74, 58, 1, '摩卡', 25.00, 5, 125.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 17:09:11');
INSERT INTO `shop_order_items` VALUES (75, 59, 1, '摩卡', 25.00, 10, 250.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 17:09:17');
INSERT INTO `shop_order_items` VALUES (76, 59, 1, '摩卡', 25.00, 5, 125.00, 'medium', NULL, 'iced', NULL, NULL, NULL, 0.00, '2025-12-31 17:09:17');
INSERT INTO `shop_order_items` VALUES (77, 60, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-31 18:07:11');
INSERT INTO `shop_order_items` VALUES (78, 61, 1, '摩卡', 25.00, 10, 250.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-01 15:08:30');
INSERT INTO `shop_order_items` VALUES (79, 62, 1, '摩卡', 25.00, 10, 250.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-01 15:08:43');
INSERT INTO `shop_order_items` VALUES (80, 63, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-01 17:05:54');
INSERT INTO `shop_order_items` VALUES (81, 64, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-01 18:00:56');
INSERT INTO `shop_order_items` VALUES (82, 65, 1, '摩卡', 25.00, 1, 25.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-02 10:50:43');
INSERT INTO `shop_order_items` VALUES (83, 66, 17, '模拟300临界消费 预计获取430积分', 300.00, 1, 300.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-02 11:02:01');
INSERT INTO `shop_order_items` VALUES (84, 67, 8, '原味咖啡', 20.00, 1, 20.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-02 11:02:42');
INSERT INTO `shop_order_items` VALUES (85, 68, 17, '模拟300临界消费 预计获取430积分', 300.00, 1, 300.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-02 11:25:22');
INSERT INTO `shop_order_items` VALUES (86, 69, 17, '模拟300临界消费 预计获取430积分', 300.00, 1, 300.00, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-02 11:30:48');
INSERT INTO `shop_order_items` VALUES (87, 70, 8, '原味咖啡', 20.00, 2, 40.00, 'MEDIUM', 'STANDARD', 'HOT', NULL, NULL, NULL, 0.00, '2026-01-02 18:10:28');
INSERT INTO `shop_order_items` VALUES (88, 71, 15, 'test', 1.00, 1, 1.00, 'MEDIUM', 'STANDARD', 'HOT', NULL, NULL, NULL, 0.00, '2026-01-02 18:36:52');
INSERT INTO `shop_order_items` VALUES (89, 71, 5, '抹茶拿铁', 30.00, 2, 60.00, 'MEDIUM', 'STANDARD', 'HOT', NULL, NULL, NULL, 0.00, '2026-01-02 18:36:52');
INSERT INTO `shop_order_items` VALUES (90, 72, 8, '原味咖啡', 20.00, 1, 20.00, 'MEDIUM', 'STANDARD', 'HOT', NULL, NULL, NULL, 0.00, '2026-01-02 18:41:43');
INSERT INTO `shop_order_items` VALUES (91, 73, 8, '原味咖啡', 20.00, 1, 20.00, 'MEDIUM', 'STANDARD', 'HOT', NULL, NULL, NULL, 0.00, '2026-01-02 18:46:47');
INSERT INTO `shop_order_items` VALUES (92, 74, 15, 'test', 1.00, 2, 2.00, 'EXTRA_LARGE', 'HALF', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-02 22:11:02');
INSERT INTO `shop_order_items` VALUES (93, 74, 15, 'test', 1.00, 1, 1.00, 'MEDIUM', 'HALF', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-02 22:11:02');
INSERT INTO `shop_order_items` VALUES (94, 75, 8, '原味咖啡', 20.00, 1, 20.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-02 22:32:55');
INSERT INTO `shop_order_items` VALUES (95, 76, 8, '原味咖啡', 20.00, 1, 20.00, 'STANDARD', 'NONE', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-02 22:38:54');
INSERT INTO `shop_order_items` VALUES (96, 77, 8, '原味咖啡', 20.00, 1, 20.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-02 22:48:53');
INSERT INTO `shop_order_items` VALUES (97, 78, 5, '抹茶拿铁', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:08:57');
INSERT INTO `shop_order_items` VALUES (98, 78, 8, '原味咖啡', 20.00, 1, 20.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:08:57');
INSERT INTO `shop_order_items` VALUES (99, 79, 1, '摩卡', 25.00, 1, 25.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:24:09');
INSERT INTO `shop_order_items` VALUES (100, 79, 5, '抹茶拿铁', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:24:09');
INSERT INTO `shop_order_items` VALUES (101, 80, 1, '摩卡', 25.00, 1, 25.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:32:55');
INSERT INTO `shop_order_items` VALUES (102, 80, 5, '抹茶拿铁', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:32:55');
INSERT INTO `shop_order_items` VALUES (103, 81, 8, '原味咖啡', 20.00, 1, 20.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:40:58');
INSERT INTO `shop_order_items` VALUES (104, 82, 8, '原味咖啡', 20.00, 1, 20.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:41:20');
INSERT INTO `shop_order_items` VALUES (105, 83, 8, '原味咖啡', 20.00, 1, 20.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-03 00:43:13');
INSERT INTO `shop_order_items` VALUES (106, 84, 19, 'Cozy 美式 (Americano)', 22.00, 1, 22.00, 'LARGE', 'LESS', 'COLD', 'STRONG', NULL, NULL, 0.00, '2026-01-04 23:11:27');
INSERT INTO `shop_order_items` VALUES (107, 84, 30, '手工燕麦曲奇', 12.00, 1, 12.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', NULL, NULL, 0.00, '2026-01-04 23:11:27');
INSERT INTO `shop_order_items` VALUES (108, 85, 20, '经典拿铁 (Latte)', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', NULL, NULL, 0.00, '2026-01-04 23:37:59');
INSERT INTO `shop_order_items` VALUES (109, 86, 20, '经典拿铁 (Latte)', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'COLD', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', NULL, 0.00, '2026-01-04 23:56:54');
INSERT INTO `shop_order_items` VALUES (110, 87, 20, '经典拿铁 (Latte)', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'COLD', 'NORMAL', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 0}}', NULL, 0.00, '2026-01-04 23:57:15');
INSERT INTO `shop_order_items` VALUES (111, 88, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 00:19:54');
INSERT INTO `shop_order_items` VALUES (112, 89, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', NULL, 0.00, '2026-01-05 00:29:33');
INSERT INTO `shop_order_items` VALUES (113, 90, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 00:35:54');
INSERT INTO `shop_order_items` VALUES (114, 90, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 00:35:54');
INSERT INTO `shop_order_items` VALUES (115, 91, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 00:36:50');
INSERT INTO `shop_order_items` VALUES (116, 91, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 00:36:50');
INSERT INTO `shop_order_items` VALUES (117, 92, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 0}}', NULL, 0.00, '2026-01-05 10:29:06');
INSERT INTO `shop_order_items` VALUES (118, 93, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', NULL, 0.00, '2026-01-05 10:30:55');
INSERT INTO `shop_order_items` VALUES (119, 93, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, 'LARGE', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 10:30:55');
INSERT INTO `shop_order_items` VALUES (120, 93, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 10:30:55');
INSERT INTO `shop_order_items` VALUES (121, 93, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-05 10:30:55');
INSERT INTO `shop_order_items` VALUES (122, 94, 23, '燕麦拿铁', 32.00, 1, 32.00, 'LARGE', 'NONE', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', NULL, 0.00, '2026-01-05 10:42:23');
INSERT INTO `shop_order_items` VALUES (123, 95, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'LESS', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', NULL, 0.00, '2026-01-05 10:56:20');
INSERT INTO `shop_order_items` VALUES (124, 96, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', NULL, 0.00, '2026-01-05 19:31:08');
INSERT INTO `shop_order_items` VALUES (125, 97, 22, '生椰拿铁', 32.00, 2, 64.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-06 12:26:01');
INSERT INTO `shop_order_items` VALUES (126, 98, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-06 12:26:52');
INSERT INTO `shop_order_items` VALUES (127, 99, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 14:59:25');
INSERT INTO `shop_order_items` VALUES (128, 99, 30, '手工燕麦曲奇', 12.00, 1, 12.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 14:59:25');
INSERT INTO `shop_order_items` VALUES (129, 100, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 15:02:28');
INSERT INTO `shop_order_items` VALUES (130, 101, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 15:45:50');
INSERT INTO `shop_order_items` VALUES (131, 102, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 15:48:07');
INSERT INTO `shop_order_items` VALUES (132, 103, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 15:52:12');
INSERT INTO `shop_order_items` VALUES (133, 104, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 15:56:35');
INSERT INTO `shop_order_items` VALUES (134, 105, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 16:00:25');
INSERT INTO `shop_order_items` VALUES (135, 106, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 21:24:30');
INSERT INTO `shop_order_items` VALUES (136, 107, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 22:31:02');
INSERT INTO `shop_order_items` VALUES (137, 108, 19, 'Cozy 美式', 22.00, 2, 44.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 22:44:41');
INSERT INTO `shop_order_items` VALUES (138, 109, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 23:02:59');
INSERT INTO `shop_order_items` VALUES (139, 110, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 23:03:40');
INSERT INTO `shop_order_items` VALUES (140, 111, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 23:09:59');
INSERT INTO `shop_order_items` VALUES (141, 112, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 23:28:30');
INSERT INTO `shop_order_items` VALUES (142, 113, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-10 23:28:43');
INSERT INTO `shop_order_items` VALUES (143, 114, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 12:21:38');
INSERT INTO `shop_order_items` VALUES (144, 115, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 12:21:57');
INSERT INTO `shop_order_items` VALUES (145, 116, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 12:23:38');
INSERT INTO `shop_order_items` VALUES (146, 117, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 12:24:46');
INSERT INTO `shop_order_items` VALUES (147, 118, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 12:25:05');
INSERT INTO `shop_order_items` VALUES (148, 119, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 12:29:16');
INSERT INTO `shop_order_items` VALUES (149, 120, 20, '经典拿铁', 28.00, 1, 28.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 15:51:51');
INSERT INTO `shop_order_items` VALUES (150, 121, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 15:52:59');
INSERT INTO `shop_order_items` VALUES (151, 122, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:07:43');
INSERT INTO `shop_order_items` VALUES (152, 123, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:13:35');
INSERT INTO `shop_order_items` VALUES (153, 124, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:20:06');
INSERT INTO `shop_order_items` VALUES (154, 125, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 16:21:41');
INSERT INTO `shop_order_items` VALUES (155, 126, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:27:43');
INSERT INTO `shop_order_items` VALUES (156, 127, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:28:29');
INSERT INTO `shop_order_items` VALUES (157, 128, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:34:08');
INSERT INTO `shop_order_items` VALUES (158, 129, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:43:09');
INSERT INTO `shop_order_items` VALUES (159, 130, 19, 'Cozy 美式', 22.00, 2, 44.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 16:57:11');
INSERT INTO `shop_order_items` VALUES (160, 131, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 17:16:53');
INSERT INTO `shop_order_items` VALUES (161, 132, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 17:18:09');
INSERT INTO `shop_order_items` VALUES (162, 133, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 18:08:43');
INSERT INTO `shop_order_items` VALUES (163, 134, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 18:09:35');
INSERT INTO `shop_order_items` VALUES (164, 135, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-11 18:10:44');
INSERT INTO `shop_order_items` VALUES (165, 136, 29, '提拉米苏 (Cozy版)', 38.00, 1, 38.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 18:15:49');
INSERT INTO `shop_order_items` VALUES (166, 136, 30, '手工燕麦曲奇', 12.00, 1, 12.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-11 18:15:49');
INSERT INTO `shop_order_items` VALUES (167, 137, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 10:21:45');
INSERT INTO `shop_order_items` VALUES (168, 138, 19, 'Cozy 美式', 22.00, 1, 22.00, 'LARGE', 'STANDARD', 'COLD', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 10:28:59');
INSERT INTO `shop_order_items` VALUES (169, 139, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 10:32:30');
INSERT INTO `shop_order_items` VALUES (170, 140, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 10:33:27');
INSERT INTO `shop_order_items` VALUES (171, 141, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:03:29');
INSERT INTO `shop_order_items` VALUES (172, 142, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'HALF', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:29:06');
INSERT INTO `shop_order_items` VALUES (173, 142, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'NONE', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:29:06');
INSERT INTO `shop_order_items` VALUES (174, 142, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'LESS', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:29:06');
INSERT INTO `shop_order_items` VALUES (175, 142, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:29:06');
INSERT INTO `shop_order_items` VALUES (176, 142, 19, 'Cozy 美式', 22.00, 1, 22.00, 'LARGE', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:29:06');
INSERT INTO `shop_order_items` VALUES (177, 142, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'COLD', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:29:06');
INSERT INTO `shop_order_items` VALUES (178, 142, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 11:29:06');
INSERT INTO `shop_order_items` VALUES (179, 143, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:31:16');
INSERT INTO `shop_order_items` VALUES (180, 144, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:31:22');
INSERT INTO `shop_order_items` VALUES (181, 145, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:31:26');
INSERT INTO `shop_order_items` VALUES (182, 146, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'HALF', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:34:09');
INSERT INTO `shop_order_items` VALUES (183, 147, 23, '燕麦拿铁', 32.00, 1, 32.00, 'STANDARD', 'LESS', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:47:20');
INSERT INTO `shop_order_items` VALUES (184, 147, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'HALF', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:47:20');
INSERT INTO `shop_order_items` VALUES (185, 147, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'LESS', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:47:20');
INSERT INTO `shop_order_items` VALUES (186, 148, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 11:53:03');
INSERT INTO `shop_order_items` VALUES (187, 148, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 11:53:03');
INSERT INTO `shop_order_items` VALUES (188, 149, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 12:02:19');
INSERT INTO `shop_order_items` VALUES (189, 150, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 12:02:44');
INSERT INTO `shop_order_items` VALUES (190, 151, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 15:17:13');
INSERT INTO `shop_order_items` VALUES (191, 152, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 15:20:58');
INSERT INTO `shop_order_items` VALUES (192, 153, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 15:32:35');
INSERT INTO `shop_order_items` VALUES (193, 154, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 15:36:32');
INSERT INTO `shop_order_items` VALUES (194, 155, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 15:45:02');
INSERT INTO `shop_order_items` VALUES (195, 156, 19, 'Cozy 美式', 22.00, 1, 22.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 15:49:04');
INSERT INTO `shop_order_items` VALUES (196, 157, 19, 'Cozy 美式', 22.00, 1, 22.00, 'LARGE', 'NONE', 'COLD', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 16:02:12');
INSERT INTO `shop_order_items` VALUES (197, 158, 19, 'Cozy 美式', 22.00, 1, 25.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 16:27:49');
INSERT INTO `shop_order_items` VALUES (198, 159, 19, 'Cozy 美式', 22.00, 1, 25.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', NULL, 0.00, '2026-01-12 16:43:00');
INSERT INTO `shop_order_items` VALUES (199, 160, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 16:50:16');
INSERT INTO `shop_order_items` VALUES (200, 161, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'NONE', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 16:52:50');
INSERT INTO `shop_order_items` VALUES (201, 162, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 16:58:26');
INSERT INTO `shop_order_items` VALUES (202, 162, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 16:58:26');
INSERT INTO `shop_order_items` VALUES (203, 163, 26, '手冲精品咖啡 (SOE)', 50.00, 2, 100.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:00:08');
INSERT INTO `shop_order_items` VALUES (204, 164, 26, '手冲精品咖啡 (SOE)', 50.00, 2, 100.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:07:27');
INSERT INTO `shop_order_items` VALUES (205, 165, 20, '经典拿铁', 28.00, 1, 31.00, 'LARGE', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:10:08');
INSERT INTO `shop_order_items` VALUES (206, 165, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:10:08');
INSERT INTO `shop_order_items` VALUES (207, 166, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:13:32');
INSERT INTO `shop_order_items` VALUES (208, 167, 21, '澳白', 30.00, 1, 30.00, NULL, 'STANDARD', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:22:04');
INSERT INTO `shop_order_items` VALUES (209, 168, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:22:48');
INSERT INTO `shop_order_items` VALUES (210, 168, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:22:48');
INSERT INTO `shop_order_items` VALUES (211, 169, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:37:08');
INSERT INTO `shop_order_items` VALUES (212, 170, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:37:32');
INSERT INTO `shop_order_items` VALUES (213, 171, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:39:53');
INSERT INTO `shop_order_items` VALUES (214, 172, 29, '提拉米苏 (Cozy版)', 38.00, 1, 38.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:40:12');
INSERT INTO `shop_order_items` VALUES (215, 173, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:40:27');
INSERT INTO `shop_order_items` VALUES (216, 173, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:40:27');
INSERT INTO `shop_order_items` VALUES (217, 174, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:42:14');
INSERT INTO `shop_order_items` VALUES (218, 174, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:42:14');
INSERT INTO `shop_order_items` VALUES (219, 175, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:42:26');
INSERT INTO `shop_order_items` VALUES (220, 175, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:42:26');
INSERT INTO `shop_order_items` VALUES (221, 176, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:42:49');
INSERT INTO `shop_order_items` VALUES (222, 176, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:42:49');
INSERT INTO `shop_order_items` VALUES (223, 177, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'LESS', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:43:18');
INSERT INTO `shop_order_items` VALUES (224, 177, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:43:18');
INSERT INTO `shop_order_items` VALUES (225, 178, 29, '提拉米苏 (Cozy版)', 38.00, 1, 38.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:43:42');
INSERT INTO `shop_order_items` VALUES (226, 178, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'HALF', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:43:42');
INSERT INTO `shop_order_items` VALUES (227, 179, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:44:08');
INSERT INTO `shop_order_items` VALUES (228, 179, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:44:08');
INSERT INTO `shop_order_items` VALUES (229, 180, 23, '燕麦拿铁', 32.00, 1, 32.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:49:22');
INSERT INTO `shop_order_items` VALUES (230, 180, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:49:22');
INSERT INTO `shop_order_items` VALUES (231, 181, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:49:32');
INSERT INTO `shop_order_items` VALUES (232, 182, 21, '澳白', 30.00, 1, 30.00, NULL, 'HALF', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:49:53');
INSERT INTO `shop_order_items` VALUES (233, 182, 30, '手工燕麦曲奇', 12.00, 1, 12.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:49:53');
INSERT INTO `shop_order_items` VALUES (234, 183, 21, '澳白', 30.00, 1, 30.00, NULL, 'STANDARD', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:50:09');
INSERT INTO `shop_order_items` VALUES (235, 183, 30, '手工燕麦曲奇', 12.00, 1, 12.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:50:09');
INSERT INTO `shop_order_items` VALUES (236, 184, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'HALF', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:50:50');
INSERT INTO `shop_order_items` VALUES (237, 184, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:50:50');
INSERT INTO `shop_order_items` VALUES (238, 185, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'HALF', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:51:11');
INSERT INTO `shop_order_items` VALUES (239, 185, 29, '提拉米苏 (Cozy版)', 38.00, 1, 38.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:51:11');
INSERT INTO `shop_order_items` VALUES (240, 186, 19, 'Cozy 美式', 22.00, 10, 220.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:53:09');
INSERT INTO `shop_order_items` VALUES (241, 187, 19, 'Cozy 美式', 22.00, 4, 88.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 17:55:33');
INSERT INTO `shop_order_items` VALUES (242, 188, 19, 'Cozy 美式', 22.00, 1, 27.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 18:22:32');
INSERT INTO `shop_order_items` VALUES (243, 189, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 18:23:56');
INSERT INTO `shop_order_items` VALUES (244, 190, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 18:24:49');
INSERT INTO `shop_order_items` VALUES (245, 190, 30, '手工燕麦曲奇', 12.00, 1, 12.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 18:24:49');
INSERT INTO `shop_order_items` VALUES (246, 191, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 18:32:39');
INSERT INTO `shop_order_items` VALUES (247, 191, 30, '手工燕麦曲奇', 12.00, 1, 12.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 18:32:39');
INSERT INTO `shop_order_items` VALUES (248, 192, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:15:53');
INSERT INTO `shop_order_items` VALUES (249, 193, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:36:45');
INSERT INTO `shop_order_items` VALUES (250, 193, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:36:45');
INSERT INTO `shop_order_items` VALUES (251, 194, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:44:18');
INSERT INTO `shop_order_items` VALUES (252, 194, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:44:18');
INSERT INTO `shop_order_items` VALUES (253, 195, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:51:12');
INSERT INTO `shop_order_items` VALUES (254, 196, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:55:44');
INSERT INTO `shop_order_items` VALUES (255, 196, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 20:55:44');
INSERT INTO `shop_order_items` VALUES (256, 197, 28, '巴斯克芝士蛋糕', 35.00, 2, 70.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:02:14');
INSERT INTO `shop_order_items` VALUES (257, 197, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:02:14');
INSERT INTO `shop_order_items` VALUES (258, 197, 29, '提拉米苏 (Cozy版)', 38.00, 1, 38.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:02:14');
INSERT INTO `shop_order_items` VALUES (259, 198, 27, '海盐焦糖牛角包', 18.00, 1, 18.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:03:11');
INSERT INTO `shop_order_items` VALUES (260, 198, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:03:11');
INSERT INTO `shop_order_items` VALUES (261, 199, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:03:37');
INSERT INTO `shop_order_items` VALUES (262, 200, 32, '焦糖玛奇朵', 33.00, 1, 33.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:10:18');
INSERT INTO `shop_order_items` VALUES (263, 200, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:10:18');
INSERT INTO `shop_order_items` VALUES (264, 201, 19, 'Cozy 美式', 22.00, 2, 54.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 21:40:58');
INSERT INTO `shop_order_items` VALUES (265, 202, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:47:26');
INSERT INTO `shop_order_items` VALUES (266, 203, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 21:51:57');
INSERT INTO `shop_order_items` VALUES (267, 204, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 22:05:56');
INSERT INTO `shop_order_items` VALUES (268, 205, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 22:07:00');
INSERT INTO `shop_order_items` VALUES (269, 205, 19, 'Cozy 美式', 22.00, 1, 27.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 22:07:00');
INSERT INTO `shop_order_items` VALUES (270, 206, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-12 22:31:58');
INSERT INTO `shop_order_items` VALUES (271, 207, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-12 23:04:04');
INSERT INTO `shop_order_items` VALUES (272, 208, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 13:53:33');
INSERT INTO `shop_order_items` VALUES (273, 209, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 14:10:51');
INSERT INTO `shop_order_items` VALUES (274, 210, 20, '经典拿铁', 28.00, 1, 31.00, 'LARGE', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 15:51:22');
INSERT INTO `shop_order_items` VALUES (275, 210, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 15:51:22');
INSERT INTO `shop_order_items` VALUES (276, 211, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 16:43:16');
INSERT INTO `shop_order_items` VALUES (277, 212, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 17:30:51');
INSERT INTO `shop_order_items` VALUES (278, 213, 19, 'Cozy 美式', 22.00, 1, 27.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-13 18:18:05');
INSERT INTO `shop_order_items` VALUES (279, 214, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 18:19:39');
INSERT INTO `shop_order_items` VALUES (280, 215, 21, '澳白', 30.00, 1, 30.00, NULL, 'STANDARD', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 18:33:44');
INSERT INTO `shop_order_items` VALUES (281, 216, 28, '巴斯克芝士蛋糕', 35.00, 1, 35.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 18:40:14');
INSERT INTO `shop_order_items` VALUES (282, 217, 29, '提拉米苏 (Cozy版)', 38.00, 1, 38.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 19:57:31');
INSERT INTO `shop_order_items` VALUES (283, 218, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 20:21:39');
INSERT INTO `shop_order_items` VALUES (284, 219, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 20:29:10');
INSERT INTO `shop_order_items` VALUES (285, 220, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-13 20:29:28');
INSERT INTO `shop_order_items` VALUES (286, 221, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 20:29:44');
INSERT INTO `shop_order_items` VALUES (287, 222, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-13 20:31:08');
INSERT INTO `shop_order_items` VALUES (288, 223, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-13 20:52:05');
INSERT INTO `shop_order_items` VALUES (289, 224, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"name\":\"加浓缩\",\"price\":5}]', 0.00, '2026-01-13 20:52:17');
INSERT INTO `shop_order_items` VALUES (290, 225, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5}]', 0.00, '2026-01-13 21:13:02');
INSERT INTO `shop_order_items` VALUES (291, 226, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5}]', 0.00, '2026-01-13 21:13:41');
INSERT INTO `shop_order_items` VALUES (292, 227, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5}]', 0.00, '2026-01-13 21:23:22');
INSERT INTO `shop_order_items` VALUES (293, 228, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-13 21:39:57');
INSERT INTO `shop_order_items` VALUES (294, 229, 21, '澳白', 30.00, 1, 30.00, NULL, 'STANDARD', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-14 19:13:07');
INSERT INTO `shop_order_items` VALUES (295, 230, 19, 'Cozy 美式', 22.00, 1, 27.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5}]', 0.00, '2026-01-15 17:37:47');
INSERT INTO `shop_order_items` VALUES (296, 231, 19, 'Cozy 美式', 22.00, 1, 30.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5}]', 0.00, '2026-01-15 18:28:56');
INSERT INTO `shop_order_items` VALUES (297, 231, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 18:28:56');
INSERT INTO `shop_order_items` VALUES (298, 232, 21, '澳白', 30.00, 1, 35.00, NULL, 'STANDARD', NULL, 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5}]', 0.00, '2026-01-15 18:57:14');
INSERT INTO `shop_order_items` VALUES (299, 233, 20, '经典拿铁', 28.00, 1, 37.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 19:29:44');
INSERT INTO `shop_order_items` VALUES (300, 234, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 19:38:06');
INSERT INTO `shop_order_items` VALUES (301, 234, 30, '手工燕麦曲奇', 12.00, 1, 12.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 19:38:06');
INSERT INTO `shop_order_items` VALUES (302, 235, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 19:40:39');
INSERT INTO `shop_order_items` VALUES (303, 236, 19, 'Cozy 美式', 22.00, 1, 27.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5}]', 0.00, '2026-01-15 19:59:35');
INSERT INTO `shop_order_items` VALUES (304, 237, 26, '手冲精品咖啡 (SOE)', 50.00, 2, 100.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 20:07:56');
INSERT INTO `shop_order_items` VALUES (305, 238, 20, '经典拿铁', 28.00, 1, 31.00, 'LARGE', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 20:13:11');
INSERT INTO `shop_order_items` VALUES (306, 238, 20, '经典拿铁', 28.00, 1, 32.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 0}}', '[{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 20:13:11');
INSERT INTO `shop_order_items` VALUES (307, 239, 20, '经典拿铁', 28.00, 1, 40.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 20:13:48');
INSERT INTO `shop_order_items` VALUES (308, 239, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 20:13:48');
INSERT INTO `shop_order_items` VALUES (309, 240, 20, '经典拿铁', 28.00, 1, 31.00, 'LARGE', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 3, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 20:14:42');
INSERT INTO `shop_order_items` VALUES (310, 240, 20, '经典拿铁', 28.00, 1, 32.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 0}}', '[{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 20:14:42');
INSERT INTO `shop_order_items` VALUES (311, 241, 20, '经典拿铁', 28.00, 1, 40.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 20:37:03');
INSERT INTO `shop_order_items` VALUES (312, 241, 20, '经典拿铁', 28.00, 1, 35.00, 'LARGE', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 0}}', '[{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 20:37:03');
INSERT INTO `shop_order_items` VALUES (313, 242, 20, '经典拿铁', 28.00, 1, 32.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 0}}', '[{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 20:41:39');
INSERT INTO `shop_order_items` VALUES (314, 243, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 20:50:41');
INSERT INTO `shop_order_items` VALUES (315, 244, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 21:11:47');
INSERT INTO `shop_order_items` VALUES (316, 245, 20, '经典拿铁', 28.00, 1, 37.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 21:13:53');
INSERT INTO `shop_order_items` VALUES (317, 246, 20, '经典拿铁', 28.00, 1, 37.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 21:23:00');
INSERT INTO `shop_order_items` VALUES (318, 247, 20, '经典拿铁', 28.00, 1, 37.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 21:24:45');
INSERT INTO `shop_order_items` VALUES (319, 248, 20, '经典拿铁', 28.00, 1, 37.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"OAT\",\"price\":4}]', 0.00, '2026-01-15 22:35:10');
INSERT INTO `shop_order_items` VALUES (320, 249, 20, '经典拿铁', 28.00, 2, 80.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"OAT\",\"price\":4}]', 0.00, '2026-01-15 22:36:28');
INSERT INTO `shop_order_items` VALUES (321, 250, 26, '手冲精品咖啡 (SOE)', 50.00, 2, 100.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 22:36:44');
INSERT INTO `shop_order_items` VALUES (322, 251, 20, '经典拿铁', 28.00, 1, 37.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"OAT\",\"price\":4}]', 0.00, '2026-01-15 22:41:49');
INSERT INTO `shop_order_items` VALUES (323, 252, 28, '巴斯克芝士蛋糕', 35.00, 2, 70.00, NULL, NULL, NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 22:42:37');
INSERT INTO `shop_order_items` VALUES (324, 253, 19, 'Cozy 美式', 22.00, 2, 44.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 22:44:04');
INSERT INTO `shop_order_items` VALUES (325, 254, 20, '经典拿铁', 28.00, 1, 40.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 22:44:59');
INSERT INTO `shop_order_items` VALUES (326, 255, 20, '经典拿铁', 28.00, 1, 40.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"OAT\",\"price\":4}]', 0.00, '2026-01-15 22:50:16');
INSERT INTO `shop_order_items` VALUES (327, 256, 20, '经典拿铁', 28.00, 1, 40.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"OAT\",\"price\":4}]', 0.00, '2026-01-15 22:50:43');
INSERT INTO `shop_order_items` VALUES (328, 256, 20, '经典拿铁', 28.00, 1, 37.00, 'STANDARD', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"OAT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"OAT\",\"price\":4}]', 0.00, '2026-01-15 22:50:43');
INSERT INTO `shop_order_items` VALUES (329, 257, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 22:51:44');
INSERT INTO `shop_order_items` VALUES (330, 258, 21, '澳白', 30.00, 1, 30.00, NULL, 'NONE', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 22:51:56');
INSERT INTO `shop_order_items` VALUES (331, 259, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 22:53:10');
INSERT INTO `shop_order_items` VALUES (332, 260, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 22:53:36');
INSERT INTO `shop_order_items` VALUES (333, 261, 20, '经典拿铁', 28.00, 1, 32.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 0, \"milk\": 4, \"strength\": 0}}', '[{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 23:03:27');
INSERT INTO `shop_order_items` VALUES (334, 262, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:03:35');
INSERT INTO `shop_order_items` VALUES (335, 263, 21, '澳白', 30.00, 1, 30.00, NULL, 'STANDARD', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:03:43');
INSERT INTO `shop_order_items` VALUES (336, 264, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:05:57');
INSERT INTO `shop_order_items` VALUES (337, 265, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:06:06');
INSERT INTO `shop_order_items` VALUES (338, 266, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:06:11');
INSERT INTO `shop_order_items` VALUES (339, 267, 20, '经典拿铁', 28.00, 1, 40.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-15 23:14:43');
INSERT INTO `shop_order_items` VALUES (340, 268, 22, '生椰拿铁', 32.00, 1, 32.00, 'STANDARD', 'STANDARD', 'COLD', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:14:52');
INSERT INTO `shop_order_items` VALUES (341, 269, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:15:04');
INSERT INTO `shop_order_items` VALUES (342, 270, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:15:12');
INSERT INTO `shop_order_items` VALUES (343, 271, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:16:25');
INSERT INTO `shop_order_items` VALUES (344, 272, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:21:19');
INSERT INTO `shop_order_items` VALUES (345, 273, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:22:48');
INSERT INTO `shop_order_items` VALUES (346, 274, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:23:07');
INSERT INTO `shop_order_items` VALUES (347, 275, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:24:05');
INSERT INTO `shop_order_items` VALUES (348, 276, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:29:12');
INSERT INTO `shop_order_items` VALUES (349, 277, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:29:32');
INSERT INTO `shop_order_items` VALUES (350, 278, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:29:39');
INSERT INTO `shop_order_items` VALUES (351, 279, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:30:44');
INSERT INTO `shop_order_items` VALUES (352, 280, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:35:30');
INSERT INTO `shop_order_items` VALUES (353, 281, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:35:38');
INSERT INTO `shop_order_items` VALUES (354, 282, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:35:49');
INSERT INTO `shop_order_items` VALUES (355, 283, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:36:12');
INSERT INTO `shop_order_items` VALUES (356, 284, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:36:32');
INSERT INTO `shop_order_items` VALUES (357, 285, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:37:21');
INSERT INTO `shop_order_items` VALUES (358, 286, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:37:34');
INSERT INTO `shop_order_items` VALUES (359, 287, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:41:55');
INSERT INTO `shop_order_items` VALUES (360, 288, 24, '拿铁金·桂花特调', 38.00, 1, 38.00, NULL, 'STANDARD', 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:42:16');
INSERT INTO `shop_order_items` VALUES (361, 289, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:42:23');
INSERT INTO `shop_order_items` VALUES (362, 290, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, NULL, 'STANDARD', NULL, NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-15 23:42:55');
INSERT INTO `shop_order_items` VALUES (363, 291, 21, '澳白', 30.00, 1, 30.00, NULL, 'STANDARD', NULL, 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-16 08:15:43');
INSERT INTO `shop_order_items` VALUES (364, 292, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-16 08:16:59');
INSERT INTO `shop_order_items` VALUES (365, 293, 26, '手冲精品咖啡 (SOE)', 50.00, 1, 50.00, NULL, NULL, 'HOT', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-16 08:17:12');
INSERT INTO `shop_order_items` VALUES (366, 294, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-16 09:05:05');
INSERT INTO `shop_order_items` VALUES (367, 295, 20, '经典拿铁', 28.00, 1, 40.00, 'LARGE', 'STANDARD', 'HOT', 'STRONG', '{\"milkType\": \"COCONUT\", \"extraPrices\": {\"cup\": 3, \"milk\": 4, \"strength\": 5}}', '[{\"code\":\"EXTRA_SHOT\",\"name\":\"加浓\",\"price\":5},{\"code\":\"SPECIAL_MILK\",\"name\":\"COCONUT\",\"price\":4}]', 0.00, '2026-01-16 09:21:11');
INSERT INTO `shop_order_items` VALUES (368, 296, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-19 22:45:12');
INSERT INTO `shop_order_items` VALUES (369, 297, 25, '冰摇荔枝咖啡', 36.00, 1, 36.00, 'STANDARD', 'STANDARD', 'COLD', NULL, '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-19 22:45:18');
INSERT INTO `shop_order_items` VALUES (370, 298, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-19 22:47:48');
INSERT INTO `shop_order_items` VALUES (371, 299, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-19 22:47:57');
INSERT INTO `shop_order_items` VALUES (372, 300, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, 0.00, '2026-01-19 23:38:11');
INSERT INTO `shop_order_items` VALUES (373, 301, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-07 14:20:35');
INSERT INTO `shop_order_items` VALUES (374, 302, 22, '生椰拿铁', 32.00, 1, 32.00, 'STANDARD', 'STANDARD', 'COLD', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-07 14:25:23');
INSERT INTO `shop_order_items` VALUES (375, 303, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-07 14:31:16');
INSERT INTO `shop_order_items` VALUES (376, 304, 22, '生椰拿铁', 32.00, 1, 32.00, 'STANDARD', 'STANDARD', 'COLD', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-07 14:35:07');
INSERT INTO `shop_order_items` VALUES (377, 305, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-08 13:28:02');
INSERT INTO `shop_order_items` VALUES (378, 306, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-08 13:39:36');
INSERT INTO `shop_order_items` VALUES (379, 307, 20, '经典拿铁', 28.00, 1, 28.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"milkType\": \"WHOLE\", \"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-08 13:44:41');
INSERT INTO `shop_order_items` VALUES (380, 308, 19, 'Cozy 美式', 22.00, 1, 22.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-08 13:46:55');
INSERT INTO `shop_order_items` VALUES (381, 309, 21, '澳白', 30.00, 1, 30.00, 'STANDARD', 'STANDARD', 'HOT', 'NORMAL', '{\"extraPrices\": {\"cup\": 0, \"milk\": 0, \"strength\": 0}}', NULL, NULL, '2026-07-08 14:28:05');

-- ----------------------------
-- Table structure for shop_orders
-- ----------------------------
DROP TABLE IF EXISTS `shop_orders`;
CREATE TABLE `shop_orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单总额',
  `points_earned` int NOT NULL COMMENT '获得积分',
  `status` enum('pending','preparing','completed','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'è®¢å•çŠ¶æ€: pending=å¾…å¤„ç† preparing=åˆ¶ä½œä¸­ completed=å·²å®Œæˆ cancelled=å·²å–æ¶ˆ',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `store_id` bigint NOT NULL DEFAULT 1,
  `business_date` date NULL DEFAULT NULL,
  `pickup_code` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `pickup_code_generated_at` timestamp NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `total_quantity` int NOT NULL DEFAULT 0 COMMENT '商品总数量',
  `discount_amount` decimal(10, 2) NOT NULL COMMENT '优惠金额',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '实付金额（total_amount-discount_amount）',
  `applied_coupon_id` bigint NULL DEFAULT NULL COMMENT '使用的券实例ID(cozy_mall.user_coupons.id)',
  `exp_earned` int NOT NULL DEFAULT 0 COMMENT '获得EXP',
  `rewards_granted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '奖励已发放（幂等标记）',
  `points_multiplier` decimal(3, 1) NULL DEFAULT 1.0,
  `dining_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用餐方式: DINE_IN(堂食)/TAKEOUT(外带)/DELIVERY(外卖)',
  `applied_addon_coupon_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'v5.0 附加券ID列表，JSON格式如 [1,2,3]，用于取消时回滚',
  `delivery_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '配送费金额',
  `delivery_fee_waived` tinyint(1) NULL DEFAULT 0 COMMENT '配送费是否已减免',
  `delivery_fee_waived_reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '减免原因 (BLACK_GOLD_UNLIMITED/COUPON)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_status_created`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_orders_dining_method`(`dining_method` ASC) USING BTREE,
  INDEX `idx_delivery_fee_waived`(`delivery_fee_waived` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 342 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咖啡消费订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of shop_orders
-- ----------------------------
INSERT INTO `shop_orders` VALUES (1, 'CF202512221622514274', 19, 18.00, 36, 'completed', NULL, '2025-12-22 16:22:52', 1, NULL, NULL, NULL, '2025-12-26 21:07:29', 1, 0.00, 18.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (2, 'CF202512221833349838', 19, 18.00, 36, 'completed', NULL, NULL, 1, NULL, NULL, NULL, '2025-12-26 21:07:29', 1, 0.00, 18.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (3, 'CF202512221833503423', 19, 36.00, 72, 'completed', NULL, NULL, 1, NULL, NULL, NULL, '2025-12-26 21:07:29', 2, 0.00, 36.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (4, 'CF202512221835136143', 19, 18.00, 36, 'completed', NULL, NULL, 1, NULL, NULL, NULL, '2025-12-26 21:07:29', 1, 0.00, 18.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (5, 'CF202512221842073623', 19, 25.00, 50, 'cancelled', NULL, NULL, 1, NULL, NULL, NULL, '2025-12-26 21:07:29', 1, 0.00, 25.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (6, 'CF202512231622571666', 23, 18.00, 18, 'completed', 'pending', NULL, 1, '2025-12-23', '001', '2025-12-23 16:22:58', '2025-12-26 21:07:29', 1, 0.00, 18.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (7, 'CF202512231623269190', 23, 18.00, 18, 'cancelled', 'pending', NULL, 1, '2025-12-23', '002', '2025-12-23 16:23:27', '2025-12-26 21:07:29', 1, 0.00, 18.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (8, 'CF202512231625103943', 23, 25.00, 25, 'completed', 'pending', NULL, 1, '2025-12-23', '003', '2025-12-23 16:25:10', '2025-12-26 21:07:29', 1, 0.00, 25.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (9, 'CF202512231625251682', 23, 60.00, 60, 'cancelled', 'pending', NULL, 1, '2025-12-23', '004', '2025-12-23 16:25:26', '2025-12-26 21:07:29', 2, 0.00, 60.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (10, 'CF202512231625498245', 23, 30.00, 30, 'cancelled', 'pending', NULL, 1, '2025-12-23', '005', '2025-12-23 16:25:50', '2025-12-26 21:07:29', 1, 0.00, 30.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (11, 'CF202512231625545759', 23, 30.00, 30, 'cancelled', 'pending', NULL, 1, '2025-12-23', '006', '2025-12-23 16:25:55', '2025-12-26 21:07:29', 1, 0.00, 30.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (12, 'CF202512231625587913', 23, 30.00, 30, 'cancelled', 'pending', NULL, 1, '2025-12-23', '007', '2025-12-23 16:25:59', '2025-12-26 21:07:29', 1, 0.00, 30.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (13, 'CF202512231626252476', 23, 30.00, 30, 'cancelled', 'pending', NULL, 1, '2025-12-23', '008', '2025-12-23 16:26:26', '2025-12-26 21:07:29', 1, 0.00, 30.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (14, 'CF202512231626308127', 23, 30.00, 30, 'cancelled', 'pending', NULL, 1, '2025-12-23', '009', '2025-12-23 16:26:31', '2025-12-26 21:07:29', 1, 0.00, 30.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (15, 'CF202512231626341570', 23, 30.00, 30, 'cancelled', 'pending', NULL, 1, '2025-12-23', '010', '2025-12-23 16:26:35', '2025-12-26 21:07:29', 1, 0.00, 30.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (16, 'CF202512231659275125', 23, 30.00, 30, 'cancelled', 'pending', NULL, 1, '2025-12-23', '011', '2025-12-23 16:59:27', '2025-12-26 21:07:29', 1, 0.00, 30.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (17, 'CF202512231718517464', 23, 18.00, 18, 'cancelled', 'pending', '2025-12-23 17:18:52', 1, '2025-12-23', '012', '2025-12-23 17:18:52', '2025-12-26 21:07:29', 1, 0.00, 18.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (18, 'CF202512231719155329', 23, 18.00, 18, 'completed', 'pending', '2025-12-23 17:19:15', 1, '2025-12-23', '013', '2025-12-23 17:19:15', '2025-12-26 21:07:29', 1, 0.00, 18.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (19, 'CF202512231719232011', 23, 50.00, 50, 'cancelled', 'pending', '2025-12-23 17:19:24', 1, '2025-12-23', '014', '2025-12-23 17:19:24', '2025-12-26 21:07:29', 2, 0.00, 50.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (20, 'CF202512241757161529', 23, 56.00, 67, 'completed', '不要香菜', '2025-12-24 17:57:17', 1, '2025-12-24', '001', '2025-12-24 17:57:17', '2025-12-26 21:07:29', 2, 0.00, 56.00, NULL, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (21, 'CF202512280856043195', 39, 18.00, 23, 'completed', 'Frontend Simulation Order', '2025-12-28 08:56:05', 1, '2025-12-28', '001', '2025-12-28 08:56:05', NULL, 1, 0.00, 18.00, NULL, 18, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (22, 'CF202512280906447744', 39, 66.00, 0, 'cancelled', '手动测试-TC01', '2025-12-28 09:06:44', 1, '2025-12-28', '002', '2025-12-28 09:06:44', NULL, 3, 0.00, 66.00, NULL, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (23, 'CF202512280924362855', 39, 66.00, 86, 'completed', '手动测试-TC01', '2025-12-28 09:24:36', 1, '2025-12-28', '003', '2025-12-28 09:24:36', NULL, 3, 0.00, 66.00, NULL, 66, 1, 1.3, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (24, 'CF202512281123475951', 22, 18.00, 0, 'cancelled', '手动测试-TC06', '2025-12-28 11:23:47', 1, '2025-12-28', '004', '2025-12-28 11:23:47', NULL, 1, 18.00, 0.00, NULL, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (25, 'CF202512281246103910', 22, 20.00, 0, 'cancelled', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 12:46:11', 1, '2025-12-28', '005', '2025-12-28 12:46:11', NULL, 1, 20.00, 0.00, 10, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (26, 'CF202512281251253563', 22, 20.00, 0, 'cancelled', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 12:51:25', 1, '2025-12-28', '006', '2025-12-28 12:51:25', NULL, 1, 20.00, 0.00, 10, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (27, 'CF202512281304227554', 22, 20.00, 0, 'cancelled', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 13:04:23', 1, '2025-12-28', '007', '2025-12-28 13:04:23', NULL, 1, 20.00, 0.00, 10, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (28, 'CF202512281313326815', 22, 20.00, 0, 'cancelled', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 13:13:32', 1, '2025-12-28', '008', '2025-12-28 13:13:32', NULL, 1, 20.00, 0.00, 10, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (29, 'CF202512281337285115', 22, 20.00, 0, 'cancelled', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 13:37:29', 1, '2025-12-28', '009', '2025-12-28 13:37:29', NULL, 1, 20.00, 0.00, 12, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (30, 'CF202512281353145440', 22, 20.00, 0, 'cancelled', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 13:53:14', 1, '2025-12-28', '010', '2025-12-28 13:53:14', NULL, 1, 20.00, 0.00, 12, 0, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (31, 'CF202512281357512381', 22, 30.00, 0, 'completed', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 13:57:51', 1, '2025-12-28', '011', '2025-12-28 13:57:51', NULL, 1, 30.00, 0.00, 12, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (32, 'CF202512281359195506', 22, 30.00, 51, 'cancelled', '手动测试-TC06 抹茶拿铁兑换券', '2025-12-28 13:59:20', 1, '2025-12-28', '012', '2025-12-28 13:59:20', NULL, 1, 0.00, 30.00, 13, 30, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (33, 'CF202512281404446010', 22, 55.00, 77, 'cancelled', '手动测试-TC06 满减券50-10 未到达门槛', '2025-12-28 14:04:45', 1, '2025-12-28', '013', '2025-12-28 14:04:45', NULL, 2, 10.00, 45.00, 13, 45, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (34, 'CF202512281442282252', 22, 30.00, 43, 'cancelled', '手动测试-TC06 折扣券85', '2025-12-28 14:42:28', 1, '2025-12-28', '014', '2025-12-28 14:42:28', NULL, 1, 4.50, 25.50, 14, 26, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (35, 'CF202512281525182965', 22, 30.00, 43, 'completed', '手动测试-TC06 折扣券85', '2025-12-28 15:25:18', 1, '2025-12-28', '015', '2025-12-28 15:25:18', NULL, 1, 4.50, 25.50, 14, 26, 1, 1.7, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (36, 'CF202512281540439236', 22, 30.00, 43, 'completed', '手动测试-TC06 折扣券85', '2025-12-28 15:40:44', 1, '2025-12-28', '016', '2025-12-28 15:40:44', NULL, 1, 4.50, 25.50, 15, 26, 1, 1.7, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (37, 'CF202512281548342460', 22, 30.00, 0, 'completed', '手动测试-TC06 折扣券85', '2025-12-28 15:48:34', 1, '2025-12-28', '017', '2025-12-28 15:48:34', '2026-01-05 18:30:50', 1, 30.00, 0.00, 16, 0, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (38, 'CF202512281551228067', 22, 228.00, 388, 'completed', '手动测试-TC06 测试黑金加速卡边界', '2025-12-28 15:51:23', 1, '2025-12-28', '018', '2025-12-28 15:51:23', NULL, 1, 0.00, 228.00, NULL, 228, 1, 1.7, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (39, 'CF202512281552244378', 22, 30.00, 48, 'completed', '手动测试-TC06 测试黑金加速卡边界', '2025-12-28 15:52:24', 1, '2025-12-28', '019', '2025-12-28 15:52:24', NULL, 1, 0.00, 30.00, NULL, 30, 1, 1.6, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (40, 'CF202512281720286186', 22, 25.00, 34, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:20:28', 1, '2025-12-28', '020', '2025-12-28 17:20:28', NULL, 1, 0.00, 25.00, NULL, 25, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (41, 'CF202512281720506848', 22, 25.00, 34, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:20:50', 1, '2025-12-28', '021', '2025-12-28 17:20:50', NULL, 1, 0.00, 25.00, NULL, 25, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (42, 'CF202512281721406631', 22, 25.00, 34, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:21:40', 1, '2025-12-28', '022', '2025-12-28 17:21:40', NULL, 1, 0.00, 25.00, NULL, 25, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (43, 'CF202512281725473081', 22, 25.00, 34, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:25:48', 1, '2025-12-28', '023', '2025-12-28 17:25:48', NULL, 1, 0.00, 25.00, NULL, 25, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (44, 'CF202512281741203361', 22, 25.00, 34, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:41:21', 1, '2025-12-28', '024', '2025-12-28 17:41:21', NULL, 1, 0.00, 25.00, NULL, 25, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (45, 'CF202512281741525029', 22, 25.00, 34, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:41:52', 1, '2025-12-28', '025', '2025-12-28 17:41:52', NULL, 1, 0.00, 25.00, NULL, 25, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (46, 'CF202512281751045261', 22, 30.00, 41, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:51:04', 1, '2025-12-28', '026', '2025-12-28 17:51:04', NULL, 1, 0.00, 30.00, NULL, 30, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (47, 'CF202512281751252061', 22, 30.00, 41, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:51:25', 1, '2025-12-28', '027', '2025-12-28 17:51:25', NULL, 1, 0.00, 30.00, NULL, 30, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (48, 'CF202512281752096989', 22, 30.00, 41, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 17:52:10', 1, '2025-12-28', '028', '2025-12-28 17:52:10', NULL, 1, 0.00, 30.00, NULL, 30, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (49, 'CF202512281827543746', 38, 30.00, 30, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 18:27:54', 1, '2025-12-28', '029', '2025-12-28 18:27:54', NULL, 1, 0.00, 30.00, NULL, 30, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (50, 'CF202512281829528910', 38, 30.00, 30, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 18:29:52', 1, '2025-12-28', '030', '2025-12-28 18:29:52', NULL, 1, 0.00, 30.00, NULL, 30, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (51, 'CF202512281830012040', 38, 30.00, 30, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 18:30:02', 1, '2025-12-28', '031', '2025-12-28 18:30:02', NULL, 1, 0.00, 30.00, NULL, 30, 0, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (52, 'CF202512281832511663', 22, 30.00, 41, 'cancelled', '手动测试-TC06 测试sse反馈', '2025-12-28 18:32:52', 1, '2025-12-28', '032', '2025-12-28 18:32:52', NULL, 1, 0.00, 30.00, NULL, 30, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (54, 'CF202512311638161546', 41, 50.00, 50, 'completed', 'v4.2-manual-test', '2025-12-31 16:38:16', 1, '2025-12-31', '002', '2025-12-31 16:38:16', NULL, 2, 0.00, 50.00, NULL, 50, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (55, 'CF202512311647066338', 41, 50.00, 50, 'completed', 'v4.2-manual-test', '2025-12-31 16:47:07', 1, '2025-12-31', '003', '2025-12-31 16:47:07', NULL, 2, 0.00, 50.00, NULL, 50, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (56, 'CF202512311705575749', 41, 250.00, 250, 'completed', '300挡测试', '2025-12-31 17:05:57', 1, '2025-12-31', '004', '2025-12-31 17:05:57', NULL, 10, 0.00, 250.00, NULL, 250, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (57, 'CF202512311708371364', 41, 300.00, 300, 'completed', '300挡测试', '2025-12-31 17:08:37', 1, '2025-12-31', '005', '2025-12-31 17:08:37', NULL, 12, 0.00, 300.00, NULL, 300, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (58, 'CF202512311709104819', 41, 375.00, 413, 'completed', '300挡测试', '2025-12-31 17:09:11', 1, '2025-12-31', '006', '2025-12-31 17:09:11', NULL, 15, 0.00, 375.00, NULL, 375, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (59, 'CF202512311709178683', 41, 375.00, 375, 'completed', '300挡测试', '2025-12-31 17:09:17', 1, '2025-12-31', '007', '2025-12-31 17:09:17', NULL, 15, 0.00, 375.00, NULL, 375, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (60, 'CF202512311807112287', 41, 25.00, 28, 'completed', 'order-with-coupon', '2025-12-31 18:07:11', 1, '2025-12-31', '008', '2025-12-31 18:07:11', NULL, 1, 3.75, 21.25, 18, 21, 1, 1.1, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (61, 'CF202601011508308596', 41, 250.00, 299, 'completed', 'order-with-coupon', '2026-01-01 15:08:30', 1, '2026-01-01', '001', '2026-01-01 15:08:30', NULL, 10, 20.00, 230.00, 26, 230, 1, 1.3, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (62, 'CF202601011508425032', 41, 250.00, 325, 'completed', 'order-with-coupon', '2026-01-01 15:08:43', 1, '2026-01-01', '002', '2026-01-01 15:08:43', NULL, 10, 0.00, 250.00, NULL, 250, 1, 1.3, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (63, 'CF202601011705547437', 41, 25.00, 33, 'completed', 'order-with-coupon', '2026-01-01 17:05:54', 1, '2026-01-01', '003', '2026-01-01 17:05:54', NULL, 1, 0.00, 25.00, NULL, 25, 1, 1.3, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (64, 'CF202601011800558706', 41, 25.00, 34, 'completed', 'order-with-coupon', '2026-01-01 18:00:56', 1, '2026-01-01', '004', '2026-01-01 18:00:56', NULL, 1, 0.00, 25.00, NULL, 25, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (65, 'CF202601021050433025', 41, 25.00, 34, 'completed', 'order-with-coupon', '2026-01-02 10:50:43', 1, '2026-01-02', '001', '2026-01-02 10:50:43', NULL, 1, 0.00, 25.00, NULL, 25, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (66, 'CF202601021102011086', 41, 300.00, 390, 'completed', NULL, '2026-01-02 11:02:01', 1, '2026-01-02', '002', '2026-01-02 11:02:01', NULL, 1, 0.00, 300.00, NULL, 300, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (67, 'CF202601021102412631', 41, 20.00, 27, 'completed', NULL, '2026-01-02 11:02:42', 1, '2026-01-02', '003', '2026-01-02 11:02:42', NULL, 1, 0.00, 20.00, NULL, 20, 1, 1.3, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (68, 'CF202601021125219552', 43, 300.00, 390, 'completed', NULL, '2026-01-02 11:25:22', 1, '2026-01-02', '004', '2026-01-02 11:25:22', NULL, 1, 0.00, 300.00, NULL, 300, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (69, 'CF202601021130484338', 43, 300.00, 430, 'completed', NULL, '2026-01-02 11:30:48', 1, '2026-01-02', '005', '2026-01-02 11:30:48', NULL, 1, 0.00, 300.00, NULL, 300, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (70, 'CF202601021810274657', 43, 40.00, 54, 'completed', '', '2026-01-02 18:10:28', 1, '2026-01-02', '006', '2026-01-02 18:10:28', NULL, 2, 0.00, 40.00, NULL, 40, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (71, 'CF202601021836521772', 43, 61.00, 55, 'completed', '', '2026-01-02 18:36:52', 1, '2026-01-02', '007', '2026-01-02 18:36:52', NULL, 3, 20.00, 41.00, 30, 41, 1, 1.3, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (72, 'CF202601021841438238', 43, 20.00, 27, 'completed', '', '2026-01-02 18:41:43', 1, '2026-01-02', '008', '2026-01-02 18:41:43', NULL, 1, 0.00, 20.00, NULL, 20, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (73, 'CF202601021846473402', 43, 20.00, 27, 'completed', '', '2026-01-02 18:46:47', 1, '2026-01-02', '009', '2026-01-02 18:46:47', NULL, 1, 0.00, 20.00, NULL, 20, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (74, 'CF202601022211018117', 43, 3.00, 0, 'completed', '少甜', '2026-01-02 22:11:02', 1, '2026-01-02', '010', '2026-01-02 22:11:02', NULL, 3, 3.00, 0.00, 29, 0, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (75, 'CF202601022232555262', 43, 20.00, 27, 'completed', '', '2026-01-02 22:32:55', 1, '2026-01-02', '011', '2026-01-02 22:32:55', NULL, 1, 0.00, 20.00, NULL, 20, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (76, 'CF202601022238546210', 43, 20.00, 27, 'cancelled', '', '2026-01-02 22:38:54', 1, '2026-01-02', '012', '2026-01-02 22:38:54', NULL, 1, 0.00, 20.00, NULL, 20, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (77, 'CF202601022248521485', 43, 20.00, 34, 'completed', '', '2026-01-02 22:48:53', 1, '2026-01-02', '013', '2026-01-02 22:48:53', NULL, 1, 0.00, 20.00, NULL, 20, 1, 1.7, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (78, 'CF202601030008564557', 43, 50.00, 0, 'completed', '', '2026-01-03 00:08:57', 1, '2026-01-02', '014', '2026-01-03 00:08:57', NULL, 2, 30.00, 20.00, 31, 20, 1, 1.0, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (79, 'CF202601030024093293', 43, 55.00, 40, 'completed', '', '2026-01-03 00:24:09', 1, '2026-01-02', '015', '2026-01-03 00:24:09', NULL, 2, 30.00, 25.00, 32, 25, 1, 1.6, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (80, 'CF202601030032549885', 43, 55.00, 47, 'completed', '', '2026-01-03 00:32:55', 1, '2026-01-02', '016', '2026-01-03 00:32:55', NULL, 2, 25.00, 30.00, 33, 30, 1, 1.6, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (81, 'CF202601030040581423', 43, 20.00, 27, 'cancelled', '', '2026-01-03 00:40:58', 1, '2026-01-02', '017', '2026-01-03 00:40:58', NULL, 1, 0.00, 20.00, NULL, 20, 0, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (82, 'CF202601030041209801', 43, 20.00, 27, 'completed', '', '2026-01-03 00:41:20', 1, '2026-01-02', '018', '2026-01-03 00:41:20', NULL, 1, 0.00, 20.00, NULL, 20, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (83, 'CF202601030043134808', 43, 20.00, 27, 'completed', '', '2026-01-03 00:43:13', 1, '2026-01-02', '019', '2026-01-03 00:43:13', NULL, 1, 0.00, 20.00, NULL, 20, 1, 1.4, NULL, NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (84, 'CF202601042311272313', 43, 34.00, 41, 'cancelled', '', '2026-01-04 23:11:27', 1, '2026-01-04', '001', '2026-01-04 23:11:27', '2026-01-04 23:11:27', 2, 0.00, 34.00, NULL, 34, 0, 1.2, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (85, 'CF202601042337594704', 43, 28.00, 34, 'cancelled', '', '2026-01-04 23:37:59', 1, '2026-01-04', '002', '2026-01-04 23:37:59', '2026-01-04 23:37:59', 1, 0.00, 28.00, NULL, 28, 0, 1.2, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (86, 'CF202601042356542106', 41, 28.00, 48, 'cancelled', '', '2026-01-04 23:56:54', 1, '2026-01-04', '003', '2026-01-04 23:56:54', '2026-01-04 23:56:55', 1, 0.00, 28.00, NULL, 28, 0, 1.7, 'TAKEOUT', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (87, 'CF202601042357155095', 41, 28.00, 48, 'cancelled', '', '2026-01-04 23:57:15', 1, '2026-01-04', '004', '2026-01-04 23:57:15', '2026-01-04 23:57:15', 1, 0.00, 28.00, NULL, 28, 0, 1.7, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (88, 'CF202601050019544626', 41, 30.00, 51, 'completed', '', '2026-01-05 00:19:54', 1, '2026-01-04', '005', '2026-01-05 00:19:54', '2026-01-05 00:19:54', 1, 0.00, 30.00, NULL, 30, 1, 1.7, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (89, 'CF202601050029337527', 43, 30.00, 36, 'cancelled', '', '2026-01-05 00:29:33', 1, '2026-01-04', '006', '2026-01-05 00:29:33', '2026-01-05 00:29:33', 1, 0.00, 30.00, NULL, 30, 0, 1.2, 'TAKEOUT', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (90, 'CF202601050035534725', 43, 48.00, 58, 'cancelled', '', '2026-01-05 00:35:54', 1, '2026-01-04', '007', '2026-01-05 00:35:54', '2026-01-05 00:35:54', 2, 0.00, 48.00, NULL, 48, 0, 1.2, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (91, 'CF202601050036495928', 43, 48.00, 58, 'cancelled', '', '2026-01-05 00:36:50', 1, '2026-01-04', '008', '2026-01-05 00:36:50', '2026-01-05 00:36:50', 2, 0.00, 48.00, NULL, 48, 0, 1.2, 'TAKEOUT', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (92, 'CF202601051029061595', 43, 28.00, 36, 'cancelled', '', '2026-01-05 10:29:06', 1, '2026-01-05', NULL, '2026-01-05 10:29:06', '2026-01-05 10:29:06', 1, 0.00, 28.00, NULL, 28, 0, 1.3, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (93, 'CF202601051030547536', 43, 121.00, 157, 'cancelled', '', '2026-01-05 10:30:55', 1, '2026-01-05', '001', '2026-01-05 10:30:55', '2026-01-05 10:30:55', 4, 0.00, 121.00, NULL, 121, 0, 1.3, 'TAKEOUT', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (94, 'CF202601051042221001', 43, 32.00, 42, 'cancelled', '', '2026-01-05 10:42:23', 1, '2026-01-05', '002', '2026-01-05 10:42:23', '2026-01-05 10:42:23', 1, 0.00, 32.00, NULL, 32, 0, 1.3, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (95, 'CF202601051056197779', 43, 30.00, 39, 'cancelled', '', '2026-01-05 10:56:20', 1, '2026-01-05', '003', '2026-01-05 10:56:20', '2026-01-05 10:56:20', 1, 0.00, 30.00, NULL, 30, 0, 1.3, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (96, 'CF202601051931083818', 43, 28.00, 36, 'preparing', '', '2026-01-05 19:31:08', 1, '2026-01-05', '004', '2026-01-05 19:31:08', '2026-01-05 19:31:08', 1, 0.00, 28.00, NULL, 28, 0, 1.3, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (97, 'CF202601061226016730', 43, 64.00, 81, 'preparing', '', '2026-01-06 12:26:02', 1, '2026-01-06', '001', '2026-01-06 12:26:02', '2026-01-06 12:26:02', 2, 10.00, 54.00, 34, 54, 0, 1.5, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (98, 'CF202601061226516842', 43, 22.00, 33, 'cancelled', '', '2026-01-06 12:26:52', 1, '2026-01-06', '002', '2026-01-06 12:26:52', '2026-01-06 12:26:52', 1, 0.00, 22.00, NULL, 22, 0, 1.5, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (99, 'CF202601101459248359', 43, 40.00, 45, 'cancelled', '', '2026-01-10 14:59:25', 1, '2026-01-10', '001', '2026-01-10 14:59:25', '2026-01-10 14:59:25', 2, 10.00, 30.00, 47, 30, 0, 1.5, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (100, 'CF202601101502282746', 43, 38.00, 57, 'completed', '', '2026-01-10 15:02:28', 1, '2026-01-10', '002', '2026-01-10 15:02:28', '2026-01-10 15:02:28', 1, 0.00, 38.00, NULL, 38, 1, 1.5, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (101, 'CF202601101545493981', 43, 28.00, 27, 'cancelled', '', '2026-01-10 15:45:50', 1, '2026-01-10', '003', '2026-01-10 15:45:50', '2026-01-10 15:45:50', 1, 0.00, 18.00, NULL, 18, 0, 1.5, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (102, 'CF202601101548061660', 43, 22.00, 33, 'completed', '', '2026-01-10 15:48:07', 1, '2026-01-10', '004', '2026-01-10 15:48:07', '2026-01-10 15:48:07', 1, 0.00, 22.00, NULL, 22, 1, 1.5, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (103, 'CF202601101552113091', 43, 22.00, 0, 'cancelled', '', '2026-01-10 15:52:12', 1, '2026-01-10', '005', '2026-01-10 15:52:12', '2026-01-10 15:52:12', 1, 22.00, 0.00, 43, 0, 0, 1.0, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (104, 'CF202601101556347502', 43, 28.00, 42, 'completed', '', '2026-01-10 15:56:35', 1, '2026-01-10', '006', '2026-01-10 15:56:35', '2026-01-10 15:56:35', 1, 0.00, 28.00, NULL, 28, 1, 1.5, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (105, 'CF202601101600252427', 43, 22.00, 33, 'completed', '', '2026-01-10 16:00:25', 1, '2026-01-10', '007', '2026-01-10 16:00:25', '2026-01-10 16:00:25', 1, 0.00, 22.00, NULL, 22, 1, 1.5, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (106, 'CF202601102124305069', 49, 35.00, 18, 'cancelled', '', '2026-01-10 21:24:31', 1, '2026-01-10', '008', '2026-01-10 21:24:31', '2026-01-10 21:24:31', 1, 17.50, 17.50, 52, 18, 0, 1.0, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (107, 'CF202601102231022777', 50, 38.00, 19, 'completed', '', '2026-01-10 22:31:02', 1, '2026-01-10', '009', '2026-01-10 22:31:02', '2026-01-10 22:31:02', 1, 19.00, 19.00, 53, 19, 1, 1.0, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (108, 'CF202601102244411826', 43, 44.00, 33, 'completed', '', '2026-01-10 22:44:41', 1, '2026-01-10', '010', '2026-01-10 22:44:41', '2026-01-10 22:44:41', 2, 22.00, 22.00, 54, 22, 1, 1.5, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (109, 'CF202601102302595592', 50, 38.00, 38, 'cancelled', '', '2026-01-10 23:02:59', 1, '2026-01-10', '011', '2026-01-10 23:02:59', '2026-01-10 23:02:59', 1, 0.00, 38.00, NULL, 38, 0, 1.0, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (110, 'CF202601102303401876', 50, 50.00, 50, 'cancelled', '', '2026-01-10 23:03:40', 1, '2026-01-10', '012', '2026-01-10 23:03:40', '2026-01-10 23:03:40', 1, 0.00, 50.00, NULL, 50, 0, 1.0, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (111, 'CF202601102309599058', 50, 22.00, 22, 'completed', '', '2026-01-10 23:09:59', 1, '2026-01-10', '013', '2026-01-10 23:09:59', '2026-01-10 23:09:59', 1, 0.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (112, 'CF202601102328292959', 50, 36.00, 36, 'completed', '', '2026-01-10 23:28:30', 1, '2026-01-10', '014', '2026-01-10 23:28:30', '2026-01-10 23:28:30', 1, 0.00, 36.00, NULL, 36, 1, 1.0, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (113, 'CF202601102328426989', 50, 50.00, 50, 'completed', '', '2026-01-10 23:28:43', 1, '2026-01-10', '015', '2026-01-10 23:28:43', '2026-01-10 23:28:43', 1, 0.00, 50.00, NULL, 50, 1, 1.0, 'DELIVERY', NULL, NULL, 0, NULL);
INSERT INTO `shop_orders` VALUES (114, 'CF202601111221384455', 43, 28.00, 35, 'cancelled', '', '2026-01-11 12:21:38', 1, '2026-01-11', '001', '2026-01-11 12:21:38', '2026-01-11 12:21:38', 1, 0.00, 23.00, NULL, 23, 0, 1.5, 'DELIVERY', NULL, 5.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (115, 'CF202601111221574823', 43, 28.00, 35, 'cancelled', '', '2026-01-11 12:21:57', 1, '2026-01-11', '002', '2026-01-11 12:21:57', '2026-01-11 12:21:57', 1, 0.00, 23.00, NULL, 23, 0, 1.5, 'DELIVERY', NULL, 5.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (116, 'CF202601111223384799', 43, 28.00, 42, 'cancelled', '', '2026-01-11 12:23:38', 1, '2026-01-11', '003', '2026-01-11 12:23:38', '2026-01-11 12:23:38', 1, 0.00, 28.00, NULL, 28, 0, 1.5, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (117, 'CF202601111224457431', 43, 28.00, 35, 'cancelled', '', '2026-01-11 12:24:46', 1, '2026-01-11', '004', '2026-01-11 12:24:46', '2026-01-11 12:24:46', 1, 0.00, 23.00, NULL, 23, 0, 1.5, 'DELIVERY', NULL, 5.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (118, 'CF202601111225056821', 43, 22.00, 26, 'cancelled', '', '2026-01-11 12:25:05', 1, '2026-01-11', '005', '2026-01-11 12:25:05', '2026-01-11 12:25:05', 1, 0.00, 17.00, NULL, 17, 0, 1.5, 'DELIVERY', NULL, 5.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (119, 'CF202601111229166588', 43, 28.00, 42, 'cancelled', '', '2026-01-11 12:29:16', 1, '2026-01-11', '006', '2026-01-11 12:29:16', '2026-01-11 12:29:16', 1, 0.00, 28.00, NULL, 28, 0, 1.5, 'DELIVERY', NULL, 5.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (120, 'CF202601111551506364', 48, 28.00, 28, 'completed', '', '2026-01-11 15:51:51', 1, '2026-01-11', '007', '2026-01-11 15:51:51', '2026-01-11 15:51:51', 1, 0.00, 28.00, NULL, 28, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (121, 'CF202601111552594109', 48, 22.00, 22, 'cancelled', '', '2026-01-11 15:52:59', 1, '2026-01-11', '008', '2026-01-11 15:52:59', '2026-01-11 15:52:59', 1, 0.00, 22.00, NULL, 22, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (122, 'CF202601111607424143', 48, 22.00, 22, 'cancelled', '', '2026-01-11 16:07:43', 1, '2026-01-11', '009', '2026-01-11 16:07:43', '2026-01-11 16:07:43', 1, 0.00, 22.00, NULL, 22, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (123, 'CF202601111613343508', 48, 22.00, 22, 'cancelled', '', '2026-01-11 16:13:35', 1, '2026-01-11', '010', '2026-01-11 16:13:35', '2026-01-11 16:13:35', 1, 0.00, 22.00, NULL, 22, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (124, 'CF202601111620067969', 48, 22.00, 22, 'cancelled', '', '2026-01-11 16:20:06', 1, '2026-01-11', '011', '2026-01-11 16:20:06', '2026-01-11 16:20:06', 1, 0.00, 22.00, NULL, 22, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (125, 'CF202601111621417734', 48, 28.00, 14, 'cancelled', '', '2026-01-11 16:21:41', 1, '2026-01-11', '012', '2026-01-11 16:21:41', '2026-01-11 16:21:41', 1, 14.00, 14.00, 51, 14, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (126, 'CF202601111627428813', 48, 22.00, 22, 'cancelled', '', '2026-01-11 16:27:43', 1, '2026-01-11', '013', '2026-01-11 16:27:43', '2026-01-11 16:27:43', 1, 0.00, 22.00, NULL, 22, 0, 1.0, 'DINE_IN', '[70]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (127, 'CF202601111628285012', 48, 22.00, 22, 'completed', '', '2026-01-11 16:28:29', 1, '2026-01-11', '014', '2026-01-11 16:28:29', '2026-01-11 16:28:29', 1, 0.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', '[70]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (128, 'CF202601111634077531', 49, 22.00, 24, 'cancelled', '', '2026-01-11 16:34:08', 1, '2026-01-11', '015', '2026-01-11 16:34:08', '2026-01-11 16:34:08', 1, 0.00, 22.00, NULL, 22, 0, 1.1, 'DELIVERY', '[68,67,69]', 5.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (129, 'CF202601111643085880', 49, 22.00, 12, 'cancelled', '', '2026-01-11 16:43:09', 1, '2026-01-11', '016', '2026-01-11 16:43:09', '2026-01-11 16:43:09', 1, 11.00, 11.00, 52, 11, 0, 1.1, 'DINE_IN', '[68,69]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (130, 'CF202601111657105031', 49, 44.00, 48, 'completed', '', '2026-01-11 16:57:11', 1, '2026-01-11', '017', '2026-01-11 16:57:11', '2026-01-11 16:57:11', 2, 0.00, 44.00, NULL, 44, 1, 1.1, 'DELIVERY', '[68,69,67]', 5.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (131, 'CF202601111716538129', 43, 30.00, 20, 'cancelled', '', '2026-01-11 17:16:53', 1, '2026-01-11', '018', '2026-01-11 17:16:53', '2026-01-11 17:16:53', 1, 15.00, 15.00, 45, 15, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (132, 'CF202601111718093557', 47, 28.00, 30, 'cancelled', '', '2026-01-11 17:18:10', 1, '2026-01-11', '019', '2026-01-11 17:18:10', '2026-01-11 17:18:10', 1, 3.36, 24.64, 74, 25, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (133, 'CF202601111808428500', 51, 28.00, 14, 'cancelled', '', '2026-01-11 18:08:43', 1, '2026-01-11', '020', '2026-01-11 18:08:43', '2026-01-11 18:08:43', 1, 14.00, 14.00, 79, 14, 0, 1.0, 'DINE_IN', '[80]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (134, 'CF202601111809342832', 51, 22.00, 11, 'cancelled', '', '2026-01-11 18:09:35', 1, '2026-01-11', '021', '2026-01-11 18:09:35', '2026-01-11 18:09:35', 1, 11.00, 11.00, 79, 11, 0, 1.0, 'DINE_IN', '[80]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (135, 'CF202601111810449916', 51, 22.00, 22, 'cancelled', '', '2026-01-11 18:10:44', 1, '2026-01-11', '022', '2026-01-11 18:10:44', '2026-01-11 18:10:44', 1, 0.00, 22.00, NULL, 22, 0, 1.0, 'DINE_IN', '[80]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (136, 'CF202601111815482169', 51, 50.00, 30, 'cancelled', '', '2026-01-11 18:15:49', 1, '2026-01-11', '023', '2026-01-11 18:15:49', '2026-01-11 18:15:49', 2, 20.00, 30.00, 79, 30, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (137, 'CF202601121021458245', 43, 22.00, 29, 'completed', '', '2026-01-12 10:21:45', 1, '2026-01-12', '001', '2026-01-12 10:21:45', '2026-01-12 10:21:45', 1, 0.00, 22.00, NULL, 22, 1, 1.3, 'DINE_IN', '[66]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (138, 'CF202601121028588588', 43, 22.00, 0, 'completed', '', '2026-01-12 10:28:59', 1, '2026-01-12', '002', '2026-01-12 10:28:59', '2026-01-12 10:28:59', 1, 999.00, 0.00, 64, 0, 1, 1.0, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (139, 'CF202601121032291259', 43, 35.00, 0, 'completed', '', '2026-01-12 10:32:30', 1, '2026-01-12', '003', '2026-01-12 10:32:30', '2026-01-12 10:32:30', 1, 40.00, 0.00, 65, 0, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (140, 'CF202601121033266103', 43, 18.00, 12, 'completed', '', '2026-01-12 10:33:27', 1, '2026-01-12', '004', '2026-01-12 10:33:27', '2026-01-12 10:33:27', 1, 9.00, 9.00, 45, 9, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (141, 'CF202601121103293658', 52, 38.00, 22, 'cancelled', '', '2026-01-12 11:03:29', 1, '2026-01-12', '005', '2026-01-12 11:03:29', '2026-01-12 11:03:29', 1, 19.00, 22.00, 81, 22, 0, 1.0, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (142, 'CF202601121129053274', 52, 154.00, 143, 'cancelled', '', '2026-01-12 11:29:06', 1, '2026-01-12', '006', '2026-01-12 11:29:06', '2026-01-12 11:29:06', 7, 11.00, 143.00, 81, 143, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (143, 'CF202601121131156404', 52, 28.00, 28, 'cancelled', '', '2026-01-12 11:31:16', 1, '2026-01-12', '007', '2026-01-12 11:31:16', '2026-01-12 11:31:16', 1, 0.00, 28.00, NULL, 28, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (144, 'CF202601121131216532', 52, 22.00, 22, 'cancelled', '', '2026-01-12 11:31:22', 1, '2026-01-12', '008', '2026-01-12 11:31:22', '2026-01-12 11:31:22', 1, 0.00, 22.00, NULL, 22, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (145, 'CF202601121131266253', 52, 28.00, 28, 'cancelled', '', '2026-01-12 11:31:26', 1, '2026-01-12', '009', '2026-01-12 11:31:26', '2026-01-12 11:31:26', 1, 0.00, 28.00, NULL, 28, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (146, 'CF202601121134081031', 52, 36.00, 36, 'cancelled', '', '2026-01-12 11:34:09', 1, '2026-01-12', '010', '2026-01-12 11:34:09', '2026-01-12 11:34:09', 1, 0.00, 36.00, NULL, 36, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (147, 'CF202601121147204793', 52, 76.00, 60, 'cancelled', '', '2026-01-12 11:47:20', 1, '2026-01-12', '011', '2026-01-12 11:47:20', '2026-01-12 11:47:20', 3, 16.00, 60.00, 81, 60, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (148, 'CF202601121153033614', 52, 58.00, 58, 'cancelled', '', '2026-01-12 11:53:03', 1, '2026-01-12', '012', '2026-01-12 11:53:03', '2026-01-12 11:53:03', 2, 0.00, 58.00, NULL, 58, 0, 1.0, 'DINE_IN', '[82]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (149, 'CF202601121202196631', 52, 28.00, 14, 'completed', '', '2026-01-12 12:02:19', 1, '2026-01-12', '013', '2026-01-12 12:02:19', '2026-01-12 12:02:19', 1, 14.00, 14.00, 81, 14, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (150, 'CF202601121202435017', 52, 38.00, 38, 'completed', '', '2026-01-12 12:02:44', 1, '2026-01-12', '014', '2026-01-12 12:02:44', '2026-01-12 12:02:44', 1, 0.00, 38.00, NULL, 38, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (151, 'CF202601121517135927', 55, 22.00, 11, 'completed', '', '2026-01-12 15:17:13', 1, '2026-01-12', '015', '2026-01-12 15:17:13', '2026-01-12 15:17:13', 1, 11.00, 11.00, 91, 11, 1, 1.0, 'DINE_IN', '[93]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (152, 'CF202601121520575419', 55, 22.00, 22, 'completed', '', '2026-01-12 15:20:58', 1, '2026-01-12', '016', '2026-01-12 15:20:58', '2026-01-12 15:20:58', 1, 0.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (153, 'CF202601121532342910', 55, 22.00, 22, 'completed', '', '2026-01-12 15:32:35', 1, '2026-01-12', '017', '2026-01-12 15:32:35', '2026-01-12 15:32:35', 1, 0.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (154, 'CF202601121536327829', 54, 22.00, 22, 'completed', '', '2026-01-12 15:36:32', 1, '2026-01-12', '018', '2026-01-12 15:36:32', '2026-01-12 15:36:32', 1, 0.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (155, 'CF202601121545014245', 56, 28.00, 28, 'completed', '', '2026-01-12 15:45:02', 1, '2026-01-12', '019', '2026-01-12 15:45:02', '2026-01-12 15:45:02', 1, 0.00, 28.00, NULL, 28, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (156, 'CF202601121549042017', 56, 22.00, 12, 'cancelled', '', '2026-01-12 15:49:04', 1, '2026-01-12', '020', '2026-01-12 15:49:04', '2026-01-12 15:49:04', 1, 11.00, 11.00, 97, 11, 0, 1.1, 'DELIVERY', '[100,98]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (157, 'CF202601121602123290', 56, 22.00, 7, 'cancelled', '', '2026-01-12 16:02:13', 1, '2026-01-12', '021', '2026-01-12 16:02:13', '2026-01-12 16:02:13', 1, 11.00, 6.00, 96, 6, 0, 1.1, 'DELIVERY', '[99,98]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (158, 'CF202601121627492232', 56, 25.00, 10, 'cancelled', '', '2026-01-12 16:27:49', 1, '2026-01-12', '022', '2026-01-12 16:27:49', '2026-01-12 16:27:49', 1, 11.00, 9.00, 96, 9, 0, 1.1, 'DELIVERY', '[99,98]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (159, 'CF202601121642592227', 56, 25.00, 8, 'cancelled', '', '2026-01-12 16:43:00', 1, '2026-01-12', '023', '2026-01-12 16:43:00', '2026-01-12 16:43:00', 1, 12.50, 7.50, 96, 8, 0, 1.1, 'DELIVERY', '[98,99]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (160, 'CF202601121650159235', 56, 30.00, 14, 'cancelled', '', '2026-01-12 16:50:16', 1, '2026-01-12', '024', '2026-01-12 16:50:16', '2026-01-12 16:50:16', 1, 12.50, 12.50, 96, 13, 0, 1.1, 'DELIVERY', '[98,99]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (161, 'CF202601121652505820', 56, 30.00, 23, 'cancelled', '', '2026-01-12 16:52:50', 1, '2026-01-12', '025', '2026-01-12 16:52:50', '2026-01-12 16:52:50', 1, 12.50, 20.50, 96, 21, 0, 1.1, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (162, 'CF202601121658266201', 56, 52.00, 38, 'cancelled', '', '2026-01-12 16:58:26', 1, '2026-01-12', '026', '2026-01-12 16:58:26', '2026-01-12 16:58:26', 2, 12.50, 34.50, 97, 35, 0, 1.1, 'DELIVERY', '[99,98]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (163, 'CF202601121700071256', 56, 100.00, 66, 'cancelled', '', '2026-01-12 17:00:08', 1, '2026-01-12', '027', '2026-01-12 17:00:08', '2026-01-12 17:00:08', 2, 40.00, 60.00, 101, 60, 0, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (164, 'CF202601121707274894', 56, 100.00, 66, 'cancelled', '', '2026-01-12 17:07:27', 1, '2026-01-12', '028', '2026-01-12 17:07:27', '2026-01-12 17:07:27', 2, 40.00, 60.00, 101, 60, 0, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (165, 'CF202601121710089278', 56, 53.00, 34, 'cancelled', '', '2026-01-12 17:10:08', 1, '2026-01-12', '029', '2026-01-12 17:10:08', '2026-01-12 17:10:08', 2, 22.00, 31.00, 101, 31, 0, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (166, 'CF202601121713326416', 56, 22.00, 24, 'completed', '', '2026-01-12 17:13:32', 1, '2026-01-12', '030', '2026-01-12 17:13:32', '2026-01-12 17:13:32', 1, 0.00, 22.00, NULL, 22, 1, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (167, 'CF202601121722043833', 57, 30.00, 32, 'cancelled', '', '2026-01-12 17:22:04', 1, '2026-01-12', '031', '2026-01-12 17:22:04', '2026-01-12 17:22:04', 1, 3.60, 26.40, 106, 26, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (168, 'CF202601121722479848', 57, 85.00, 90, 'cancelled', '', '2026-01-12 17:22:48', 1, '2026-01-12', '032', '2026-01-12 17:22:48', '2026-01-12 17:22:48', 2, 10.20, 74.80, 105, 75, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (169, 'CF202601121737086207', 57, 22.00, 0, 'cancelled', '', '2026-01-12 17:37:08', 1, '2026-01-12', '033', '2026-01-12 17:37:08', '2026-01-12 17:37:08', 1, 22.00, 0.00, 109, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (170, 'CF202601121737317648', 57, 22.00, 0, 'cancelled', '', '2026-01-12 17:37:32', 1, '2026-01-12', '034', '2026-01-12 17:37:32', '2026-01-12 17:37:32', 1, 22.00, 0.00, 109, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (171, 'CF202601121739534547', 57, 18.00, 22, 'cancelled', '', '2026-01-12 17:39:53', 1, '2026-01-12', '035', '2026-01-12 17:39:53', '2026-01-12 17:39:53', 1, 0.00, 18.00, NULL, 18, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (172, 'CF202601121740111006', 57, 38.00, 46, 'cancelled', '', '2026-01-12 17:40:12', 1, '2026-01-12', '036', '2026-01-12 17:40:12', '2026-01-12 17:40:12', 1, 0.00, 38.00, NULL, 38, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (173, 'CF202601121740273256', 57, 40.00, 48, 'cancelled', '', '2026-01-12 17:40:27', 1, '2026-01-12', '037', '2026-01-12 17:40:27', '2026-01-12 17:40:27', 2, 0.00, 40.00, NULL, 40, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (174, 'CF202601121742137033', 57, 63.00, 76, 'cancelled', '', '2026-01-12 17:42:14', 1, '2026-01-12', '038', '2026-01-12 17:42:14', '2026-01-12 17:42:14', 2, 0.00, 63.00, NULL, 63, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (175, 'CF202601121742256233', 57, 63.00, 67, 'cancelled', '', '2026-01-12 17:42:26', 1, '2026-01-12', '039', '2026-01-12 17:42:26', '2026-01-12 17:42:26', 2, 7.56, 55.44, 105, 55, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (176, 'CF202601121742482614', 57, 46.00, 49, 'cancelled', '', '2026-01-12 17:42:49', 1, '2026-01-12', '040', '2026-01-12 17:42:49', '2026-01-12 17:42:49', 2, 5.52, 40.48, 106, 40, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (177, 'CF202601121743172638', 57, 63.00, 59, 'cancelled', '', '2026-01-12 17:43:18', 1, '2026-01-12', '041', '2026-01-12 17:43:18', '2026-01-12 17:43:18', 2, 14.00, 49.00, 103, 49, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (178, 'CF202601121743414522', 57, 66.00, 70, 'cancelled', '', '2026-01-12 17:43:42', 1, '2026-01-12', '042', '2026-01-12 17:43:42', '2026-01-12 17:43:42', 2, 7.92, 58.08, 105, 58, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (179, 'CF202601121744076929', 57, 63.00, 59, 'cancelled', '', '2026-01-12 17:44:08', 1, '2026-01-12', '043', '2026-01-12 17:44:08', '2026-01-12 17:44:08', 2, 14.00, 49.00, 103, 49, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (180, 'CF202601121749214837', 57, 67.00, 61, 'cancelled', '', '2026-01-12 17:49:22', 1, '2026-01-12', '044', '2026-01-12 17:49:22', '2026-01-12 17:49:22', 2, 16.00, 51.00, 103, 51, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (181, 'CF202601121749327077', 57, 35.00, 37, 'cancelled', '', '2026-01-12 17:49:32', 1, '2026-01-12', '045', '2026-01-12 17:49:32', '2026-01-12 17:49:32', 1, 4.20, 30.80, 105, 31, 0, 1.2, 'TAKEOUT', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (182, 'CF202601121749523054', 57, 42.00, 44, 'cancelled', '', '2026-01-12 17:49:53', 1, '2026-01-12', '046', '2026-01-12 17:49:53', '2026-01-12 17:49:53', 2, 5.04, 36.96, 106, 37, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (183, 'CF202601121750099246', 57, 42.00, 50, 'cancelled', '', '2026-01-12 17:50:09', 1, '2026-01-12', '047', '2026-01-12 17:50:09', '2026-01-12 17:50:09', 2, 0.00, 42.00, NULL, 42, 0, 1.2, 'TAKEOUT', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (184, 'CF202601121750505861', 57, 63.00, 67, 'cancelled', '', '2026-01-12 17:50:50', 1, '2026-01-12', '048', '2026-01-12 17:50:50', '2026-01-12 17:50:50', 2, 7.56, 55.44, 106, 55, 0, 1.2, 'TAKEOUT', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (185, 'CF202601121751114527', 57, 60.00, 63, 'cancelled', '', '2026-01-12 17:51:11', 1, '2026-01-12', '049', '2026-01-12 17:51:11', '2026-01-12 17:51:11', 2, 7.20, 52.80, 105, 53, 0, 1.2, 'TAKEOUT', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (186, 'CF202601121753088020', 57, 220.00, 132, 'cancelled', '', '2026-01-12 17:53:09', 1, '2026-01-12', '050', '2026-01-12 17:53:09', '2026-01-12 17:53:09', 10, 110.00, 110.00, 104, 110, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (187, 'CF202601121755333526', 57, 88.00, 79, 'completed', '', '2026-01-12 17:55:33', 1, '2026-01-12', '051', '2026-01-12 17:55:33', '2026-01-12 17:55:33', 4, 22.00, 66.00, 104, 66, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (188, 'CF202601121822322102', 57, 27.00, 7, 'cancelled', '', '2026-01-12 18:22:33', 1, '2026-01-12', '052', '2026-01-12 18:22:33', '2026-01-12 18:22:33', 1, 22.00, 5.00, 109, 5, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (189, 'CF202601121823556171', 57, 18.00, 21, 'cancelled', '', '2026-01-12 18:23:56', 1, '2026-01-12', '053', '2026-01-12 18:23:56', '2026-01-12 18:23:56', 1, 2.16, 15.84, 105, 16, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (190, 'CF202601121824497387', 57, 34.00, 16, 'cancelled', '', '2026-01-12 18:24:49', 1, '2026-01-12', '054', '2026-01-12 18:24:49', '2026-01-12 18:24:49', 2, 22.00, 12.00, 109, 12, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (191, 'CF202601121832397531', 57, 47.00, 54, 'cancelled', '', '2026-01-12 18:32:39', 1, '2026-01-12', '055', '2026-01-12 18:32:39', '2026-01-12 18:32:39', 2, 5.64, 41.36, 106, 41, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (192, 'CF202601122015532487', 59, 22.00, 0, 'cancelled', '', '2026-01-12 20:15:53', 1, '2026-01-12', '056', '2026-01-12 20:15:53', '2026-01-12 20:15:53', 1, 22.00, 0.00, 119, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (193, 'CF202601122036445953', 59, 86.00, 86, 'cancelled', '', '2026-01-12 20:36:45', 1, '2026-01-12', '057', '2026-01-12 20:36:45', '2026-01-12 20:36:45', 2, 20.00, 66.00, 127, 66, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (194, 'CF202601122044175018', 59, 86.00, 86, 'cancelled', '', '2026-01-12 20:44:18', 1, '2026-01-12', '058', '2026-01-12 20:44:18', '2026-01-12 20:44:18', 2, 20.00, 66.00, 127, 66, 0, 1.3, 'DELIVERY', '[122]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (195, 'CF202601122051122628', 59, 50.00, 13, 'cancelled', '', '2026-01-12 20:51:12', 1, '2026-01-12', '059', '2026-01-12 20:51:12', '2026-01-12 20:51:12', 1, 40.00, 10.00, 128, 10, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (196, 'CF202601122055436761', 59, 53.00, 34, 'cancelled', '', '2026-01-12 20:55:44', 1, '2026-01-12', '060', '2026-01-12 20:55:44', '2026-01-12 20:55:44', 2, 26.50, 26.50, 129, 27, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (197, 'CF202601122102139476', 59, 126.00, 139, 'cancelled', '', '2026-01-12 21:02:14', 1, '2026-01-12', '061', '2026-01-12 21:02:14', '2026-01-12 21:02:14', 4, 19.00, 107.00, 129, 107, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (198, 'CF202601122103115467', 59, 53.00, 46, 'completed', '', '2026-01-12 21:03:11', 1, '2026-01-12', '062', '2026-01-12 21:03:11', '2026-01-12 21:03:11', 2, 17.50, 35.50, 129, 36, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (199, 'CF202601122103369760', 59, 50.00, 13, 'completed', '', '2026-01-12 21:03:37', 1, '2026-01-12', '063', '2026-01-12 21:03:37', '2026-01-12 21:03:37', 1, 40.00, 10.00, 128, 10, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (200, 'CF202601122110183129', 59, 71.00, 50, 'cancelled', '', '2026-01-12 21:10:18', 1, '2026-01-12', '064', '2026-01-12 21:10:18', '2026-01-12 21:10:18', 2, 38.00, 33.00, 130, 33, 0, 1.5, 'TAKEOUT', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (201, 'CF202601122140587436', 56, 54.00, 59, 'completed', '', '2026-01-12 21:40:58', 1, '2026-01-12', '065', '2026-01-12 21:40:58', '2026-01-12 21:40:58', 2, 0.00, 49.00, NULL, 49, 1, 1.2, 'DINE_IN', '[100]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (202, 'CF202601122147266014', 60, 22.00, 26, 'completed', '', '2026-01-12 21:47:26', 1, '2026-01-12', '066', '2026-01-12 21:47:26', '2026-01-12 21:47:26', 1, 0.00, 22.00, NULL, 22, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (203, 'CF202601122151563403', 46, 22.00, 26, 'completed', '', '2026-01-12 21:51:57', 1, '2026-01-12', '067', '2026-01-12 21:51:57', '2026-01-12 21:51:57', 1, 0.00, 22.00, NULL, 22, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (204, 'CF202601122205561695', 61, 22.00, 13, 'completed', '', '2026-01-12 22:05:56', 1, '2026-01-12', '068', '2026-01-12 22:05:56', '2026-01-12 22:05:56', 1, 11.00, 11.00, 137, 11, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (205, 'CF202601122206591114', 61, 57.00, 46, 'cancelled', '', '2026-01-12 22:07:00', 1, '2026-01-12', '069', '2026-01-12 22:07:00', '2026-01-12 22:07:00', 2, 22.00, 35.00, 138, 35, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (206, 'CF202601122231587943', 62, 22.00, 13, 'completed', '', '2026-01-12 22:31:58', 1, '2026-01-12', '070', '2026-01-12 22:31:58', '2026-01-12 22:31:58', 1, 11.00, 11.00, 148, 11, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (207, 'CF202601122304044388', 62, 30.00, 43, 'completed', '', '2026-01-12 23:04:04', 1, '2026-01-12', '071', '2026-01-12 23:04:04', '2026-01-12 23:04:04', 1, 0.00, 33.00, NULL, 33, 1, 1.3, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (208, 'CF202601131353331909', 62, 22.00, 29, 'completed', '', '2026-01-13 13:53:33', 1, '2026-01-13', '001', '2026-01-13 13:53:33', '2026-01-13 13:53:33', 1, 0.00, 22.00, NULL, 22, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (209, 'CF202601131410502348', 63, 22.00, 29, 'completed', '', '2026-01-13 14:10:51', 1, '2026-01-13', '002', '2026-01-13 14:10:51', '2026-01-13 14:10:51', 1, 0.00, 22.00, NULL, 22, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (210, 'CF202601131551224770', 64, 59.00, 57, 'completed', '', '2026-01-13 15:51:22', 1, '2026-01-13', '003', '2026-01-13 15:51:22', '2026-01-13 15:51:22', 2, 15.50, 43.50, 177, 44, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (211, 'CF202601131643162970', 65, 28.00, 17, 'completed', '', '2026-01-13 16:43:16', 1, '2026-01-13', '004', '2026-01-13 16:43:16', '2026-01-13 16:43:16', 1, 14.00, 14.00, 190, 14, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (212, 'CF202601131730515117', 66, 28.00, 18, 'completed', '', '2026-01-13 17:30:51', 1, '2026-01-13', '005', '2026-01-13 17:30:51', '2026-01-13 17:30:51', 1, 14.00, 14.00, 203, 14, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (213, 'CF202601131818048261', 67, 27.00, 29, 'completed', '', '2026-01-13 18:18:05', 1, '2026-01-13', '006', '2026-01-13 18:18:05', '2026-01-13 18:18:05', 1, 0.00, 22.00, NULL, 22, 1, 1.3, 'DINE_IN', '[208]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (214, 'CF202601131819391902', 67, 50.00, 75, 'cancelled', '', '2026-01-13 18:19:39', 1, '2026-01-13', '007', '2026-01-13 18:19:39', '2026-01-13 18:19:39', 1, 0.00, 50.00, NULL, 50, 0, 1.5, 'DELIVERY', NULL, 3.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (215, 'CF202601131833435831', 68, 30.00, 47, 'completed', '', '2026-01-13 18:33:44', 1, '2026-01-13', '008', '2026-01-13 18:33:44', '2026-01-13 18:33:44', 1, 0.00, 30.00, NULL, 30, 1, 1.6, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (216, 'CF202601131840143954', 68, 35.00, 0, 'cancelled', '', '2026-01-13 18:40:14', 1, '2026-01-13', '009', '2026-01-13 18:40:14', '2026-01-13 18:40:14', 1, 35.00, 0.00, 215, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (217, 'CF202601131957312715', 68, 38.00, 0, 'completed', '', '2026-01-13 19:57:32', 1, '2026-01-13', '010', '2026-01-13 19:57:32', '2026-01-13 19:57:32', 1, 38.00, 0.00, 215, 0, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (218, 'CF202601132021396324', 68, 50.00, 17, 'cancelled', '', '2026-01-13 20:21:39', 1, '2026-01-13', '011', '2026-01-13 20:21:39', '2026-01-13 20:21:39', 1, 40.00, 10.00, 213, 10, 0, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (219, 'CF202601132029095864', 68, 50.00, 0, 'cancelled', '', '2026-01-13 20:29:10', 1, '2026-01-13', '012', '2026-01-13 20:29:10', '2026-01-13 20:29:10', 1, 50.00, 0.00, 213, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (220, 'CF202601132029271677', 68, 30.00, 9, 'cancelled', '', '2026-01-13 20:29:28', 1, '2026-01-13', '013', '2026-01-13 20:29:28', '2026-01-13 20:29:28', 1, 25.00, 5.00, 214, 5, 0, 1.8, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (221, 'CF202601132029439089', 68, 38.00, 0, 'cancelled', '', '2026-01-13 20:29:44', 1, '2026-01-13', '014', '2026-01-13 20:29:44', '2026-01-13 20:29:44', 1, 38.00, 0.00, 214, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (222, 'CF202601132031079624', 68, 30.00, 9, 'cancelled', '', '2026-01-13 20:31:08', 1, '2026-01-13', '015', '2026-01-13 20:31:08', '2026-01-13 20:31:08', 1, 25.00, 5.00, 213, 5, 0, 1.8, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (223, 'CF202601132052052722', 68, 30.00, 0, 'cancelled', '', '2026-01-13 20:52:05', 1, '2026-01-13', '016', '2026-01-13 20:52:05', '2026-01-13 20:52:05', 1, 30.00, 0.00, 213, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (224, 'CF202601132052179901', 68, 30.00, 9, 'cancelled', '', '2026-01-13 20:52:17', 1, '2026-01-13', '017', '2026-01-13 20:52:17', '2026-01-13 20:52:17', 1, 25.00, 5.00, 214, 5, 0, 1.8, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (225, 'CF202601132113016220', 68, 30.00, 0, 'cancelled', '', '2026-01-13 21:13:02', 1, '2026-01-13', '018', '2026-01-13 21:13:02', '2026-01-13 21:13:02', 1, 30.00, 0.00, 213, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (226, 'CF202601132113402801', 68, 30.00, 9, 'cancelled', '', '2026-01-13 21:13:41', 1, '2026-01-13', '019', '2026-01-13 21:13:41', '2026-01-13 21:13:41', 1, 25.00, 5.00, 214, 5, 0, 1.8, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (227, 'CF202601132123226941', 68, 30.00, 30, 'cancelled', '', '2026-01-13 21:23:23', 1, '2026-01-13', '020', '2026-01-13 21:23:23', '2026-01-13 21:23:23', 1, 12.50, 17.50, 212, 18, 0, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (228, 'CF202601132139564631', 68, 50.00, 85, 'cancelled', '', '2026-01-13 21:39:57', 1, '2026-01-13', '021', '2026-01-13 21:39:57', '2026-01-13 21:39:57', 1, 0.00, 50.00, NULL, 50, 0, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (229, 'CF202601141913063014', 68, 30.00, 51, 'cancelled', '', '2026-01-14 19:13:07', 1, '2026-01-14', '001', '2026-01-14 19:13:07', '2026-01-14 19:13:07', 1, 0.00, 30.00, NULL, 30, 0, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (230, 'CF202601151737464549', 69, 27.00, 11, 'cancelled', '', '2026-01-15 17:37:47', 1, '2026-01-15', '001', '2026-01-15 17:37:47', '2026-01-15 17:37:47', 1, 16.00, 11.00, 232, 11, 0, 1.0, 'DINE_IN', '[233]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (231, 'CF202601151828551503', 69, 58.00, 47, 'cancelled', '', '2026-01-15 18:28:56', 1, '2026-01-15', '002', '2026-01-15 18:28:56', '2026-01-15 18:28:56', 2, 10.60, 47.40, 243, 47, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (232, 'CF202601151857146386', 69, 35.00, 5, 'cancelled', '', '2026-01-15 18:57:14', 1, '2026-01-15', '003', '2026-01-15 18:57:14', '2026-01-15 18:57:14', 1, 30.00, 5.00, 238, 5, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (233, 'CF202601151929435352', 69, 37.00, 9, 'cancelled', '', '2026-01-15 19:29:44', 1, '2026-01-15', '004', '2026-01-15 19:29:44', '2026-01-15 19:29:44', 1, 28.00, 9.00, 253, 9, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (234, 'CF202601151938066306', 53, 34.00, 23, 'completed', '', '2026-01-15 19:38:06', 1, '2026-01-15', '005', '2026-01-15 19:38:06', '2026-01-15 19:38:06', 2, 11.00, 23.00, 85, 23, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (235, 'CF202601151940392291', 69, 38.00, 28, 'cancelled', '', '2026-01-15 19:40:39', 1, '2026-01-15', '006', '2026-01-15 19:40:39', '2026-01-15 19:40:39', 1, 10.00, 28.00, 255, 28, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (236, 'CF202601151959341014', 70, 27.00, 22, 'completed', '', '2026-01-15 19:59:35', 1, '2026-01-15', '007', '2026-01-15 19:59:35', '2026-01-15 19:59:35', 1, 5.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', '[257]', 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (237, 'CF202601152007559255', 71, 100.00, 66, 'completed', '', '2026-01-15 20:07:56', 1, '2026-01-15', '008', '2026-01-15 20:07:56', '2026-01-15 20:07:56', 2, 40.00, 60.00, 264, 60, 1, 1.1, 'DELIVERY', '[261]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (238, 'CF202601152013105704', 71, 63.00, 42, 'cancelled', '', '2026-01-15 20:13:11', 1, '2026-01-15', '009', '2026-01-15 20:13:11', '2026-01-15 20:13:11', 2, 28.00, 35.00, 265, 35, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (239, 'CF202601152013481630', 71, 68.00, 48, 'cancelled', '', '2026-01-15 20:13:48', 1, '2026-01-15', '010', '2026-01-15 20:13:48', '2026-01-15 20:13:48', 2, 28.00, 40.00, 265, 40, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (240, 'CF202601152014423003', 71, 63.00, 42, 'cancelled', '', '2026-01-15 20:14:42', 1, '2026-01-15', '011', '2026-01-15 20:14:42', '2026-01-15 20:14:42', 2, 28.00, 35.00, 265, 35, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (241, 'CF202601152037032924', 71, 75.00, 53, 'cancelled', '', '2026-01-15 20:37:03', 1, '2026-01-15', '012', '2026-01-15 20:37:03', '2026-01-15 20:37:03', 2, 31.00, 44.00, 265, 44, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (242, 'CF202601152041398485', 72, 32.00, 34, 'cancelled', '', '2026-01-15 20:41:39', 1, '2026-01-15', '013', '2026-01-15 20:41:39', '2026-01-15 20:41:39', 1, 3.36, 28.64, 269, 29, 0, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (243, 'CF202601152050409050', 72, 22.00, 26, 'completed', '', '2026-01-15 20:50:41', 1, '2026-01-15', '014', '2026-01-15 20:50:41', '2026-01-15 20:50:41', 1, 0.00, 22.00, NULL, 22, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (244, 'CF202601152111471458', 73, 22.00, 13, 'completed', '', '2026-01-15 21:11:47', 1, '2026-01-15', '015', '2026-01-15 21:11:47', '2026-01-15 21:11:47', 1, 11.00, 11.00, 274, 11, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (245, 'CF202601152113522436', 73, 37.00, 12, 'cancelled', '', '2026-01-15 21:13:53', 1, '2026-01-15', '016', '2026-01-15 21:13:53', '2026-01-15 21:13:53', 1, 28.00, 9.00, 275, 9, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (246, 'CF202601152122591441', 73, 37.00, 12, 'cancelled', '', '2026-01-15 21:23:00', 1, '2026-01-15', '017', '2026-01-15 21:23:00', '2026-01-15 21:23:00', 1, 28.00, 9.00, 275, 9, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (247, 'CF202601152124442277', 73, 37.00, 12, 'cancelled', '', '2026-01-15 21:24:45', 1, '2026-01-15', '018', '2026-01-15 21:24:45', '2026-01-15 21:24:45', 1, 28.00, 9.00, 276, 9, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (248, 'CF202601152235109515', 73, 37.00, 12, 'cancelled', '', '2026-01-15 22:35:10', 1, '2026-01-15', '019', '2026-01-15 22:35:10', '2026-01-15 22:35:10', 1, 28.00, 9.00, 276, 9, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (249, 'CF202601152236282826', 73, 80.00, 64, 'cancelled', '', '2026-01-15 22:36:28', 1, '2026-01-15', '020', '2026-01-15 22:36:28', '2026-01-15 22:36:28', 2, 31.00, 49.00, 277, 49, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (250, 'CF202601152236431657', 73, 100.00, 78, 'cancelled', '', '2026-01-15 22:36:44', 1, '2026-01-15', '021', '2026-01-15 22:36:44', '2026-01-15 22:36:44', 2, 40.00, 60.00, 277, 60, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (251, 'CF202601152241499340', 73, 37.00, 12, 'cancelled', '', '2026-01-15 22:41:50', 1, '2026-01-15', '022', '2026-01-15 22:41:50', '2026-01-15 22:41:50', 1, 28.00, 9.00, 285, 9, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (252, 'CF202601152242377500', 73, 70.00, 68, 'cancelled', '', '2026-01-15 22:42:37', 1, '2026-01-15', '023', '2026-01-15 22:42:37', '2026-01-15 22:42:37', 2, 17.50, 52.50, 286, 53, 0, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (253, 'CF202601152244043816', 73, 44.00, 37, 'completed', '', '2026-01-15 22:44:04', 1, '2026-01-15', '024', '2026-01-15 22:44:04', '2026-01-15 22:44:04', 2, 22.00, 22.00, 276, 22, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (254, 'CF202601152244587669', 73, 40.00, 7, 'cancelled', '', '2026-01-15 22:44:59', 1, '2026-01-15', '025', '2026-01-15 22:44:59', '2026-01-15 22:44:59', 1, 36.00, 4.00, 287, 4, 0, 1.8, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (255, 'CF202601152250164813', 74, 40.00, 15, 'completed', '', '2026-01-15 22:50:16', 1, '2026-01-15', '026', '2026-01-15 22:50:16', '2026-01-15 22:50:16', 1, 31.00, 9.00, 289, 9, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (256, 'CF202601152250422950', 74, 77.00, 83, 'completed', '', '2026-01-15 22:50:43', 1, '2026-01-15', '027', '2026-01-15 22:50:43', '2026-01-15 22:50:43', 2, 28.00, 49.00, 295, 49, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (257, 'CF202601152251444742', 74, 28.00, 48, 'completed', '', '2026-01-15 22:51:44', 1, '2026-01-15', '028', '2026-01-15 22:51:44', '2026-01-15 22:51:44', 1, 0.00, 28.00, NULL, 28, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (258, 'CF202601152251562038', 74, 30.00, 0, 'completed', '', '2026-01-15 22:51:56', 1, '2026-01-15', '029', '2026-01-15 22:51:56', '2026-01-15 22:51:56', 1, 33.00, 0.00, 290, 0, 1, 1.0, 'DELIVERY', NULL, 3.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (259, 'CF202601152253101214', 74, 28.00, 24, 'completed', '', '2026-01-15 22:53:10', 1, '2026-01-15', '030', '2026-01-15 22:53:10', '2026-01-15 22:53:10', 1, 14.00, 14.00, 288, 14, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (260, 'CF202601152253358715', 74, 28.00, 48, 'completed', '', '2026-01-15 22:53:36', 1, '2026-01-15', '031', '2026-01-15 22:53:36', '2026-01-15 22:53:36', 1, 3.00, 28.00, NULL, 28, 1, 1.7, 'DELIVERY', NULL, 3.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (261, 'CF202601152303277340', 70, 32.00, 23, 'completed', '', '2026-01-15 23:03:27', 1, '2026-01-15', '032', '2026-01-15 23:03:27', '2026-01-15 23:03:27', 1, 14.00, 21.00, 256, 21, 1, 1.1, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (262, 'CF202601152303344037', 70, 28.00, 19, 'completed', '', '2026-01-15 23:03:35', 1, '2026-01-15', '033', '2026-01-15 23:03:35', '2026-01-15 23:03:35', 1, 14.00, 17.00, 258, 17, 1, 1.1, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (263, 'CF202601152303432464', 70, 30.00, 20, 'completed', '', '2026-01-15 23:03:43', 1, '2026-01-15', '034', '2026-01-15 23:03:43', '2026-01-15 23:03:43', 1, 15.00, 18.00, 259, 18, 1, 1.1, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (264, 'CF202601152305574912', 70, 38.00, 42, 'completed', '', '2026-01-15 23:05:57', 1, '2026-01-15', '035', '2026-01-15 23:05:57', '2026-01-15 23:05:57', 1, 0.00, 38.00, NULL, 38, 1, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (265, 'CF202601152306055817', 70, 36.00, 40, 'completed', '', '2026-01-15 23:06:06', 1, '2026-01-15', '036', '2026-01-15 23:06:06', '2026-01-15 23:06:06', 1, 0.00, 36.00, NULL, 36, 1, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (266, 'CF202601152306112123', 70, 36.00, 40, 'completed', '', '2026-01-15 23:06:11', 1, '2026-01-15', '037', '2026-01-15 23:06:11', '2026-01-15 23:06:11', 1, 0.00, 36.00, NULL, 36, 1, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (267, 'CF202601152314433727', 75, 40.00, 15, 'completed', '', '2026-01-15 23:14:43', 1, '2026-01-15', '038', '2026-01-15 23:14:43', '2026-01-15 23:14:43', 1, 34.00, 9.00, 298, 9, 1, 1.7, 'DELIVERY', NULL, 3.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (268, 'CF202601152314511144', 75, 32.00, 27, 'completed', '', '2026-01-15 23:14:52', 1, '2026-01-15', '039', '2026-01-15 23:14:52', '2026-01-15 23:14:52', 1, 19.00, 16.00, 297, 16, 1, 1.7, 'DELIVERY', NULL, 3.00, 1, 'BLACK_GOLD_UNLIMITED');
INSERT INTO `shop_orders` VALUES (269, 'CF202601152315047083', 75, 36.00, 61, 'completed', '', '2026-01-15 23:15:04', 1, '2026-01-15', '040', '2026-01-15 23:15:04', '2026-01-15 23:15:04', 1, 0.00, 36.00, NULL, 36, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (270, 'CF202601152315128170', 75, 36.00, 61, 'completed', '', '2026-01-15 23:15:12', 1, '2026-01-15', '041', '2026-01-15 23:15:12', '2026-01-15 23:15:12', 1, 0.00, 36.00, NULL, 36, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (271, 'CF202601152316254014', 75, 36.00, 61, 'completed', '', '2026-01-15 23:16:25', 1, '2026-01-15', '042', '2026-01-15 23:16:25', '2026-01-15 23:16:25', 1, 0.00, 36.00, NULL, 36, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (272, 'CF202601152321194640', 76, 38.00, 38, 'completed', '', '2026-01-15 23:21:19', 1, '2026-01-15', '043', '2026-01-15 23:21:19', '2026-01-15 23:21:19', 1, 0.00, 38.00, NULL, 38, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (273, 'CF202601152322484901', 76, 36.00, 39, 'completed', '', '2026-01-15 23:22:48', 1, '2026-01-15', '044', '2026-01-15 23:22:48', '2026-01-15 23:22:48', 1, 0.00, 39.00, NULL, 39, 1, 1.0, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (274, 'CF202601152323076666', 76, 36.00, 36, 'completed', '', '2026-01-15 23:23:07', 1, '2026-01-15', '045', '2026-01-15 23:23:07', '2026-01-15 23:23:07', 1, 0.00, 36.00, NULL, 36, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (275, 'CF202601152324056987', 76, 28.00, 31, 'completed', '', '2026-01-15 23:24:05', 1, '2026-01-15', '046', '2026-01-15 23:24:05', '2026-01-15 23:24:05', 1, 0.00, 31.00, NULL, 31, 1, 1.0, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (276, 'CF202601152329128615', 46, 22.00, 0, 'completed', '', '2026-01-15 23:29:12', 1, '2026-01-15', '047', '2026-01-15 23:29:12', '2026-01-15 23:29:12', 1, 22.00, 0.00, 135, 0, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (277, 'CF202601152329327572', 46, 38.00, 49, 'completed', '', '2026-01-15 23:29:32', 1, '2026-01-15', '048', '2026-01-15 23:29:32', '2026-01-15 23:29:32', 1, 0.00, 38.00, NULL, 38, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (278, 'CF202601152329396575', 46, 36.00, 47, 'completed', '', '2026-01-15 23:29:39', 1, '2026-01-15', '049', '2026-01-15 23:29:39', '2026-01-15 23:29:39', 1, 0.00, 36.00, NULL, 36, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (279, 'CF202601152330437758', 46, 36.00, 47, 'completed', '', '2026-01-15 23:30:44', 1, '2026-01-15', '050', '2026-01-15 23:30:44', '2026-01-15 23:30:44', 1, 0.00, 36.00, NULL, 36, 1, 1.3, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (280, 'CF202601152335309396', 47, 36.00, 38, 'completed', '', '2026-01-15 23:35:30', 1, '2026-01-15', '051', '2026-01-15 23:35:30', '2026-01-15 23:35:30', 1, 4.32, 31.68, 74, 32, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (281, 'CF202601152335382875', 47, 38.00, 40, 'completed', '', '2026-01-15 23:35:38', 1, '2026-01-15', '052', '2026-01-15 23:35:38', '2026-01-15 23:35:38', 1, 4.56, 33.44, 75, 33, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (282, 'CF202601152335492623', 47, 28.00, 34, 'completed', '', '2026-01-15 23:35:49', 1, '2026-01-15', '053', '2026-01-15 23:35:49', '2026-01-15 23:35:49', 1, 0.00, 28.00, NULL, 28, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (283, 'CF202601152336112505', 47, 22.00, 26, 'completed', '', '2026-01-15 23:36:12', 1, '2026-01-15', '054', '2026-01-15 23:36:12', '2026-01-15 23:36:12', 1, 0.00, 22.00, NULL, 22, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (284, 'CF202601152336319009', 47, 38.00, 46, 'completed', '', '2026-01-15 23:36:32', 1, '2026-01-15', '055', '2026-01-15 23:36:32', '2026-01-15 23:36:32', 1, 0.00, 38.00, NULL, 38, 1, 1.2, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (285, 'CF202601152337201212', 47, 28.00, 37, 'completed', '', '2026-01-15 23:37:21', 1, '2026-01-15', '056', '2026-01-15 23:37:21', '2026-01-15 23:37:21', 1, 0.00, 31.00, NULL, 31, 1, 1.2, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (286, 'CF202601152337349419', 47, 28.00, 37, 'completed', '', '2026-01-15 23:37:34', 1, '2026-01-15', '057', '2026-01-15 23:37:34', '2026-01-15 23:37:34', 1, 0.00, 31.00, NULL, 31, 1, 1.2, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (287, 'CF202601152341557295', 49, 28.00, 19, 'completed', '', '2026-01-15 23:41:55', 1, '2026-01-15', '058', '2026-01-15 23:41:55', '2026-01-15 23:41:55', 1, 14.00, 17.00, 52, 17, 1, 1.1, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (288, 'CF202601152342152497', 49, 38.00, 42, 'completed', '', '2026-01-15 23:42:16', 1, '2026-01-15', '059', '2026-01-15 23:42:16', '2026-01-15 23:42:16', 1, 0.00, 38.00, NULL, 38, 1, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (289, 'CF202601152342227644', 49, 36.00, 40, 'completed', '', '2026-01-15 23:42:23', 1, '2026-01-15', '060', '2026-01-15 23:42:23', '2026-01-15 23:42:23', 1, 0.00, 36.00, NULL, 36, 1, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (290, 'CF202601152342557176', 49, 36.00, 40, 'completed', '', '2026-01-15 23:42:55', 1, '2026-01-15', '061', '2026-01-15 23:42:55', '2026-01-15 23:42:55', 1, 0.00, 36.00, NULL, 36, 1, 1.1, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (291, 'CF202601160815433698', 47, 30.00, 51, 'completed', '', '2026-01-16 08:15:43', 1, '2026-01-16', '001', '2026-01-16 08:15:43', '2026-01-16 08:15:43', 1, 0.00, 30.00, NULL, 30, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (292, 'CF202601160816598434', 47, 28.00, 48, 'completed', '', '2026-01-16 08:16:59', 1, '2026-01-16', '002', '2026-01-16 08:16:59', '2026-01-16 08:16:59', 1, 0.00, 28.00, NULL, 28, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (293, 'CF202601160817123508', 47, 50.00, 85, 'completed', '', '2026-01-16 08:17:12', 1, '2026-01-16', '003', '2026-01-16 08:17:12', '2026-01-16 08:17:12', 1, 0.00, 50.00, NULL, 50, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (294, 'CF202601160905058232', 78, 22.00, 18, 'completed', '', '2026-01-16 09:05:05', 1, '2026-01-16', '004', '2026-01-16 09:05:05', '2026-01-16 09:05:05', 1, 11.00, 11.00, 307, 11, 1, 1.6, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (295, 'CF202601160921105665', 79, 40.00, 33, 'cancelled', '', '2026-01-16 09:21:11', 1, '2026-01-16', '005', '2026-01-16 09:21:11', '2026-01-16 09:21:11', 1, 20.50, 19.50, 308, 20, 0, 1.7, 'DELIVERY', '[313,312]', 3.00, 1, 'COUPON');
INSERT INTO `shop_orders` VALUES (296, 'CF202601192245114213', 79, 22.00, 37, 'completed', '', '2026-01-19 22:45:12', 1, '2026-01-19', '001', '2026-01-19 22:45:12', '2026-01-19 22:45:12', 1, 0.00, 22.00, NULL, 22, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (297, 'CF202601192245181812', 79, 36.00, 61, 'completed', '', '2026-01-19 22:45:18', 1, '2026-01-19', '002', '2026-01-19 22:45:18', '2026-01-19 22:45:18', 1, 0.00, 36.00, NULL, 36, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (298, 'CF202601192247474099', 79, 22.00, 0, 'cancelled', '', '2026-01-19 22:47:48', 1, '2026-01-19', '003', '2026-01-19 22:47:48', '2026-01-19 22:47:48', 1, 22.00, 0.00, 314, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (299, 'CF202601192247574105', 79, 22.00, 37, 'completed', '', '2026-01-19 22:47:57', 1, '2026-01-19', '004', '2026-01-19 22:47:57', '2026-01-19 22:47:57', 1, 0.00, 22.00, NULL, 22, 1, 1.7, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (300, 'CF202601192338102945', 79, 22.00, 0, 'cancelled', '', '2026-01-19 23:38:11', 1, '2026-01-19', '005', '2026-01-19 23:38:11', '2026-01-19 23:38:11', 1, 22.00, 0.00, 315, 0, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (301, 'CF202607071420349318', 19, 28.00, 28, 'cancelled', '', '2026-07-07 14:20:35', 1, '2026-07-07', '001', '2026-07-07 14:20:35', '2026-07-07 14:20:35', 1, 0.00, 28.00, NULL, 28, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (302, 'CF202607071425227349', 19, 32.00, 32, 'cancelled', '', '2026-07-07 14:25:23', 1, '2026-07-07', '002', '2026-07-07 14:25:23', '2026-07-07 14:25:23', 1, 0.00, 32.00, NULL, 32, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (303, 'CF202607071431168114', 19, 22.00, 22, 'completed', '', '2026-07-07 14:31:16', 1, '2026-07-07', '003', '2026-07-07 14:31:16', '2026-07-07 14:31:16', 1, 0.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (304, 'CF202607071435065714', 19, 32.00, 32, 'completed', '', '2026-07-07 14:35:07', 1, '2026-07-07', '004', '2026-07-07 14:35:07', '2026-07-07 14:35:07', 1, 0.00, 32.00, NULL, 32, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (305, 'CF202607081328015064', 19, 22.00, 22, 'completed', '', '2026-07-08 13:28:02', 1, '2026-07-08', '001', '2026-07-08 13:28:02', '2026-07-08 13:28:02', 1, 0.00, 22.00, NULL, 22, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (306, 'CF202607081339358960', 19, 30.00, 30, 'completed', '', '2026-07-08 13:39:36', 1, '2026-07-08', '002', '2026-07-08 13:39:36', '2026-07-08 13:39:36', 1, 0.00, 30.00, NULL, 30, 1, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (307, 'CF202607081344419793', 19, 28.00, 31, 'completed', '', '2026-07-08 13:44:41', 1, '2026-07-08', '003', '2026-07-08 13:44:41', '2026-07-08 13:44:41', 1, 0.00, 31.00, NULL, 31, 1, 1.0, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (308, 'CF202607081346552288', 19, 22.00, 25, 'completed', '', '2026-07-08 13:46:55', 1, '2026-07-08', '004', '2026-07-08 13:46:55', '2026-07-08 13:46:55', 1, 0.00, 25.00, NULL, 25, 1, 1.0, 'DELIVERY', NULL, 3.00, 0, NULL);
INSERT INTO `shop_orders` VALUES (309, 'CF202607081428048997', 19, 30.00, 15, 'cancelled', '', '2026-07-08 14:28:05', 1, '2026-07-08', '005', '2026-07-08 14:28:05', '2026-07-08 14:28:05', 1, 15.00, 15.00, 323, 15, 0, 1.0, 'DINE_IN', NULL, 0.00, 0, NULL);

-- ----------------------------
-- View structure for v_shop_orders_full
-- ----------------------------
DROP VIEW IF EXISTS `v_shop_orders_full`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_shop_orders_full` AS select `shop_orders`.`id` AS `id`,`shop_orders`.`order_no` AS `order_no`,`shop_orders`.`user_id` AS `user_id`,`shop_orders`.`total_amount` AS `total_amount`,`shop_orders`.`points_earned` AS `points_earned`,`shop_orders`.`status` AS `status`,`shop_orders`.`remark` AS `remark`,`shop_orders`.`created_at` AS `created_at`,`shop_orders`.`store_id` AS `store_id`,`shop_orders`.`business_date` AS `business_date`,`shop_orders`.`pickup_code` AS `pickup_code`,`shop_orders`.`pickup_code_generated_at` AS `pickup_code_generated_at`,`shop_orders`.`updated_at` AS `updated_at`,`shop_orders`.`total_quantity` AS `total_quantity`,`shop_orders`.`discount_amount` AS `discount_amount`,`shop_orders`.`pay_amount` AS `pay_amount`,`shop_orders`.`applied_coupon_id` AS `applied_coupon_id`,`shop_orders`.`exp_earned` AS `exp_earned`,`shop_orders`.`rewards_granted` AS `rewards_granted`,`shop_orders`.`points_multiplier` AS `points_multiplier`,`shop_orders`.`dining_method` AS `dining_method`,`shop_orders`.`applied_addon_coupon_ids` AS `applied_addon_coupon_ids`,`shop_orders`.`delivery_fee` AS `delivery_fee`,`shop_orders`.`delivery_fee_waived` AS `delivery_fee_waived`,`shop_orders`.`delivery_fee_waived_reason` AS `delivery_fee_waived_reason` from `shop_orders`;

-- ----------------------------
-- View structure for v_shop_orders_hot
-- ----------------------------
DROP VIEW IF EXISTS `v_shop_orders_hot`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_shop_orders_hot` AS select `shop_orders`.`id` AS `id`,`shop_orders`.`order_no` AS `order_no`,`shop_orders`.`user_id` AS `user_id`,`shop_orders`.`status` AS `status`,`shop_orders`.`total_amount` AS `total_amount`,`shop_orders`.`pay_amount` AS `pay_amount`,`shop_orders`.`discount_amount` AS `discount_amount`,`shop_orders`.`points_earned` AS `points_earned`,`shop_orders`.`exp_earned` AS `exp_earned`,`shop_orders`.`total_quantity` AS `total_quantity`,`shop_orders`.`dining_method` AS `dining_method`,`shop_orders`.`store_id` AS `store_id`,`shop_orders`.`created_at` AS `created_at` from `shop_orders`;

SET FOREIGN_KEY_CHECKS = 1;
