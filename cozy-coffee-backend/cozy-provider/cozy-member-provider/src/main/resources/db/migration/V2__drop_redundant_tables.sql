-- 清理 member 冗余表（为上线瘦身）
-- 依据：
--   - monthly_challenge_config：挑战配置已迁 yml @ConfigurationProperties（cozy.member.monthly-challenge，
--     MonthlyChallengeConfig 单一事实源，代码注释明确不再读表）
--   - monthly_challenge_progress：挑战进度已存 monthly_tasks.challenge_*_claimed 列，0 行无消费
--   - system_config：占位表，0 行无任何消费方
--   - v_points_balance_check：积分余额诊断视图，无代码消费
-- 已确认：无任何表外键引用这些表，DROP 安全

DROP VIEW IF EXISTS `v_points_balance_check`;

DROP TABLE IF EXISTS `monthly_challenge_config`;
DROP TABLE IF EXISTS `monthly_challenge_progress`;
DROP TABLE IF EXISTS `system_config`;
