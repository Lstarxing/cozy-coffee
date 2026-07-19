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
