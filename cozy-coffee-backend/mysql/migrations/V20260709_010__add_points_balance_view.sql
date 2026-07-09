-- DH8: points_lots vs member_info.current_points 对账视图 + 检查 SQL
-- NFR-11: 积分余额 = SUM(points_lots.remaining)，需逐笔可对账
USE cozy_member;

-- 对账视图：实时计算 SUM(lots) vs member_info.current_points
CREATE OR REPLACE VIEW v_points_balance_check AS
SELECT
  m.user_id,
  m.current_points AS member_balance,
  COALESCE(SUM(l.remaining), 0) AS lots_balance,
  m.current_points - COALESCE(SUM(l.remaining), 0) AS diff,
  COUNT(l.id) AS lot_count
FROM member_info m
LEFT JOIN points_lots l ON l.user_id = m.user_id AND l.remaining > 0
GROUP BY m.user_id, m.current_points
HAVING diff != 0;

-- 手动执行对账检查（Phase 2 admin 里可加定时任务）
-- SELECT * FROM v_points_balance_check;
-- 预期：0 行（所有用户 SUM(lots) == current_points）
-- 如有差异：memberService.fixPointsConsistency(userId) 可修复
