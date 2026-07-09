-- Rollback DH7: drop config + progress tables
USE cozy_member;
DROP TABLE IF EXISTS monthly_challenge_progress;
DROP TABLE IF EXISTS monthly_challenge_config;
