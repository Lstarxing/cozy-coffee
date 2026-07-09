-- DH4: user_coupons.coupon_type VARCHAR(30) -> ENUM
-- 防状态失控，与 shop_orders.status ENUM (DC7) 对齐
USE cozy_mall;

-- 已确认现存 10 种值均有数据行，MODIFY 不会失败
ALTER TABLE user_coupons
  MODIFY COLUMN coupon_type ENUM(
    'BOGO','DELIVERY_FEE','DISCOUNT','EXCHANGE','FULL_REDUCE',
    'NEW_PRODUCT_FREE','NEW_PRODUCT_HALF','SHOT','T_ADDON','T3_ALL_FREE'
  ) DEFAULT NULL COMMENT '券类型';

-- 未来新增券类型用 coupon_type_config 配置表 (4.7.2)，
-- 不再 ALTER TABLE MODIFY ENUM
