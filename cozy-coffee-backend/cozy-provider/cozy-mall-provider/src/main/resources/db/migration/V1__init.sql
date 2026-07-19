/*
 Navicat Premium Data Transfer

 Source Server         : cozycoffee开发
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : cozy_mall

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 10/07/2026 10:15:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for monthly_redemptions
-- ----------------------------
DROP TABLE IF EXISTS `monthly_redemptions`;
CREATE TABLE `monthly_redemptions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '兑换月份(YYYY-MM)',
  `redeemed_count` int NOT NULL DEFAULT 0 COMMENT '本月已兑换次数',
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_product_month`(`user_id` ASC, `product_id` ASC, `month` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '月限兑计数表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for points_order_fulfillments
-- ----------------------------
DROP TABLE IF EXISTS `points_order_fulfillments`;
CREATE TABLE `points_order_fulfillments`  (
  `order_id` bigint NOT NULL COMMENT '订单ID（PK，1:1关联）',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交付类型: VIRTUAL/PICKUP/DELIVERY',
  `address_id` bigint NULL DEFAULT NULL COMMENT '地址簿ID（可选追溯）',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `receiver_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `shipping_company` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '物流公司',
  `tracking_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '物流单号（仅配送类型生效，关联物流公司查询物流轨迹）',
  `shipped_at` datetime NULL DEFAULT NULL COMMENT '发货时间（仅配送类型生效，记录商家发货的时间戳）',
  `store_id` bigint NULL DEFAULT 1 COMMENT '自提门店ID（默认1）',
  `pickup_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '自提码',
  `business_date` date NULL DEFAULT NULL COMMENT '业务日期（记录订单交付的核心日期，如自提日期 / 核销日期）',
  `virtual_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '兑换码/券码',
  `issued_at` datetime NULL DEFAULT NULL COMMENT '发放时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '兑换订单交付信息表（1:1）' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for points_orders
-- ----------------------------
DROP TABLE IF EXISTS `points_orders`;
CREATE TABLE `points_orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称（冗余）',
  `product_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片（冗余）',
  `points_cost` int NOT NULL COMMENT '消耗积分',
  `quantity` int NULL DEFAULT 1 COMMENT '兑换数量',
  `status` enum('pending','processing','shipped','completed','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'pending' COMMENT '订单状态：pending待处理/processing处理中/shipped已发货/completed已完成/cancelled已取消',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `product_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PHYSICAL' COMMENT '商品类型: VIRTUAL/PHYSICAL',
  `fulfillment_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PICKUP' COMMENT 'VIRTUAL/PICKUP/DELIVERY',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `business_date` date NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `product_id`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分兑换订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------

-- ----------------------------
-- Table structure for points_products
-- ----------------------------
DROP TABLE IF EXISTS `points_products`;
CREATE TABLE `points_products`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品描述',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片URL',
  `points_price` int NOT NULL COMMENT '兑换所需积分',
  `original_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '商品原价',
  `stock` int NULL DEFAULT 0 COMMENT '库存数量',
  `status` enum('active','inactive','sold_out') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'active' COMMENT '商品状态',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品分类：drink/merchandise/coupon',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `product_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PHYSICAL' COMMENT '商品类型: VIRTUAL/PHYSICAL',
  `coupon_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '券类型',
  `coupon_value` int NULL DEFAULT NULL COMMENT '折扣率或满减额度',
  `face_value` int NULL DEFAULT NULL COMMENT '兑换券抵扣面值',
  `min_order_amount` int NULL DEFAULT NULL COMMENT '满减券门槛金额',
  `linked_product_id` bigint NULL DEFAULT NULL COMMENT '兑换券关联的咖啡商品ID',
  `monthly_limit` int NULL DEFAULT NULL COMMENT '月度兑换限制(NULL=不限,适用于所有商品类型)',
  `valid_days` int NOT NULL DEFAULT 7 COMMENT '券有效天数(兑换后)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_points_price`(`points_price` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分商品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of points_products
-- ----------------------------
INSERT INTO `points_products` VALUES (17, '加浓缩券', '下单时可额外添加1份浓缩，可与主券叠加使用', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E6%B5%93%E7%BC%A9%E5%88%B8.png', 80, NULL, 91, 'active', 'coupon', '2026-01-04 11:54:32', '2026-07-08 17:48:26', 'PHYSICAL', 'SHOT', 1, NULL, NULL, NULL, NULL, 30);
INSERT INTO `points_products` VALUES (18, '配送费抵扣券', '仅抵扣外卖订单配送费，可与主券叠加使用,最高可抵扣配送费3元', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E9%85%8D%E9%80%81%E8%B4%B9%E6%8A%B5%E6%89%A3%E5%88%B8.png', 120, NULL, 92, 'active', 'coupon', '2026-01-04 12:03:48', '2026-07-08 17:48:26', 'PHYSICAL', 'DELIVERY_FEE', 3, NULL, NULL, NULL, 10, 30);
INSERT INTO `points_products` VALUES (19, '5元代金券', '', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/5%E5%85%83%E4%BB%A3%E9%87%91%E5%88%B8.png', 150, NULL, 97, 'active', 'coupon', '2026-01-04 12:06:36', '2026-07-08 17:48:26', 'PHYSICAL', 'FULL_REDUCE', 5, NULL, 0, NULL, 2, 30);
INSERT INTO `points_products` VALUES (20, '全场饮品通兑券', '通兑券将自动抵扣订单中价格最高的饮品(最高40元)', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E4%BB%BB%E9%80%89%E9%A5%AE%E5%93%81%E9%80%9A%E5%85%91%E5%88%B8.png', 800, NULL, 95, 'active', 'coupon', '2026-01-04 12:30:24', '2026-07-08 17:48:26', 'PHYSICAL', 'EXCHANGE', NULL, NULL, NULL, NULL, 10, 14);
INSERT INTO `points_products` VALUES (21, 'Cozy 品牌贴纸包', '精选多款 Cozy 主题原创插画贴纸。采用防水材质，无论是贴在笔记本电脑、手机壳还是手账本上，都能随时随地随身携带一份“惬意”，让你的私人物品瞬间变身。', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/Cozy%20%E5%93%81%E7%89%8C%E8%B4%B4%E7%BA%B8%E5%8C%85.png', 100, NULL, 100, 'active', 'gift', '2026-01-04 16:46:30', '2026-07-08 17:48:26', 'PHYSICAL', NULL, NULL, NULL, NULL, NULL, NULL, 7);
INSERT INTO `points_products` VALUES (22, '定制拿铁金搅拌勺', '专为 CozyCoffee 定制的优雅“拿铁金”色泽不锈钢搅拌勺。勺柄末端精细刻印品牌 Logo，手感沉稳。每一次搅动杯中咖啡，不仅是融合风味，更充满了生活的仪式感。', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E5%AE%9A%E5%88%B6%E6%8B%BF%E9%93%81%E9%87%91%E6%90%85%E6%8B%8C%E5%8B%BA.png', 600, NULL, 98, 'active', 'gift', '2026-01-04 16:48:32', '2026-07-08 17:48:26', 'PHYSICAL', NULL, NULL, NULL, NULL, NULL, 1, 7);
INSERT INTO `points_products` VALUES (23, '拿铁金定制马克杯', '积分商城人气 No.1 单品。采用独特的哑光拿铁金釉面工艺，触感温润。人体工学手柄设计，握感舒适。它不仅是一个咖啡杯，更是你办公桌上最吸睛的“Cozy 陪伴”。', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E6%8B%BF%E9%93%81%E9%87%91%E5%AE%9A%E5%88%B6%E9%A9%AC%E5%85%8B%E6%9D%AF.png', 1200, NULL, 100, 'active', 'gift', '2026-01-04 16:49:07', '2026-07-08 17:48:26', 'PHYSICAL', NULL, NULL, NULL, NULL, NULL, 1, 7);
INSERT INTO `points_products` VALUES (24, 'Cozy 经典帆布包', '选用高磅数加厚原生帆布制作，结实耐用。极简的米色基底搭配经典的 CozyCoffee Logo 印花，内设实用收纳暗袋。无论是通勤、上课还是周末采购，它都是你最实用且时髦的日常单品。', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/Cozy%20%E7%BB%8F%E5%85%B8%E5%B8%86%E5%B8%83%E5%8C%85.png', 1600, NULL, 99, 'active', 'gift', '2026-01-04 16:49:45', '2026-07-08 17:48:26', 'PHYSICAL', NULL, NULL, NULL, NULL, NULL, 1, 7);
INSERT INTO `points_products` VALUES (25, '“COZY”主题 T 恤', '【黄金及以上会员专享】 核心粉丝的身份认证。采用优质精梳棉面料，亲肤透气。复古风格的“COZY”主题印花设计，不仅舒适好穿，更是一种生活态度的低调表达，做 Cozy 社区的最佳代言人。', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E2%80%9CCOZY%E2%80%9D%E4%B8%BB%E9%A2%98%20T%20%E6%81%A4.png', 2200, NULL, 100, 'active', 'gift', '2026-01-04 16:50:17', '2026-07-08 17:48:26', 'PHYSICAL', NULL, NULL, NULL, NULL, NULL, 1, 7);
INSERT INTO `points_products` VALUES (26, '挂耳咖啡精选礼盒', '汇集全球优质产区的精选咖啡豆（如埃塞俄比亚、哥伦比亚等），制成便携挂耳包，随时随地享受精品风味。精美的定制礼盒包装，无论是自用“囤货”，还是作为礼物馈赠亲友，都诚意十足。', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E6%8C%82%E8%80%B3%E5%92%96%E5%95%A1%E7%B2%BE%E9%80%89%E7%A4%BC%E7%9B%92.png', 3500, NULL, 19, 'active', 'gift', '2026-01-04 16:51:22', '2026-07-08 17:48:26', 'PHYSICAL', NULL, NULL, NULL, NULL, NULL, 1, 7);
INSERT INTO `points_products` VALUES (27, '买一赠一券', '仅限饮品类:不适用于瓶装饮料、手冲咖啡、烘焙甜品及周边商品。\n不可分次核销：必须在单笔订单中包含至少两杯饮品方可触发，不支持\"存一杯\"操作。\n不与其他主券叠加：买一送一券属于主券类型，不可与折扣券、满减券同时使用。', 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/%E4%B9%B0%E4%B8%80%E9%80%81%E4%B8%80%E5%88%B8.png', 350, NULL, 95, 'active', 'coupon', '2026-01-10 17:05:00', '2026-07-08 17:48:26', 'PHYSICAL', 'BOGO', 40, NULL, NULL, NULL, 10, 7);

-- ----------------------------
-- Table structure for user_coupons
-- ----------------------------
DROP TABLE IF EXISTS `user_coupons`;
CREATE TABLE `user_coupons`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `source_points_order_id` bigint NULL DEFAULT NULL COMMENT '来源积分兑换订单ID',
  `coupon_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '券码（唯一）',
  `coupon_type` enum('BOGO','DELIVERY_FEE','DISCOUNT','EXCHANGE','FULL_REDUCE','NEW_PRODUCT_FREE','NEW_PRODUCT_HALF','SHOT','T_ADDON','T3_ALL_FREE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'åˆ¸ç±»åž‹',
  `rule_json` json NOT NULL COMMENT '券规则JSON',
  `display_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '显示标题（如\"生日5折券\"）',
  `display_sub_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '显示副标题（如\"限标准杯\"）',
  `status` enum('ISSUED','USED','EXPIRED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ISSUED' COMMENT '状态',
  `issued_at` datetime NOT NULL COMMENT '发放时间',
  `expires_at` datetime NOT NULL COMMENT '到期时间',
  `used_at` datetime NULL DEFAULT NULL COMMENT '使用时间',
  `used_shop_order_id` bigint NULL DEFAULT NULL COMMENT '核销关联的咖啡订单ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_coupon_code`(`coupon_code` ASC) USING BTREE,
  UNIQUE INDEX `uk_user_used_order`(`user_id` ASC, `used_shop_order_id` ASC) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_expires`(`expires_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 324 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户券实例表（下单即核销）' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
