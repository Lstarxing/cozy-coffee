-- DH5: Add unique key to prevent coupon double-spending
-- (user_id, used_shop_order_id) ensures one coupon per user per order
USE cozy_mall;

ALTER TABLE user_coupons
  ADD UNIQUE KEY uk_user_used_order (user_id, used_shop_order_id);
