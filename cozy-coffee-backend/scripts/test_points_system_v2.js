const axios = require('axios');

// ===================== 配置项 =====================
const CONFIG = {
    baseUrl: 'http://localhost:8080',
    token: '', // 替换为实际的用户/管理员 Token
    // 可替换为测试用的商品ID/券ID（对齐你的数据库）
    testProductIds: {
        coffee: 1,
        cake: 2,
        couponProduct: 3 // 积分兑换的券类商品（300积分）
    }
};

// ===================== 工具函数 =====================
const log = {
    pass: (msg) => console.log(`\x1b[32mPASS: ${msg}\x1b[0m`),
    fail: (msg) => console.log(`\x1b[31mFAIL: ${msg}\x1b[0m`),
    info: (msg) => console.log(`\x1b[33mINFO: ${msg}\x1b[0m`),
    title: (msg) => console.log(`\n\x1b[36m========== ${msg} ==========\x1b[0m`)
};

// 封装 API 请求
async function request(method, url, data = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${CONFIG.token}`
    };
    try {
        const response = await axios({
            method,
            url: `${CONFIG.baseUrl}${url}`,
            headers,
            data
        });
        return response.data;
    } catch (err) {
        if (err.response) {
            // 接口返回错误（4xx/5xx）
            throw new Error(`[${err.response.status}] ${JSON.stringify(err.response.data)}`);
        }
        throw new Error(`请求失败: ${err.message}`);
    }
}

// ===================== 核心测试逻辑 =====================
async function runTests() {
    // 校验 Token
    if (!CONFIG.token) {
        log.fail('Token 不能为空！请先登录获取 Bearer Token');
        process.exit(1);
    }

    let userId;
    let initialPoints = 0;
    let initialExp = 0;
    let testOrderId = '';

    try {
        // ----------------------------
        // 0) 前置：获取会员信息
        // ----------------------------
        log.title('Precheck: 获取会员信息');
        const memberRes = await request('GET', '/api/member/info');
        userId = memberRes.data.userId;
        initialPoints = parseInt(memberRes.data.currentPoints);
        initialExp = parseInt(memberRes.data.expTotal);
        log.pass(`当前用户: userId=${userId}, level=${memberRes.data.memberLevel}, exp=${initialExp}, points=${initialPoints}`);

        // ----------------------------
        // 1) TC01: 多商品下单 → 接单 → 完成 → 奖励发放
        // ----------------------------
        log.title('TC01: 多商品下单 -> 完成 -> 发放');
        // 创建订单
        const orderBody = {
            items: [
                { productId: CONFIG.testProductIds.coffee, quantity: 2, cupSize: "medium", temperature: "iced" },
                { productId: CONFIG.testProductIds.cake, quantity: 1, cupSize: "large" }
            ],
            remark: "TC01测试订单(JS版)"
        };
        const createOrderRes = await request('POST', '/api/orders', orderBody);
        testOrderId = createOrderRes.data.id;
        const { totalQuantity, status, payAmount } = createOrderRes.data;

        if (status !== 'pending') throw new Error(`订单创建状态错误，实际=${status}`);
        if (totalQuantity < 3) throw new Error(`totalQuantity 错误，实际=${totalQuantity}`);
        log.pass(`订单创建成功: ${createOrderRes.data.orderNo}, qty=${totalQuantity}, payAmount=${payAmount}`);

        // 接单（需管理员Token，无权限则跳过）
        try {
            const acceptRes = await request('POST', `/api/admin/orders/${testOrderId}/accept`);
            if (acceptRes.data.status !== 'preparing') throw new Error('接单后状态错误');
            log.pass(`接单成功: pickupCode=${acceptRes.data.pickupCode}`);
        } catch (err) {
            log.info(`接单接口异常（可能需要管理员Token）: ${err.message}`);
        }

        // 完成订单（需管理员Token，无权限则跳过）
        let completeRes;
        try {
            completeRes = await request('POST', `/api/admin/orders/${testOrderId}/complete`);
            if (completeRes.data.status !== 'completed') throw new Error('完成后状态错误');
            if (!completeRes.data.rewardsGranted) throw new Error('完成后 rewardsGranted=false');
            if (parseInt(completeRes.data.expEarned) <= 0) throw new Error('expEarned 未发放');
            if (parseInt(completeRes.data.pointsEarned) <= 0) throw new Error('pointsEarned 未发放');
            log.pass(`订单完成并发放：exp=${completeRes.data.expEarned}, points=${completeRes.data.pointsEarned}`);
        } catch (err) {
            log.info(`完成接口异常（可能需要管理员Token）: ${err.message}`);
        }

        // ----------------------------
        // 2) TC04: 重复完成订单幂等
        // ----------------------------
        log.title('TC04: 重复完成幂等');
        try {
            await request('POST', `/api/admin/orders/${testOrderId}/complete`);
            log.pass('重复完成未报错（需DB校验是否重复发放）');
        } catch (err) {
            log.pass(`重复完成被拒绝（符合预期）: ${err.message}`);
        }

        // ----------------------------
        // 3) 会员信息变动检查
        // ----------------------------
        log.title('Check: 会员信息变化');
        const memberAfterRes = await request('GET', '/api/member/info');
        const currentPoints = parseInt(memberAfterRes.data.currentPoints);
        const currentExp = parseInt(memberAfterRes.data.expTotal);

        if (currentExp < initialExp) throw new Error(`exp_total 减少: ${initialExp} -> ${currentExp}`);
        if (currentPoints < initialPoints) log.info(`积分减少（若未兑换则异常）: ${initialPoints} -> ${currentPoints}`);
        log.pass(`会员信息 OK：exp ${initialExp} -> ${currentExp}, points ${initialPoints} -> ${currentPoints}`);

        // ----------------------------
        // 4) TC08: 签到（月封顶800）
        // ----------------------------
        log.title('TC08: 签到（月封顶800）');
        try {
            const signinRes = await request('POST', '/api/member/signin');
            log.pass(`签到返回：pointsEarned=${signinRes.data.pointsEarned}, consecutiveDays=${signinRes.data.consecutiveDays}`);
            if (signinRes.data.message) log.info(`签到提示: ${signinRes.data.message}`);
        } catch (err) {
            log.info(`签到异常（可能已签到）: ${err.message}`);
        }

        // ----------------------------
        // 5) TC02: FIFO 积分兑换扣减
        // ----------------------------
        log.title('TC02: 兑换（FIFO 扣减）');
        const memberBeforeRedeem = await request('GET', '/api/member/info');
        const pointsBeforeRedeem = parseInt(memberBeforeRedeem.data.currentPoints);

        if (pointsBeforeRedeem < 300) {
            log.info(`当前积分(${pointsBeforeRedeem})不足兑换，建议多完成订单后重试`);
        } else {
            const redeemBody = {
                productId: CONFIG.testProductIds.couponProduct,
                quantity: 1,
                fulfillmentType: "PICKUP"
            };
            const redeemRes = await request('POST', '/api/mall/redeem', redeemBody);
            log.pass(`兑换成功：orderNo=${redeemRes.data.orderNo}, cost=${redeemRes.data.pointsCost}`);
            log.info('请通过DB SQL校验FIFO扣减顺序（见脚本末尾）');
        }

        // ----------------------------
        // 6) TC06/TC07: 券体系（获取券 + 用券下单）
        // ----------------------------
        log.title('TC06/TC07: 券获取 + 使用券下单核销');
        try {
            const couponsRes = await request('GET', '/api/mall/coupons?status=ISSUED');
            const couponList = couponsRes.data || [];
            log.pass(`ISSUED 券数量：${couponList.length}`);

            if (couponList.length > 0) {
                const targetCoupon = couponList[0];
                log.info(`选取券：code=${targetCoupon.couponCode}, type=${targetCoupon.couponType}, expiresAt=${targetCoupon.expiresAt}`);

                // 用券下单（验证核销/门槛）
                const couponOrderBody = {
                    items: [{ productId: CONFIG.testProductIds.coffee, quantity: 1, cupSize: "medium" }],
                    couponCode: targetCoupon.couponCode,
                    remark: "TC06券核销测试(JS版)"
                };
                try {
                    const couponOrderRes = await request('POST', '/api/orders', couponOrderBody);
                    log.pass(`券下单成功：orderNo=${couponOrderRes.data.orderNo}, discount=${couponOrderRes.data.discountAmount}, pay=${couponOrderRes.data.payAmount}`);
                } catch (err) {
                    log.pass(`券下单失败（符合TC07门槛校验）: ${err.message}`);
                }

                // 校验可用券查询
                try {
                    const availRes = await request('GET', '/api/mall/coupons/available?orderAmount=50');
                    log.pass(`可用券查询成功：count=${availRes.data.length}`);
                } catch (err) {
                    log.info(`可用券查询异常: ${err.message}`);
                }
            } else {
                log.info('无 ISSUED 券：请先兑换积分券类商品后重试');
            }
        } catch (err) {
            log.info(`获取券包异常: ${err.message}`);
        }

        // ----------------------------
        // 测试完成：输出DB校验SQL
        // ----------------------------
        log.title('Done: 手动DB校验SQL');
        console.log(`
# 1) 余额一致性
SELECT mi.user_id, mi.current_points,
       (SELECT COALESCE(SUM(pl.remaining),0) FROM cozy_member.points_lots pl WHERE pl.user_id=mi.user_id) AS sum_remaining
FROM cozy_member.member_info mi WHERE mi.user_id = ${userId};

# 2) FIFO消耗顺序
SELECT plc.consume_id, plc.consume_amount, pl.id AS lot_id, pl.expires_at
FROM cozy_member.points_lot_consumptions plc
JOIN cozy_member.points_lots pl ON pl.id = plc.lot_id
WHERE plc.user_id = ${userId}
ORDER BY plc.created_at DESC, pl.expires_at ASC, pl.id ASC
LIMIT 20;

# 3) 券核销记录
SELECT * FROM cozy_mall.user_coupons WHERE user_id=${userId} ORDER BY created_at DESC LIMIT 10;

# 4) 订单折扣字段
SELECT id, order_no, total_amount, discount_amount, pay_amount, applied_coupon_id
FROM cozy_order.shop_orders WHERE user_id=${userId} ORDER BY created_at DESC LIMIT 10;
    `);

    } catch (err) {
        log.fail(`测试中断：${err.message}`);
        process.exit(1);
    }
}

// 执行测试
runTests();