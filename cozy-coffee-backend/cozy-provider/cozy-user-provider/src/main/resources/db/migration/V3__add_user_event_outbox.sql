CREATE TABLE IF NOT EXISTS `user_event_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tag` varchar(64) NOT NULL COMMENT 'USER_EVENTS 下的事件标签',
  `unique_key` varchar(128) NOT NULL COMMENT '业务幂等键',
  `user_id` bigint NOT NULL COMMENT '事件主体用户ID',
  `payload` json NOT NULL COMMENT '事件载荷JSON',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/DEAD',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_unique_key` (`tag`, `unique_key`),
  KEY `idx_status_next_retry` (`status`, `next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户生命周期事件本地消息表';
