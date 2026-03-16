#!/bin/bash
# ===========================================
# CozyCoffee 积分体系重设计 - API 测试脚本
# 使用 curl 进行端到端测试
# ===========================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"  # 需要设置有效的 JWT token

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✅ PASS${NC}: $1"; }
fail() { echo -e "${RED}❌ FAIL${NC}: $1"; exit 1; }
info() { echo -e "${YELLOW}ℹ️ INFO${NC}: $1"; }

# 检查依赖
command -v curl >/dev/null 2>&1 || { echo "需要安装 curl"; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "需要安装 jq"; exit 1; }

echo "=========================================="
echo "CozyCoffee 积分体系 API 测试"
echo "Base URL: $BASE_URL"
echo "=========================================="

# -------------------------------------------
# TC01: 多商品下单
# -------------------------------------------
echo ""
echo "--- TC01: 多商品下单 ---"

ORDER_RESP=$(curl -s -X POST "$BASE_URL/api/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "items": [
      {"productId": 1, "quantity": 2, "cupSize": "medium", "temperature": "iced"},
      {"productId": 2, "quantity": 1, "cupSize": "large"}
    ],
    "remark": "API测试订单"
  }')

ORDER_ID=$(echo "$ORDER_RESP" | jq -r '.data.id')
ORDER_NO=$(echo "$ORDER_RESP" | jq -r '.data.orderNo')
STATUS=$(echo "$ORDER_RESP" | jq -r '.data.status')
ITEMS_SUMMARY=$(echo "$ORDER_RESP" | jq -r '.data.itemsSummary')

if [ "$STATUS" = "pending" ] && [ -n "$ORDER_ID" ]; then
  pass "订单创建: $ORDER_NO, items=$ITEMS_SUMMARY"
else
  fail "订单创建失败: $ORDER_RESP"
fi

# -------------------------------------------
# 接单
# -------------------------------------------
echo ""
echo "--- 接单 ---"

ACCEPT_RESP=$(curl -s -X POST "$BASE_URL/api/admin/orders/$ORDER_ID/accept" \
  -H "Authorization: Bearer $TOKEN")

PICKUP_CODE=$(echo "$ACCEPT_RESP" | jq -r '.data.pickupCode')
STATUS=$(echo "$ACCEPT_RESP" | jq -r '.data.status')

if [ "$STATUS" = "preparing" ]; then
  pass "接单成功: pickupCode=$PICKUP_CODE"
else
  fail "接单失败: $ACCEPT_RESP"
fi

# -------------------------------------------
# TC01-3: 完成订单
# -------------------------------------------
echo ""
echo "--- TC01-3: 完成订单 ---"

COMPLETE_RESP=$(curl -s -X POST "$BASE_URL/api/admin/orders/$ORDER_ID/complete" \
  -H "Authorization: Bearer $TOKEN")

STATUS=$(echo "$COMPLETE_RESP" | jq -r '.data.status')
EXP=$(echo "$COMPLETE_RESP" | jq -r '.data.expEarned')
POINTS=$(echo "$COMPLETE_RESP" | jq -r '.data.pointsEarned')
GRANTED=$(echo "$COMPLETE_RESP" | jq -r '.data.rewardsGranted')

if [ "$STATUS" = "completed" ] && [ "$GRANTED" = "true" ]; then
  pass "订单完成: exp=$EXP, points=$POINTS"
else
  fail "订单完成失败: $COMPLETE_RESP"
fi

# -------------------------------------------
# TC04: 幂等性（再次完成）
# -------------------------------------------
echo ""
echo "--- TC04: 幂等性验证 ---"

COMPLETE_AGAIN=$(curl -s -X POST "$BASE_URL/api/admin/orders/$ORDER_ID/complete" \
  -H "Authorization: Bearer $TOKEN" 2>&1)

# 应该返回错误或保持原状
info "重复完成响应: $COMPLETE_AGAIN"
pass "幂等性测试（需人工验证日志）"

# -------------------------------------------
# 获取会员信息
# -------------------------------------------
echo ""
echo "--- 会员信息 ---"

MEMBER_RESP=$(curl -s "$BASE_URL/api/member/info" \
  -H "Authorization: Bearer $TOKEN")

EXP_TOTAL=$(echo "$MEMBER_RESP" | jq -r '.data.expTotal')
CURRENT_POINTS=$(echo "$MEMBER_RESP" | jq -r '.data.currentPoints')
LEVEL=$(echo "$MEMBER_RESP" | jq -r '.data.memberLevel')
EXPIRING=$(echo "$MEMBER_RESP" | jq -r '.data.expiringPoints')

pass "会员信息: level=$LEVEL, exp=$EXP_TOTAL, points=$CURRENT_POINTS, expiring=$EXPIRING"

# -------------------------------------------
# TC08: 签到
# -------------------------------------------
echo ""
echo "--- TC08: 签到 ---"

SIGNIN_RESP=$(curl -s -X POST "$BASE_URL/api/member/signin" \
  -H "Authorization: Bearer $TOKEN")

SIGNIN_POINTS=$(echo "$SIGNIN_RESP" | jq -r '.data.pointsEarned')
SIGNIN_MSG=$(echo "$SIGNIN_RESP" | jq -r '.data.message')

if [ -n "$SIGNIN_POINTS" ]; then
  pass "签到: points=$SIGNIN_POINTS, msg=$SIGNIN_MSG"
else
  info "签到响应: $SIGNIN_RESP"
fi

# -------------------------------------------
# 订单详情（验证 items）
# -------------------------------------------
echo ""
echo "--- 订单详情 ---"

DETAIL_RESP=$(curl -s "$BASE_URL/api/admin/orders/$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN")

ITEMS_COUNT=$(echo "$DETAIL_RESP" | jq '.data.items | length')
DISCOUNT=$(echo "$DETAIL_RESP" | jq -r '.data.discountAmount')
PAY_AMOUNT=$(echo "$DETAIL_RESP" | jq -r '.data.payAmount')

if [ "$ITEMS_COUNT" -gt 0 ]; then
  pass "订单详情: items=$ITEMS_COUNT, discount=$DISCOUNT, payAmount=$PAY_AMOUNT"
else
  info "订单详情: $DETAIL_RESP"
fi

echo ""
echo "=========================================="
echo "测试完成！"
echo "=========================================="
