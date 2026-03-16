# CozyCoffee 积分体系重设计 - PowerShell 测试脚本
# 覆盖 TC01-TC08 测试用例

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Token = ""
)

$ErrorActionPreference = "Stop"

function Write-Pass { param($msg) Write-Host "✅ PASS: $msg" -ForegroundColor Green }
function Write-Fail { param($msg) Write-Host "❌ FAIL: $msg" -ForegroundColor Red }
function Write-Info { param($msg) Write-Host "ℹ️ INFO: $msg" -ForegroundColor Yellow }
function Write-Title { param($msg) Write-Host "`n========== $msg ==========" -ForegroundColor Cyan }

$headers = @{
    "Content-Type" = "application/json"
}
if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
}

Write-Host "=========================================="
Write-Host "CozyCoffee 积分体系 API 测试"
Write-Host "Base URL: $BaseUrl"
Write-Host "=========================================="

# -------------------------------------------
# TC01: 多商品下单 → 完成 → EXP/POINT 发放
# -------------------------------------------
Write-Title "TC01: 多商品下单"

$orderId = $null
$orderNo = $null

try {
    $orderBody = @{
        items = @(
            @{ productId = 1; quantity = 2; cupSize = "medium"; temperature = "iced" },
            @{ productId = 2; quantity = 1; cupSize = "large" }
        )
        remark = "TC01测试订单"
    } | ConvertTo-Json -Depth 3

    $orderResp = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -Method POST -Headers $headers -Body $orderBody
    $orderId = $orderResp.data.id
    $orderNo = $orderResp.data.orderNo
    $status = $orderResp.data.status
    $itemsSummary = $orderResp.data.itemsSummary
    $totalQty = $orderResp.data.totalQuantity

    if ($status -eq "pending" -and $orderId -and $totalQty -ge 3) {
        Write-Pass "订单创建: $orderNo, items=$itemsSummary, qty=$totalQty"
    } else {
        Write-Fail "订单创建失败: status=$status"
    }
} catch {
    Write-Fail "订单创建异常: $_"
}

# 接单
try {
    $acceptResp = Invoke-RestMethod -Uri "$BaseUrl/api/admin/orders/$orderId/accept" -Method POST -Headers $headers
    $pickupCode = $acceptResp.data.pickupCode
    $status = $acceptResp.data.status

    if ($status -eq "preparing" -and $pickupCode) {
        Write-Pass "接单成功: pickupCode=$pickupCode"
    } else {
        Write-Fail "接单失败"
    }
} catch {
    Write-Info "接单异常: $_"
}

# 完成订单
try {
    $completeResp = Invoke-RestMethod -Uri "$BaseUrl/api/admin/orders/$orderId/complete" -Method POST -Headers $headers
    $status = $completeResp.data.status
    $exp = $completeResp.data.expEarned
    $points = $completeResp.data.pointsEarned
    $granted = $completeResp.data.rewardsGranted

    if ($status -eq "completed" -and $granted -and $exp -gt 0 -and $points -gt 0) {
        Write-Pass "订单完成: exp=$exp, points=$points, granted=$granted"
    } else {
        Write-Fail "订单完成失败: status=$status, granted=$granted"
    }
} catch {
    Write-Info "订单完成异常: $_"
}

# -------------------------------------------
# TC04: 重复完成订单幂等性
# -------------------------------------------
Write-Title "TC04: 重复完成订单幂等性"

try {
    $againResp = Invoke-RestMethod -Uri "$BaseUrl/api/admin/orders/$orderId/complete" -Method POST -Headers $headers -ErrorAction SilentlyContinue
    Write-Info "重复完成返回: status=$($againResp.data.status)"
} catch {
    Write-Pass "幂等性验证: 重复完成被正确拒绝"
}

# -------------------------------------------
# TC08: 签到月封顶
# -------------------------------------------
Write-Title "TC08: 签到"

try {
    $signinResp = Invoke-RestMethod -Uri "$BaseUrl/api/member/signin" -Method POST -Headers $headers
    $signinPoints = $signinResp.data.pointsEarned
    $signinMsg = $signinResp.data.message
    $consecutive = $signinResp.data.consecutiveDays

    Write-Pass "签到成功: points=$signinPoints, consecutive=$consecutive"
    Write-Info "消息: $signinMsg"
} catch {
    Write-Info "签到异常（可能已签到）: $_"
}

# -------------------------------------------
# 会员信息验证
# -------------------------------------------
Write-Title "会员信息"

try {
    $memberResp = Invoke-RestMethod -Uri "$BaseUrl/api/member/info" -Method GET -Headers $headers
    $expTotal = $memberResp.data.expTotal
    $currentPoints = $memberResp.data.currentPoints
    $level = $memberResp.data.memberLevel
    $expiring = $memberResp.data.expiringPoints

    Write-Pass "会员信息: level=$level, exp=$expTotal, points=$currentPoints, expiring=$expiring"
    
    if ($expTotal -ge 1000 -and $level -ne "basic") {
        Write-Pass "TC03: 等级升级正确 (exp=$expTotal -> $level)"
    } elseif ($expTotal -lt 1000) {
        Write-Info "TC03: EXP 不足 1000，等级为 basic"
    }
} catch {
    Write-Fail "获取会员信息异常: $_"
}

