CREATE TABLE IF NOT EXISTS `coupon_rollback_inbox` (
  `event_id` varchar(96) NOT NULL COMMENT '券回滚事件幂等键',
  `processed_at` datetime NOT NULL,
  PRIMARY KEY (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='优惠券回滚消费去重表';

CREATE TABLE IF NOT EXISTS `points_refund_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `points` int NOT NULL,
  `consume_type` varchar(32) NOT NULL,
  `description` varchar(255) NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime NOT NULL,
  `locked_at` datetime NULL,
  `last_error` varchar(500) NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_order` (`consume_type`, `order_id`),
  KEY `idx_refund_relay` (`status`, `next_retry_at`, `locked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='积分兑换取消退款本地 outbox';
