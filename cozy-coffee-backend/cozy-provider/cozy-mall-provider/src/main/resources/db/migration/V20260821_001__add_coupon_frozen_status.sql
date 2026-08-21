-- 优惠券冻结状态：下单时 ISSUED → FROZEN（待支付冻结），支付/接单后 FROZEN → USED，取消回滚 ISSUED
ALTER TABLE `user_coupons`
  MODIFY COLUMN `status` enum('ISSUED','FROZEN','USED','EXPIRED')
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
  NOT NULL DEFAULT 'ISSUED' COMMENT '状态';