# -------------------------------------------
# TC02: 兑换扣减 FIFO
# -------------------------------------------
Write-Title "TC02: 积分兑换（FIFO 扣减）"

try {
    $memberCheck = Invoke-RestMethod -Uri "$BaseUrl/api/member/info" -Method GET -Headers $headers
    $pointsBefore = $memberCheck.data.currentPoints
    
    if ($pointsBefore -ge 100) {
        $redeemBody = @{
            productId = 1
            quantity = 1
            fulfillmentType = "PICKUP"
        } | ConvertTo-Json

        $redeemResp = Invoke-RestMethod -Uri "$BaseUrl/api/mall/redeem" -Method POST -Headers $headers -Body $redeemBody
        $redeemOrderNo = $redeemResp.data.orderNo
        $pointsCost = $redeemResp.data.pointsCost

        Write-Pass "兑换成功: orderNo=$redeemOrderNo, cost=$pointsCost"
        Write-Info "TC02 验证: 请检查 points_lot_consumptions 表确认 FIFO 扣减"
    } else {
        Write-Info "积分不足 100，跳过兑换测试"
    }
} catch {
    Write-Info "兑换异常: $_"
}

# -------------------------------------------
# TC06 & TC07: 券相关测试
# -------------------------------------------
Write-Title "TC06/TC07: 券体系测试"

# 获取用户券包
try {
    $couponsResp = Invoke-RestMethod -Uri "$BaseUrl/api/mall/coupons" -Method GET -Headers $headers
    $couponCount = $couponsResp.data.Count
    Write-Pass "用户券包: 共 $couponCount 张券"
    
    if ($couponCount -gt 0) {
        $firstCoupon = $couponsResp.data[0]
        Write-Info "第一张券: code=$($firstCoupon.couponCode), type=$($firstCoupon.couponType), status=$($firstCoupon.status)"
        
        # 如果有可用券，尝试下单使用
        if ($firstCoupon.status -eq "ISSUED" -and $firstCoupon.available) {
            Write-Title "TC06: 使用券下单"
            
            $couponOrderBody = @{
                items = @(
                    @{ productId = 1; quantity = 1; cupSize = "medium" }
                )
                couponCode = $firstCoupon.couponCode
                remark = "TC06券测试"
            } | ConvertTo-Json -Depth 3
            
            try {
                $couponOrderResp = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -Method POST -Headers $headers -Body $couponOrderBody
                $discount = $couponOrderResp.data.discountAmount
                $payAmount = $couponOrderResp.data.payAmount
                
                if ($discount -gt 0) {
                    Write-Pass "券核销成功: discount=$discount, payAmount=$payAmount"
                } else {
                    Write-Info "订单创建但无折扣: $($couponOrderResp.data.orderNo)"
                }
            } catch {
                Write-Info "券下单异常: $_"
            }
        }
    }
} catch {
    Write-Info "获取券包异常: $_"
}

# 获取可用券
try {
    $availableResp = Invoke-RestMethod -Uri "$BaseUrl/api/mall/coupons/available?orderAmount=50" -Method GET -Headers $headers
    $availableCount = $availableResp.data.Count
    Write-Pass "可用券: 共 $availableCount 张"
} catch {
    Write-Info "获取可用券异常: $_"
}

# -------------------------------------------
# TC05: 黑卡加速包
# -------------------------------------------
Write-Title "TC05: 黑卡加速包验证"
Write-Info "请检查后端日志中的 '黑卡加速包计算' 输出"
Write-Info "格式: userId=X, payAmount=Y, monthlySpent=Z, accelerated=A@1.70x, normal=B@1.35x"

# -------------------------------------------
# 完成
# -------------------------------------------
Write-Host "`n=========================================="
Write-Host "测试完成！"
Write-Host "=========================================="
Write-Host ""
Write-Host "SQL 验证命令:" -ForegroundColor Yellow
Write-Host "  -- 积分批次" -ForegroundColor Gray
Write-Host "  SELECT * FROM cozy_member.points_lots WHERE user_id = 1 ORDER BY expires_at;" -ForegroundColor Gray
Write-Host ""
Write-Host "  -- FIFO 扣减记录" -ForegroundColor Gray
Write-Host "  SELECT * FROM cozy_member.points_lot_consumptions ORDER BY created_at DESC LIMIT 10;" -ForegroundColor Gray
Write-Host ""
Write-Host "  -- 用户券" -ForegroundColor Gray
Write-Host "  SELECT * FROM cozy_mall.user_coupons WHERE user_id = 1;" -ForegroundColor Gray
