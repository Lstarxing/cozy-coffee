-- 测试优惠券取消订单退还逻辑
-- 场景：创建订单使用折扣券，然后取消订单，验证券是否返回

USE cozy_mall;

-- 1. 查看当前用户的可用折扣券
SELECT 
    id,
    user_id,
    coupon_code,
    coupon_type,
    status,
    rule_json,
    expires_at
FROM user_coupons
WHERE user_id = 22 
  AND status = 'ISSUED'
  AND coupon_type = 'DISCOUNT'
LIMIT 5;

-- 2. 查看最近的订单及优惠券使用情况（跨库查询）
SELECT 
    o.id,
    o.order_no,
    o.user_id,
    o.status,
    o.total_amount,
    o.discount_amount,
    o.pay_amount,
    o.applied_coupon_id,
    o.created_at,
    c.coupon_code,
    c.status as coupon_status,
    c.used_at
FROM cozy_order.shop_orders o
LEFT JOIN user_coupons c ON o.applied_coupon_id = c.id
WHERE o.user_id = 22
  AND o.applied_coupon_id IS NOT NULL
ORDER BY o.created_at DESC
LIMIT 10;

-- 3. 检查是否有 cancelled 订单的券未归还（这是问题核心）
SELECT 
    o.id as order_id,
    o.order_no,
    o.status as order_status,
    o.applied_coupon_id,
    c.id as coupon_id,
    c.coupon_code,
    c.status as coupon_status,
    c.used_at,
    o.created_at as order_created,
    o.updated_at as order_updated
FROM cozy_order.shop_orders o
INNER JOIN user_coupons c ON o.applied_coupon_id = c.id
WHERE o.user_id = 22
  AND o.status = 'cancelled'
  AND c.status = 'USED'  -- 问题：应该返回为 ISSUED
ORDER BY o.created_at DESC;

-- 4. 检查 ruleJson 格式（验证JSON解析是否有问题）
SELECT 
    id,
    coupon_code,
    coupon_type,
    rule_json,
    CASE
        WHEN rule_json LIKE '%"value":%' THEN 'OK: standard format'
        WHEN rule_json LIKE '%"value" :%' THEN 'WARNING: space after key'
        WHEN rule_json LIKE '%value%' THEN 'WARNING: has value but format unclear'
        ELSE 'ERROR: no value field'
    END as json_format_check
FROM user_coupons
WHERE user_id = 22
  AND coupon_type IN ('DISCOUNT', 'FULL_REDUCE')
LIMIT 10;
