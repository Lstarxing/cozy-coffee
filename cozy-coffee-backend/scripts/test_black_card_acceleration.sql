-- ============================================
-- 黑卡加速包测试脚本
-- ============================================

-- 1. 设置用户ID（替换为您的实际用户ID）
SET @user_id = 22;  -- ⬅️ 修改这里

-- 2. 查看当前状态
SELECT 
    '当前月消费情况' as info,
    COUNT(*) as completed_orders,
    COALESCE(SUM(pay_amount), 0) as monthly_spent,
    300 - COALESCE(SUM(pay_amount), 0) as remaining_cap
FROM shop_orders
WHERE user_id = @user_id
  AND status = 'completed'
  AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01');

-- 3. 查看详细订单列表
SELECT 
    id,
    order_no,
    status,
    total_amount,
    pay_amount,
    points_earned,
    points_multiplier,
    DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') as order_time
FROM shop_orders
WHERE user_id = @user_id
  AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')
ORDER BY created_at DESC;

-- ============================================
-- 方案A：插入模拟订单（推荐）
-- ============================================

-- 插入280元的模拟消费
-- 订单1: 100元（享受1.70倍全额）
INSERT INTO shop_orders (
    order_no, user_id, total_amount, total_quantity, 
    discount_amount, pay_amount,
    exp_earned, points_earned, points_multiplier, rewards_granted,
    status, store_id, business_date, pickup_code, 
    pickup_code_generated_at, created_at, updated_at
) VALUES (
    'CF202512010001MOCK', @user_id, 100.00, 1, 0, 100.00,
    100, 170, 1.70, TRUE, 'completed', 1, CURDATE(), '001',
    NOW(), DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL 1 DAY), NOW()
);

-- 订单2: 100元（享受1.70倍全额，剩余100额度）
INSERT INTO shop_orders (
    order_no, user_id, total_amount, total_quantity, 
    discount_amount, pay_amount,
    exp_earned, points_earned, points_multiplier, rewards_granted,
    status, store_id, business_date, pickup_code, 
    pickup_code_generated_at, created_at, updated_at
) VALUES (
    'CF202512050001MOCK', @user_id, 100.00, 1, 0, 100.00,
    100, 170, 1.70, TRUE, 'completed', 1, CURDATE(), '002',
    NOW(), DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL 5 DAY), NOW()
);

-- 订单3: 80元（享受1.70倍全额，剩余20额度）
INSERT INTO shop_orders (
    order_no, user_id, total_amount, total_quantity, 
    discount_amount, pay_amount,
    exp_earned, points_earned, points_multiplier, rewards_granted,
    status, store_id, business_date, pickup_code, 
    pickup_code_generated_at, created_at, updated_at
) VALUES (
    'CF202512100001MOCK', @user_id, 80.00, 1, 0, 80.00,
    80, 136, 1.70, TRUE, 'completed', 1, CURDATE(), '003',
    NOW(), DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL 10 DAY), NOW()
);

-- 验证：应该显示 280元，剩余20元额度
SELECT 
    '模拟订单插入后' as info,
    COUNT(*) as completed_orders,
    COALESCE(SUM(pay_amount), 0) as monthly_spent,
    300 - COALESCE(SUM(pay_amount), 0) as remaining_cap
FROM shop_orders
WHERE user_id = @user_id
  AND status = 'completed'
  AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01');

-- ============================================
-- 测试场景：新订单50元
-- ============================================
-- 期望结果：
-- - 前20元：1.70倍 = 34积分
-- - 后30元：1.35倍 = 40.5 → 41积分
-- - 总计：75积分
-- - 实际倍率：1.50 (75/50)

-- 现在去Apifox创建一个50元的订单，观察：
-- pointsEarned 应该是 75
-- pointsMultiplier 应该约 1.50

-- ============================================
-- 测试场景：超额订单（月消费已达350元）
-- ============================================

-- 再插入一个订单，使总消费达到350元
INSERT INTO shop_orders (
    order_no, user_id, total_amount, total_quantity, 
    discount_amount, pay_amount,
    exp_earned, points_earned, points_multiplier, rewards_granted,
    status, store_id, business_date, pickup_code, 
    pickup_code_generated_at, created_at, updated_at
) VALUES (
    'CF202512150001MOCK', @user_id, 70.00, 1, 0, 70.00,
    70, 95, 1.35, TRUE, 'completed', 1, CURDATE(), '004',
    NOW(), DATE_ADD(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL 15 DAY), NOW()
);

-- 验证：应该显示 350元，剩余0元额度
SELECT 
    '超额测试' as info,
    COUNT(*) as completed_orders,
    COALESCE(SUM(pay_amount), 0) as monthly_spent,
    300 - COALESCE(SUM(pay_amount), 0) as remaining_cap
FROM shop_orders
WHERE user_id = @user_id
  AND status = 'completed'
  AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01');

-- 现在创建新订单（45元），期望：
-- - 全部45元：1.35倍 = 60.75 → 61积分
-- - 实际倍率：1.36 (61/45)

-- ============================================
-- 清理测试数据
-- ============================================

-- 删除所有模拟订单
DELETE FROM shop_orders 
WHERE order_no LIKE '%MOCK%'
  AND user_id = @user_id;

-- 验证清理结果
SELECT 
    '清理后' as info,
    COUNT(*) as completed_orders,
    COALESCE(SUM(pay_amount), 0) as monthly_spent
FROM shop_orders
WHERE user_id = @user_id
  AND status = 'completed'
  AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01');

-- ============================================
-- 方案B：快速修改现有订单（不推荐）
-- ============================================

-- 如果您有现有订单，可以临时修改金额
-- 先查询订单
SELECT id, order_no, pay_amount, status
FROM shop_orders
WHERE user_id = @user_id
  AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')
ORDER BY created_at DESC
LIMIT 1;

-- 修改为280元（记录原值，测试后恢复）
-- UPDATE shop_orders 
-- SET pay_amount = 280.00
-- WHERE id = ?  -- 替换为实际ID
--   AND user_id = @user_id;
