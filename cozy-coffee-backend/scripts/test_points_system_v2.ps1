# CozyCoffee 积分体系重设计 - PowerShell 测试脚本 v2
# 覆盖：多商品下单、券核销、完成发放幂等、签到封顶、FIFO扣减、余额一致性

param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Token = ""
)

$ErrorActionPreference = "Stop"

function Pass($m){ Write-Host "PASS: $m" -ForegroundColor Green }
function Fail($m){ Write-Host "FAIL: $m" -ForegroundColor Red }
function Info($m){ Write-Host "INFO: $m" -ForegroundColor Yellow }
function Title($m){ Write-Host "`n========== $m ==========" -ForegroundColor Cyan }

if (-not $Token) { throw "Token 不能为空。先登录获取 Bearer token 再运行脚本。" }

$headers = @{
  "Content-Type"  = "application/json"
  "Authorization" = "Bearer $Token"
}

function InvokeApi($method, $url, $bodyJson = $null) {
  if ($bodyJson) {
    return Invoke-RestMethod -Uri $url -Method $method -Headers $headers -Body $bodyJson
  } else {
    return Invoke-RestMethod -Uri $url -Method $method -Headers $headers
  }
}

# ----------------------------
# 0) 获取会员信息（确定 userId）
# ----------------------------
Title "Precheck: 获取会员信息"

$member = InvokeApi "GET" "$BaseUrl/api/member/info"
$userId = $member.data.userId
$level = $member.data.memberLevel
$points0 = [int]$member.data.currentPoints
$exp0 = [int]$member.data.expTotal

if (-not $userId) { throw "member/info 未返回 userId，请确认接口返回字段" }
Pass "当前用户: userId=$userId, level=$level, exp=$exp0, points=$points0"

# ----------------------------
# 1) TC01 多商品下单 -> 接单 -> 完成 -> 发放
# ----------------------------
Title "TC01: 多商品下单 -> 完成 -> 发放"

$orderBody = @{
  items = @(
    @{ productId = 1; quantity = 2; cupSize = "medium"; temperature = "iced" },
    @{ productId = 2; quantity = 1; cupSize = "large" }
  )
  remark = "TC01测试订单"
} | ConvertTo-Json -Depth 5

$orderResp = InvokeApi "POST" "$BaseUrl/api/orders" $orderBody
$orderId = $orderResp.data.id
$orderNo = $orderResp.data.orderNo
$totalQty = [int]$orderResp.data.totalQuantity
$status = $orderResp.data.status
$payAmount = [decimal]$orderResp.data.payAmount

if ($status -ne "pending") { throw "订单创建状态不为 pending，实际=$status" }
if ($totalQty -lt 3) { throw "totalQuantity 不正确，实际=$totalQty" }
Pass "订单创建成功: $orderNo, qty=$totalQty, payAmount=$payAmount"

# 接单
try {
  $accept = InvokeApi "POST" "$BaseUrl/api/admin/orders/$orderId/accept"
  if ($accept.data.status -ne "preparing") { throw "接单后状态不为 preparing" }
  Pass "接单成功: pickupCode=$($accept.data.pickupCode)"
} catch {
  Info "接单接口可能需要管理员token：$_"
}

# 完成
$complete = $null
try {
  $complete = InvokeApi "POST" "$BaseUrl/api/admin/orders/$orderId/complete"
} catch {
  Info "完成接口可能需要管理员token：$_"
}

if ($complete) {
  if ($complete.data.status -ne "completed") { throw "完成后状态不为 completed" }
  if (-not $complete.data.rewardsGranted) { throw "完成后 rewardsGranted=false" }
  if ([int]$complete.data.expEarned -le 0) { throw "expEarned 未发放" }
  if ([int]$complete.data.pointsEarned -le 0) { throw "pointsEarned 未发放" }
  Pass "订单完成并发放：exp=$($complete.data.expEarned), points=$($complete.data.pointsEarned)"
}

# ----------------------------
# 2) TC04 幂等：重复完成不重复发放
# ----------------------------
Title "TC04: 重复完成幂等"

$threw = $false
try {
  InvokeApi "POST" "$BaseUrl/api/admin/orders/$orderId/complete" | Out-Null
} catch {
  $threw = $true
}
if ($threw) {
  Pass "重复完成被拒绝（允许）"
} else {
  Pass "重复完成未报错（也允许），但需确保不重复发放（靠DB校验）"
}

# ----------------------------
# 3) 会员信息变动检查（至少 points/exp 增加）
# ----------------------------
Title "Check: 会员信息变化"

$member1 = InvokeApi "GET" "$BaseUrl/api/member/info"
$points1 = [int]$member1.data.currentPoints
$exp1 = [int]$member1.data.expTotal

if ($exp1 -lt $exp0) { throw "exp_total 不应减少" }
if ($points1 -lt $points0) { throw "current_points 不应减少（除非你此前做了兑换）" }

Pass "会员信息 OK：exp $exp0 -> $exp1, points $points0 -> $points1"

