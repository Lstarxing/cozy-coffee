-- =============================================
-- 积分商城独立数据库 (cozy_mall)
-- 完全匹配 cozy_coffee 相关表结构
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS cozy_mall CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cozy_mall;

-- 积分商品表
DROP TABLE IF EXISTS `points_products`;
CREATE TABLE `points_products`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品描述',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片URL',
  `points_price` int(0) NOT NULL COMMENT '兑换所需积分',
  `original_price` decimal(10, 2) NULL COMMENT '商品原价',
  `stock` int(0) NULL DEFAULT 0 COMMENT '库存数量',
  `status` enum('active','inactive','sold_out') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'active' COMMENT '商品状态',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品分类：drink/merchandise/coupon',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_category`(`category`) USING BTREE,
  INDEX `idx_points_price`(`points_price`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分商品表' ROW_FORMAT = Dynamic;

-- 积分兑换订单表
DROP TABLE IF EXISTS `points_orders`;
CREATE TABLE `points_orders`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `product_id` bigint(0) NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称（冗余）',
  `product_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片（冗余）',
  `points_cost` int(0) NOT NULL COMMENT '消耗积分',
  `quantity` int(0) NULL DEFAULT 1 COMMENT '兑换数量',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收货电话',
  `receiver_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '完整收货地址',
  `status` enum('pending','processing','shipped','completed','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'pending' COMMENT '订单状态：pending待处理/processing处理中/shipped已发货/completed已完成/cancelled已取消',
  `shipping_company` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '快递公司',
  `tracking_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '快递单号',
  `shipped_at` datetime(0) NULL DEFAULT NULL COMMENT '发货时间',
  `completed_at` datetime(0) NULL DEFAULT NULL COMMENT '完成时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '下单时间',
  `updated_at` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_order_no`(`order_no`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  INDEX `product_id`(`product_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分兑换订单表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ 积分商城数据库 cozy_mall 创建完成！' AS status;
