-- ============================================================
-- 重置用户 38 的会员权益「已领取」记录（开发/测试用，非生产迁移）
-- 用途：清掉月度权益 / 晋升礼 / 生日礼 / 签到 / 月度挑战 / 首单 / 邀请 / 完善资料 的
--       「已领取」标记与已发放券，便于重新测试各领取流程。
-- 说明：删流水只清幂等标记，member_info.current_points 不会自动回退（见文末）。
-- ============================================================

-- ===================== cozy_member 库 =====================
USE cozy_member;

-- 1. 月度权益（2026-08）
DELETE FROM points_transactions WHERE user_id = 38 AND source_type = 'monthly_benefit_202608';

-- 2. 晋升礼（积分流水 + 标记）
DELETE FROM points_transactions WHERE user_id = 38 AND source_type = 'upgrade_reward';

-- 3. 生日礼
DELETE FROM points_transactions WHERE user_id = 38 AND source_type = 'birthday_gift';

-- 4. 签到（重测当日签到）
DELETE FROM signin_records WHERE user_id = 38;
UPDATE member_info SET last_signin_date = NULL, consecutive_sign_days = 0 WHERE user_id = 38;

-- 5. 月度挑战：重置 claimed 标志 + 已发积分流水
UPDATE monthly_task SET
  challenge_order_claimed = 0,
  challenge_morning_claimed = 0,
  challenge_delivery_claimed = 0,
  challenge_newproduct_claimed = 0
WHERE user_id = 38 AND task_month = '2026-08';
DELETE FROM points_transactions
WHERE user_id = 38
  AND source_type IN ('challenge_order','challenge_morning','challenge_delivery','challenge_newproduct');

-- 6. 首单奖励
DELETE FROM points_lot WHERE user_id = 38 AND source_type = 'first_order_bonus';

-- 7. 完善资料奖励
DELETE FROM points_transactions WHERE user_id = 38 AND source_type = 'profile';

-- ===================== cozy_mall 库 =====================
USE cozy_mall;

-- 已发放的相关券（月度 / 晋升 / 生日 / 邀请）
DELETE FROM user_coupons
WHERE user_id = 38
  AND (coupon_code LIKE '%_202608_%'
    OR coupon_code LIKE 'upgrade_%'
    OR coupon_code LIKE 'birthday_%'
    OR coupon_code LIKE 'invite_firstorder_%');

-- ===================== cozy_user 库 =====================
USE cozy_user;

-- 邀请奖励标记
UPDATE user SET invite_reward_granted = 0 WHERE id = 38;

-- ============================================================
-- 使用说明：
-- 1) 文件内跨三库，请在 MySQL 客户端整体执行（有相应库权限）；或按节复制到各库连接执行。
-- 2) current_points 不回退：若要精确重测积分，手动执行：
--      UPDATE member_info SET current_points = current_points - <已领积分> WHERE user_id = 38;
-- 3) 重置后建议清理 Redis profile 缓存，避免读到旧状态：
--      redis-cli KEYS 'member:profile:*' | xargs redis-cli DEL
-- ============================================================
