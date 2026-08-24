-- 优惠券类型 enum → varchar(40)：解除新增券类型需 ALTER TABLE 的约束
-- coupon_type 存算法类型（BOGO/DISCOUNT/EXCHANGE/FULL_REDUCE/SHOT/DELIVERY_FEE/NEW_PRODUCT_*/CAKE_HALF），模板标识在发券侧配置驱动
ALTER TABLE `user_coupons`
  MODIFY COLUMN `coupon_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '券类型';