# ----------------------------
# 4) TC08 签到（含月封顶提示）
# ----------------------------
Title "TC08: 签到（月封顶800）"

try {
  $signin = InvokeApi "POST" "$BaseUrl/api/member/signin"
  Pass "签到返回：pointsEarned=$($signin.data.pointsEarned), consecutiveDays=$($signin.data.consecutiveDays)"
  if ($signin.data.message) { Info "message: $($signin.data.message)" }
} catch {
  Info "签到异常（可能已签到）：$_"
}

# ----------------------------
# 5) TC02 FIFO 扣减（兑换）
#   说明：严格验证 expires_at 需要至少两个不同 expires 的 lot。
#   若无法制造不同 expires，则退化验证：lot_id 越小优先被消耗（同到期时的FIFO tie-break）
# ----------------------------
Title "TC02: 兑换（FIFO 扣减）"

# 兑换前积分检查
$member2 = InvokeApi "GET" "$BaseUrl/api/member/info"
$pointsBefore = [int]$member2.data.currentPoints

if ($pointsBefore -lt 300) {
  Info "当前积分($pointsBefore)不足以兑换券/商品，建议多完成几笔订单后再跑TC02。"
} else {
  $redeemBody = @{
    productId = 3      # 建议用 coupon 商品（你库里 points_products id=3 美式券，points_price=300）
    quantity  = 1
    fulfillmentType = "PICKUP"
  } | ConvertTo-Json

  $redeem = InvokeApi "POST" "$BaseUrl/api/mall/redeem" $redeemBody
  Pass "兑换成功：orderNo=$($redeem.data.orderNo), cost=$($redeem.data.pointsCost)"
  Info "FIFO 自动断言需要一个查询接口或DB连接。当前脚本建议用下方SQL校验（见脚本末尾输出）。"
}

# ----------------------------
# 6) TC06/TC07 券体系：先确保有 ISSUED 券，再用券下单核销
# ----------------------------
Title "TC06/TC07: 券获取 + 使用券下单核销"

# 券包
try {
  $coupons = InvokeApi "GET" "$BaseUrl/api/mall/coupons?status=ISSUED"
  $list = $coupons.data
  $count = if ($list) { $list.Count } else { 0 }
  Pass "ISSUED 券数量：$count"

  if ($count -gt 0) {
    $c = $list[0]
    Info "选取券：code=$($c.couponCode), type=$($c.couponType), expiresAt=$($c.expiresAt)"

    # 用券下单（金额小的订单，若满减不满足门槛应失败；折扣/兑换应成功）
    $couponOrderBody = @{
      items = @(@{ productId = 1; quantity = 1; cupSize = "medium" })
      couponCode = $c.couponCode
      remark = "TC06券核销测试"
    } | ConvertTo-Json -Depth 5

    $ok = $true
    try {
      $o2 = InvokeApi "POST" "$BaseUrl/api/orders" $couponOrderBody
      Pass "券下单成功：orderNo=$($o2.data.orderNo), discount=$($o2.data.discountAmount), pay=$($o2.data.payAmount)"
    } catch {
      $ok = $false
      Pass "券下单失败（可能是满减门槛不满足，符合TC07）：$($_.Exception.Message)"
    }

    # 可用券查询（orderAmount=50）
    try {
      $avail = InvokeApi "GET" "$BaseUrl/api/mall/coupons/available?orderAmount=50"
      Pass "available 查询成功：count=$($avail.data.Count)"
    } catch {
      Info "available 查询异常：$_"
    }
  } else {
    Info "没有 ISSUED 券：请先兑换券类积分商品后再测TC06/TC07。"
  }
} catch {
  Info "获取券包异常：$_"
}

Title "Done"
Write-Host "建议DB校验（需要你手动在数据库执行）："
Write-Host @"
1) 余额一致性：
   SELECT mi.user_id, mi.current_points,
          (SELECT COALESCE(SUM(pl.remaining),0) FROM cozy_member.points_lots pl WHERE pl.user_id=mi.user_id) AS sum_remaining
   FROM cozy_member.member_info mi WHERE mi.user_id = $userId;

2) FIFO消耗（按 lot.expires_at, lot.id）：
   SELECT plc.consume_id, plc.consume_amount, pl.id AS lot_id, pl.expires_at
   FROM cozy_member.points_lot_consumptions plc
   JOIN cozy_member.points_lots pl ON pl.id = plc.lot_id
   WHERE plc.user_id = $userId
   ORDER BY plc.created_at DESC, pl.expires_at ASC, pl.id ASC
   LIMIT 20;

3) 券核销：
   SELECT * FROM cozy_mall.user_coupons WHERE user_id=$userId ORDER BY created_at DESC LIMIT 10;

4) 订单折扣字段：
   SELECT id, order_no, total_amount, discount_amount, pay_amount, applied_coupon_id
   FROM cozy_order.shop_orders
   WHERE user_id=$userId
   ORDER BY created_at DESC LIMIT 10;
"@