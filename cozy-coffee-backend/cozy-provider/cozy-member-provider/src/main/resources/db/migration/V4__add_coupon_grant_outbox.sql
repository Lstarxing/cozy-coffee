-- 发券请求的本地 outbox（member → mall）。
-- member 不再同步 RPC 调 mall 发券：与业务数据同事务写入本表，提交后投递 MQ，由 mall 消费并发券。
-- uk_unique_key 保证同一次发券只入队一次（unique_key 即 mall 侧 user_coupon.coupon_code 的幂等键）。
CREATE TABLE IF NOT EXISTS `coupon_grant_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `unique_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `payload` json NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_unique_key`(`unique_key` ASC) USING BTREE,
  INDEX `idx_status_next_retry`(`status` ASC, `next_retry_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;
