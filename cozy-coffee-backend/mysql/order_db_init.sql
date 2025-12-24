-- =============================================
-- 订单服务独立数据库 (cozy_order)
-- 完全匹配 cozy_coffee 相关表结构
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS cozy_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cozy_order;

-- 咖啡商品表
DROP TABLE IF EXISTS `coffee_products`;
CREATE TABLE `coffee_products`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品描述',
  `price` decimal(10, 2) NOT NULL COMMENT '价格（元）',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '商品图片',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'coffee' COMMENT '分类：coffee/dessert/other',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'active' COMMENT '状态：active/inactive',
  `sort_order` int(0) NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咖啡商品表' ROW_FORMAT = Dynamic;

-- 咖啡消费订单表
DROP TABLE IF EXISTS `shop_orders`;
CREATE TABLE `shop_orders`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `product_id` bigint(0) NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称（冗余）',
  `quantity` int(0) NULL DEFAULT 1 COMMENT '数量',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单总额',
  `points_earned` int(0) NOT NULL COMMENT '获得积分',
  `points_multiplier` decimal(3, 1) NOT NULL COMMENT '积分倍率',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'completed' COMMENT '状态：pending/completed/cancelled',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '下单时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_order_no`(`order_no`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咖啡消费订单表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ 订单数据库 cozy_order 创建完成！' AS status;
