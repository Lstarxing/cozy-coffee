-- 诊断脚本：检查优惠券校验问题和数据冗余

-- 1. 检查优惠券的 ruleJson 配置
SELECT 
    id,
    coupon_code,
    coupon_type,
    rule_json,
    status
FROM user_coupons 
WHERE coupon_code IN ('UBF36ADW', 'BKAA2W2U')
ORDER BY created_at DESC;

-- 2. 检查积分商品的优惠券配置
SELECT 
    id,
    name,
    category,
    coupon_type,
    coupon_value,
    face_value,
    linked_product_id,
    min_order_amount,
    product_type
FROM points_products 
WHERE category = 'coupon'
ORDER BY id;

-- 3. 检查咖啡商品ID（用于匹配）
SELECT 
    id,
    name,
    price,
    status
FROM coffee_products
WHERE id IN (5, 8)
ORDER BY id;

-- 4. 数据冗余分析

-- 4.1 检查 points_products 的 product_type 字段使用情况
SELECT 
    product_type,
    COUNT(*) as count,
    GROUP_CONCAT(DISTINCT category) as categories
FROM points_products
GROUP BY product_type;

-- 4.2 检查 points_orders 的 fulfillment_type 字段使用情况
SELECT 
    fulfillment_type,
    COUNT(*) as count
FROM points_orders
GROUP BY fulfillment_type;

-- 4.3 检查 user_coupons.coupon_code 和 points_order_fulfillments.virtual_code 的关联
SELECT 
    uc.coupon_code as user_coupon_code,
    pof.virtual_code as fulfillment_virtual_code,
    po.id as order_id,
    po.order_no
FROM user_coupons uc
LEFT JOIN points_orders po ON uc.source_points_order_id = po.id
LEFT JOIN points_order_fulfillments pof ON po.id = pof.order_id
WHERE uc.source_points_order_id IS NOT NULL
LIMIT 10;

-- 5. 查找可能的校验问题
-- 检查是否有 EXCHANGE 类型但 linkedProductId 为空的券
SELECT 
    id,
    coupon_code,
    coupon_type,
    rule_json,
    JSON_EXTRACT(rule_json, '$.linkedProductId') as extracted_linked_id
FROM user_coupons
WHERE coupon_type = 'EXCHANGE'
AND (
    rule_json NOT LIKE '%linkedProductId%'
    OR JSON_EXTRACT(rule_json, '$.linkedProductId') IS NULL
)
LIMIT 10;
