-- 验证订单完成后积分和经验发放及月度统计更新

-- 1. 查看最近完成的订单
SELECT 
    id,
    order_no,
    user_id,
    status,
    pay_amount,
    exp_earned,
    points_earned,
    rewards_granted,
    created_at,
    updated_at
FROM cozy_order.shop_orders
WHERE status = 'completed'
  AND user_id = 22
ORDER BY updated_at DESC
LIMIT 5;

-- 2. 查看会员信息（应该包括积分、EXP 和月度统计）
SELECT 
    id,
    user_id,
    member_level,
    current_points,
    total_points,
    exp_total,
    monthly_spent,
    monthly_spent_month,
    monthly_accelerate_remaining,
    updated_at
FROM cozy_member.member_info
WHERE user_id = 22;

-- 3. 查看积分流水记录
SELECT 
    id,
    user_id,
    change_amount,
    balance_after,
    source_type,
    description,
    created_at
FROM cozy_member.points_transactions
WHERE user_id = 22
ORDER BY created_at DESC
LIMIT 10;

-- 4. 查看积分批次（lot）
SELECT 
    id,
    user_id,
    initial_amount,
    remaining,
    source_type,
    source_id,
    expires_at,
    created_at
FROM cozy_member.points_lots
WHERE user_id = 22
ORDER BY created_at DESC
LIMIT 10;

-- 5. 诊断：检查是否有 rewards_granted=true 但积分未增加的订单
SELECT 
    o.id as order_id,
    o.order_no,
    o.pay_amount,
    o.exp_earned as order_exp,
    o.points_earned as order_points,
    o.rewards_granted,
    m.exp_total as member_exp,
    m.current_points as member_points,
    m.monthly_spent,
    m.monthly_spent_month
FROM cozy_order.shop_orders o
LEFT JOIN cozy_member.member_info m ON o.user_id = m.user_id
WHERE o.user_id = 22
  AND o.status = 'completed'
  AND o.rewards_granted = true
ORDER BY o.updated_at DESC
LIMIT 5;

-- 6. 检查最近的积分批次是否与最近完成的订单对应
SELECT 
    o.id as order_id,
    o.order_no,
    o.pay_amount,
    o.points_earned,
    o.updated_at as order_completed_at,
    lot.id as lot_id,
    lot.initial_amount as lot_points,
    lot.source_id as lot_order_id,
    lot.created_at as lot_created_at,
    CASE 
        WHEN o.id = lot.source_id THEN 'MATCH'
        ELSE 'MISMATCH'
    END as match_status
FROM cozy_order.shop_orders o
LEFT JOIN cozy_member.points_lots lot ON o.id = lot.source_id AND lot.source_type = 'order_completed'
WHERE o.user_id = 22
  AND o.status = 'completed'
ORDER BY o.updated_at DESC
LIMIT 5;
