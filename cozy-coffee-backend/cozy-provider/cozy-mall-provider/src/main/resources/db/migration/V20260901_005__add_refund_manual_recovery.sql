ALTER TABLE `points_refund_outbox`
  ADD COLUMN `manual_retry_count` int NOT NULL DEFAULT 0 AFTER `retry_count`,
  ADD COLUMN `last_manual_retry_at` datetime NULL AFTER `last_error`,
  ADD COLUMN `last_manual_retry_by` bigint NULL AFTER `last_manual_retry_at`;
