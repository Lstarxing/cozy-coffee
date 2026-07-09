USE cozy_mall;
ALTER TABLE user_coupons
  MODIFY COLUMN coupon_type VARCHAR(30) DEFAULT NULL COMMENT '券类型';
