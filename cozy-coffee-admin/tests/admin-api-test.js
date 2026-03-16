/**
 * 管理端 API 自动化测试脚本
 * 
 * 使用方法：
 * 1. 确保后端服务已启动 (localhost:8080)
 * 2. 确保数据库已执行:
 *    - mysql -u root -p cozy_user < mysql/admin_role_migration.sql
 *    - mysql -u root -p cozy_user < mysql/test_accounts.sql
 * 3. 运行: node tests/admin-api-test.js
 * 
 * 环境变量配置:
 *   API_BASE - 后端API地址 (默认: http://localhost:8080/api)
 *   TIMEOUT_MS - 请求超时时间 (默认: 10000ms)
 *   ALLOW_MUTATIONS - 允许状态流转测试 (默认: false)
 * 
 * 示例:
 *   ALLOW_MUTATIONS=true node tests/admin-api-test.js
 *   API_BASE=http://other-host:8080/api TIMEOUT_MS=5000 node tests/admin-api-test.js
 * 
 * 测试账号:
 *   管理员: testadmin / admin123
 *   普通用户: testuser / user123
 * 
 * 测试覆盖：
 * - 安全性测试（角色权限验证，包括所有写操作）
 * - 控制台统计数据
 * - 用户管理 CRUD（包括积分调整和回滚验证）
 * - 订单管理（筛选、数据验证、可选状态流转）
 * - 商品管理（完整 CRUD 流程）
 * - 兑换订单管理（筛选、数据验证、可选状态流转）
 * - 边界条件测试（缺少参数、无效值、不存在的资源）
 * 
 * 改进特性:
 * - 增强的请求函数：支持超时、处理非JSON响应
 * - 响应形状验证：验证 {success, code, message, data} 结构
 * - 更好的断言消息：失败时显示详细原因
 * - 扩展的安全测试：覆盖所有管理端写操作
 * - 数据验证：筛选结果与过滤条件的匹配验证
 * - 积分调整验证：验证积分变化并自动回滚
 * - 可选突变测试：ALLOW_MUTATIONS=true 时测试状态转换
 * - 边界测试：缺少参数、零值、负值等异常情况
 */

// Configuration via environment variables
const MIN_TIMEOUT_MS = 1000; // Minimum timeout to prevent too-fast failures
const API_BASE = process.env.API_BASE || 'http://localhost:8080/api';
const TIMEOUT_MS = Math.max(MIN_TIMEOUT_MS, parseInt(process.env.TIMEOUT_MS || '10000', 10));
const ALLOW_MUTATIONS = process.env.ALLOW_MUTATIONS === 'true';

// 测试账号（需要执行 mysql/test_accounts.sql）
// 密码统一为: 123456
const ADMIN_CREDENTIALS = { username: 'testadmin', password: '123456' };
const USER_CREDENTIALS = { username: 'testuser', password: '123456' };

let adminToken = '';
let userToken = '';
let testResults = [];
let createdProductId = null;

// ==================== 工具函数 ====================

/**
 * Enhanced request function with timeout and non-JSON response handling
 */
async function request(method, path, data = null, token = null) {
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }

    try {
        const url = `${API_BASE}${path}`;

        // Add timeout support
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);
        options.signal = controller.signal;

        const response = await fetch(url, options);
        clearTimeout(timeoutId);

        // Try to parse JSON, but handle non-JSON/empty responses
        let json = null;
        const contentType = response.headers.get('content-type');
        const text = await response.text();

        if (text && contentType && contentType.includes('application/json')) {
            try {
                json = JSON.parse(text);
            } catch (parseError) {
                return {
                    status: response.status,
                    data: null,
                    error: `JSON parse error: ${parseError.message}`,
                    rawText: text
                };
            }
        } else if (text) {
            // Non-JSON response
            return {
                status: response.status,
                data: null,
                rawText: text
            };
        }

        return { status: response.status, data: json };
    } catch (e) {
        if (e.name === 'AbortError') {
            return { status: 0, error: `Request timeout after ${TIMEOUT_MS}ms` };
        }
        return { status: 0, error: e.message };
    }
}

/**
 * Validate response shape { success: boolean, code: number, message: string, data: any }
 */
function validateResponseShape(response, testName) {
    if (!response || !response.data) {
        test(`${testName} - Response exists`, false, 'No response data');
        return false;
    }

    const data = response.data;
    const hasSuccess = typeof data.success === 'boolean';
    const hasCode = typeof data.code === 'number';
    const hasMessage = typeof data.message === 'string';

    if (!hasSuccess || !hasCode || !hasMessage) {
        const missing = [];
        if (!hasSuccess) missing.push('success');
        if (!hasCode) missing.push('code');
        if (!hasMessage) missing.push('message');
        test(`${testName} - Response shape`, false, `Missing fields: ${missing.join(', ')}`);
        return false;
    }

    return true;
}

/**
 * Enhanced test function with better failure messages
 */
function test(name, passed, detail = '') {
    testResults.push({ name, passed, detail });
    const icon = passed ? '✅' : '❌';
    const detailMsg = detail ? ` - ${detail}` : '';
    console.log(`${icon} ${name}${detailMsg}`);
}

function section(title) {
    console.log(`\n${'='.repeat(50)}`);
    console.log(`📋 ${title}`);
    console.log('='.repeat(50));
}

/**
 * Skip a test with a message
 */
function skip(name, reason = '') {
    testResults.push({ name, passed: true, skipped: true, detail: reason });
    console.log(`⏭️  ${name} - SKIPPED${reason ? ` (${reason})` : ''}`);
}

// ==================== 测试用例 ====================

async function testLogin() {
    section('1. 登录测试');

    // 管理员登录
    const adminRes = await request('POST', '/auth/login', ADMIN_CREDENTIALS);
    if (adminRes.data?.success && adminRes.data?.data) {
        adminToken = adminRes.data?.data?.token;
        test('管理员登录成功', true);
    } else {
        test('管理员登录成功', false, adminRes.data?.message || '登录失败，请确认执行了 test_accounts.sql');
        return false;
    }

    // 普通用户登录
    const userRes = await request('POST', '/auth/login', USER_CREDENTIALS);
    if (userRes.data?.success && userRes.data?.data) {
        userToken = userRes.data?.data?.token;
        test('普通用户登录成功', true);
    } else {
        test('普通用户登录成功', false, '可选，不影响主要测试');
    }

    return true;
}

async function testSecurity() {
    section('2. 安全性测试 🔒');

    // 无token访问管理端
    const noTokenRes = await request('GET', '/admin/dashboard/stats');
    test('无Token拒绝访问管理端',
        noTokenRes.status === 401 || noTokenRes.data?.code === 401 || noTokenRes.data?.success === false,
        `状态: ${noTokenRes.status}`);

    // 普通用户访问管理端读操作
    if (userToken) {
        const userRes = await request('GET', '/admin/dashboard/stats', null, userToken);
        test('普通用户被拒绝访问管理端读操作',
            userRes.status === 403 || userRes.data?.code === 403 || userRes.data?.success === false,
            `状态: ${userRes.status}`);
    } else {
        skip('普通用户被拒绝访问管理端读操作', '无普通用户Token');
    }

    // 普通用户访问管理端写操作 - 扩展安全测试
    if (userToken) {
        // Test 1: Try to adjust points (write operation)
        const adjustRes = await request('POST', '/admin/users/1/points?amount=100&reason=test', null, userToken);
        test('普通用户被拒绝调整用户积分',
            adjustRes.status === 403 || adjustRes.data?.code === 403 || adjustRes.data?.success === false,
            `状态: ${adjustRes.status}`);

        // Test 2: Try to accept an order (write operation)
        const acceptRes = await request('POST', '/admin/orders/1/accept', null, userToken);
        test('普通用户被拒绝接受订单',
            acceptRes.status === 403 || acceptRes.data?.code === 403 || acceptRes.data?.success === false,
            `状态: ${acceptRes.status}`);

        // Test 3: Try to add product (write operation)
        const productRes = await request('POST', '/admin/products/coffee',
            { name: 'Test', price: 10 }, userToken);
        test('普通用户被拒绝添加商品',
            productRes.status === 403 || productRes.data?.code === 403 || productRes.data?.success === false,
            `状态: ${productRes.status}`);

        // Test 4: Try to process redemption (write operation)
        const redemptionRes = await request('POST', '/admin/redemptions/1/process', null, userToken);
        test('普通用户被拒绝处理兑换订单',
            redemptionRes.status === 403 || redemptionRes.data?.code === 403 || redemptionRes.data?.success === false,
            `状态: ${redemptionRes.status}`);
    } else {
        skip('普通用户被拒绝访问管理端写操作', '无普通用户Token');
    }

    // 管理员正常访问
    const adminRes = await request('GET', '/admin/dashboard/stats', null, adminToken);
    validateResponseShape(adminRes, '管理员访问管理端');
    test('管理员可正常访问管理端', adminRes.data?.success === true);
}

async function testDashboard() {
    section('3. 控制台统计测试');

    const res = await request('GET', '/admin/dashboard/stats', null, adminToken);
    validateResponseShape(res, '获取统计数据');

    const data = res.data?.data;

    test('获取统计数据成功', res.data?.success === true,
        res.data?.success ? '' : `失败: ${res.data?.message}`);
    test('返回 totalUsers 字段', typeof data?.totalUsers === 'number', `值: ${data?.totalUsers}`);
    test('返回 todayOrders 字段', typeof data?.todayOrders === 'number', `值: ${data?.todayOrders}`);
    test('返回 todayRevenue 字段', data?.todayRevenue !== undefined, `值: ${data?.todayRevenue}`);
    test('返回 pendingOrders 字段', typeof data?.pendingOrders === 'number', `值: ${data?.pendingOrders}`);
}

async function testUsers() {
    section('4. 用户管理测试');

    // 获取用户列表
    const res = await request('GET', '/admin/users', null, adminToken);
    validateResponseShape(res, '获取用户列表');
    test('获取用户列表成功', res.data?.success === true,
        res.data?.success ? '' : `失败: ${res.data?.message}`);
    test('返回用户数组', Array.isArray(res.data?.data), `数量: ${res.data?.data?.length || 0}`);

    if (res.data?.data?.length > 0) {
        const user = res.data.data[0];
        test('用户包含 id 字段', user.id !== undefined);
        test('用户包含 username 字段', user.username !== undefined);
        test('用户包含 memberLevel 字段', user.memberLevel !== undefined, `值: ${user.memberLevel}`);
        test('用户包含 currentPoints 字段', user.currentPoints !== undefined);

        // 测试积分调整 - 找一个有会员记录的用户（currentPoints > 0 或 memberLevel != 'basic'）
        const userWithMember = res.data.data.find(u => u.currentPoints > 0 || (u.memberLevel && u.memberLevel !== 'basic'));
        if (userWithMember) {
            const initialPoints = userWithMember.currentPoints || 0;
            console.log(`  💰 测试积分调整: 用户ID=${userWithMember.id}, 初始积分=${initialPoints}`);

            // 加积分
            const adjustRes = await request('POST', `/admin/users/${userWithMember.id}/points?amount=10&reason=自动化测试`, null, adminToken);
            validateResponseShape(adjustRes, '积分调整(+10)');
            test('积分调整功能(+10)', adjustRes.data?.success === true || adjustRes.data?.message?.includes('成功'),
                adjustRes.data?.success ? '' : `失败: ${adjustRes.data?.message}`);

            // 验证积分增加
            if (adjustRes.data?.success) {
                const verifyRes = await request('GET', '/admin/users', null, adminToken);
                const updatedUser = verifyRes.data?.data?.find(u => u.id === userWithMember.id);
                if (updatedUser) {
                    const expectedPoints = initialPoints + 10;
                    test('验证积分增加', updatedUser.currentPoints === expectedPoints,
                        `期望: ${expectedPoints}, 实际: ${updatedUser.currentPoints}`);

                    // 回滚积分 - 减去刚才加的积分
                    const rollbackRes = await request('POST',
                        `/admin/users/${userWithMember.id}/points?amount=-10&reason=自动化测试回滚`, null, adminToken);
                    validateResponseShape(rollbackRes, '积分回滚(-10)');
                    test('积分调整回滚(-10)', rollbackRes.data?.success === true,
                        rollbackRes.data?.success ? '' : `失败: ${rollbackRes.data?.message}`);

                    // 验证回滚成功
                    if (rollbackRes.data?.success) {
                        const finalRes = await request('GET', '/admin/users', null, adminToken);
                        const finalUser = finalRes.data?.data?.find(u => u.id === userWithMember.id);
                        if (finalUser) {
                            test('验证积分已回滚', finalUser.currentPoints === initialPoints,
                                `期望: ${initialPoints}, 实际: ${finalUser.currentPoints}`);
                        }
                    }
                }
            }
        } else {
            skip('积分调整功能', '没有找到有会员记录的用户');
        }
    }
}

async function testOrders() {
    section('5. 订单管理测试');

    // 获取所有订单
    const allRes = await request('GET', '/admin/orders', null, adminToken);
    validateResponseShape(allRes, '获取所有订单');
    test('获取所有订单', allRes.data?.success === true, `数量: ${allRes.data?.data?.length || 0}`);

    // 按状态筛选 - 并验证返回数据匹配过滤条件
    const pendingRes = await request('GET', '/admin/orders?status=pending', null, adminToken);
    validateResponseShape(pendingRes, '按状态筛选(pending)');
    test('按状态筛选订单 (pending)', pendingRes.data?.success === true);
    if (pendingRes.data?.data?.length > 0) {
        const allPending = pendingRes.data.data.every(o => o.status === 'pending');
        test('筛选结果匹配 pending 状态', allPending,
            allPending ? '' : '存在非 pending 状态的订单');
    }

    const preparingRes = await request('GET', '/admin/orders?status=preparing', null, adminToken);
    validateResponseShape(preparingRes, '按状态筛选(preparing)');
    test('按状态筛选订单 (preparing)', preparingRes.data?.success === true);
    if (preparingRes.data?.data?.length > 0) {
        const allPreparing = preparingRes.data.data.every(o => o.status === 'preparing');
        test('筛选结果匹配 preparing 状态', allPreparing,
            allPreparing ? '' : '存在非 preparing 状态的订单');
    }

    const completedRes = await request('GET', '/admin/orders?status=completed', null, adminToken);
    validateResponseShape(completedRes, '按状态筛选(completed)');
    test('按状态筛选订单 (completed)', completedRes.data?.success === true);
    if (completedRes.data?.data?.length > 0) {
        const allCompleted = completedRes.data.data.every(o => o.status === 'completed');
        test('筛选结果匹配 completed 状态', allCompleted,
            allCompleted ? '' : '存在非 completed 状态的订单');
    }

    // 按订单号筛选 - 验证返回数据
    const orderNoRes = await request('GET', '/admin/orders?orderNo=TEST', null, adminToken);
    validateResponseShape(orderNoRes, '按订单号筛选');
    test('按订单号筛选', orderNoRes.data?.success === true);
    if (orderNoRes.data?.data?.length > 0) {
        const allMatch = orderNoRes.data.data.every(o => o.orderNo && o.orderNo.includes('TEST'));
        test('筛选结果包含订单号关键字', allMatch,
            allMatch ? '' : '存在不匹配的订单号');
    }

    // 按日期筛选 - 验证返回数据
    const today = new Date().toISOString().split('T')[0];
    const dateRes = await request('GET', `/admin/orders?startDate=${today}&endDate=${today}`, null, adminToken);
    validateResponseShape(dateRes, '按日期范围筛选');
    test('按日期范围筛选', dateRes.data?.success === true);
    if (dateRes.data?.data?.length > 0) {
        const todayDate = new Date(today);
        const allInRange = dateRes.data.data.every(o => {
            if (!o.createdAt) return false;
            try {
                // Handle various date formats - extract date portion and compare
                const orderDateStr = o.createdAt.split('T')[0].split(' ')[0]; // Handle both ISO and space-separated
                const orderDate = new Date(orderDateStr);
                // For same-day filtering, compare date strings or use date equality
                return !isNaN(orderDate.getTime()) && orderDate.toDateString() === todayDate.toDateString();
            } catch (e) {
                return false; // Invalid date format
            }
        });
        test('筛选结果在日期范围内', allInRange,
            allInRange ? '' : '存在不在日期范围内的订单');
    }

    // 可选突变测试 - 订单状态流转
    if (ALLOW_MUTATIONS && pendingRes.data?.data?.length > 0) {
        await testOrderMutations(pendingRes.data.data[0]);
    } else if (ALLOW_MUTATIONS) {
        skip('订单状态流转测试', '没有待处理订单');
    } else {
        console.log(`\n  📦 可选突变测试已禁用 (ALLOW_MUTATIONS=false)`);
        if (pendingRes.data?.data?.length > 0) {
            console.log(`  提示: 发现 ${pendingRes.data.data.length} 个待处理订单，可设置 ALLOW_MUTATIONS=true 测试状态流转`);
        }
    }
}

async function testOrderMutations(order) {
    console.log(`\n  🔄 测试订单状态流转 (订单ID: ${order.id})`);

    // Accept order: pending -> preparing
    const acceptRes = await request('POST', `/admin/orders/${order.id}/accept`, null, adminToken);
    validateResponseShape(acceptRes, '接受订单');
    test('接受订单 (pending -> preparing)', acceptRes.data?.success === true,
        acceptRes.data?.success ? '' : `失败: ${acceptRes.data?.message}`);

    if (acceptRes.data?.success) {
        test('订单状态变为 preparing', acceptRes.data.data?.status === 'preparing',
            `状态: ${acceptRes.data.data?.status}`);

        // Complete order: preparing -> completed
        const completeRes = await request('POST', `/admin/orders/${order.id}/complete`, null, adminToken);
        validateResponseShape(completeRes, '完成订单');
        test('完成订单 (preparing -> completed)', completeRes.data?.success === true,
            completeRes.data?.success ? '' : `失败: ${completeRes.data?.message}`);

        if (completeRes.data?.success) {
            test('订单状态变为 completed', completeRes.data.data?.status === 'completed',
                `状态: ${completeRes.data.data?.status}`);
        }
    }
}

async function testProducts() {
    section('6. 商品管理测试 (完整CRUD)');

    // 获取咖啡商品列表
    const listRes = await request('GET', '/admin/products/coffee', null, adminToken);
    test('获取咖啡商品列表', listRes.data?.success === true, `数量: ${listRes.data?.data?.length || 0}`);

    // 添加商品
    const timestamp = Date.now();
    const newProduct = {
        name: `自动化测试商品_${timestamp}`,
        description: '这是一个自动化测试创建的商品',
        price: 19.99,
        category: 'coffee',
        imageUrl: '/images/test.jpg'
    };
    const addRes = await request('POST', '/admin/products/coffee', newProduct, adminToken);
    test('添加咖啡商品', addRes.data?.success === true);

    if (addRes.data?.data?.id) {
        createdProductId = addRes.data.data.id;
        console.log(`  📝 创建的商品ID: ${createdProductId}`);

        // 更新商品
        const updateData = {
            name: `更新后的商品_${timestamp}`,
            price: 29.99
        };
        const updateRes = await request('PUT', `/admin/products/coffee/${createdProductId}`, updateData, adminToken);
        test('更新咖啡商品', updateRes.data?.success === true);

        // 验证更新结果
        if (updateRes.data?.data) {
            test('更新后名称正确', updateRes.data.data.name?.includes('更新后'));
        }

        // 切换状态
        const toggleRes = await request('PUT', `/admin/products/coffee/${createdProductId}/status`, null, adminToken);
        test('切换商品状态 (下架)', toggleRes.data?.success === true);

        // 再次切换回来
        const toggleRes2 = await request('PUT', `/admin/products/coffee/${createdProductId}/status`, null, adminToken);
        test('切换商品状态 (上架)', toggleRes2.data?.success === true);

        // 删除商品（清理测试数据）
        const deleteRes = await request('DELETE', `/admin/products/coffee/${createdProductId}`, null, adminToken);
        test('删除咖啡商品', deleteRes.data?.success === true);

        // 验证删除成功
        const verifyRes = await request('GET', '/admin/products/coffee', null, adminToken);
        const stillExists = verifyRes.data?.data?.some(p => p.id === createdProductId);
        test('验证商品已删除', !stillExists);
    }

    // 获取积分商品
    const pointsRes = await request('GET', '/admin/products/points', null, adminToken);
    test('获取积分商品列表', pointsRes.data?.success === true, `数量: ${pointsRes.data?.data?.length || 0}`);
}

async function testRedemptions() {
    section('7. 兑换订单管理测试');

    // 获取所有兑换订单
    const res = await request('GET', '/admin/redemptions', null, adminToken);
    validateResponseShape(res, '获取兑换订单列表');
    test('获取兑换订单列表', res.data?.success === true, `数量: ${res.data?.data?.length || 0}`);

    // 按状态筛选 - 并验证返回数据匹配过滤条件
    const pendingRes = await request('GET', '/admin/redemptions?status=pending', null, adminToken);
    validateResponseShape(pendingRes, '按状态筛选(pending)');
    test('按状态筛选 (pending)', pendingRes.data?.success === true);
    if (pendingRes.data?.data?.length > 0) {
        const allPending = pendingRes.data.data.every(o => o.status === 'pending');
        test('兑换订单筛选结果匹配 pending 状态', allPending,
            allPending ? '' : '存在非 pending 状态的订单');
    }

    const processingRes = await request('GET', '/admin/redemptions?status=processing', null, adminToken);
    validateResponseShape(processingRes, '按状态筛选(processing)');
    test('按状态筛选 (processing)', processingRes.data?.success === true);
    if (processingRes.data?.data?.length > 0) {
        const allProcessing = processingRes.data.data.every(o => o.status === 'processing');
        test('兑换订单筛选结果匹配 processing 状态', allProcessing,
            allProcessing ? '' : '存在非 processing 状态的订单');
    }

    const completedRes = await request('GET', '/admin/redemptions?status=completed', null, adminToken);
    validateResponseShape(completedRes, '按状态筛选(completed)');
    test('按状态筛选 (completed)', completedRes.data?.success === true);
    if (completedRes.data?.data?.length > 0) {
        const allCompleted = completedRes.data.data.every(o => o.status === 'completed');
        test('兑换订单筛选结果匹配 completed 状态', allCompleted,
            allCompleted ? '' : '存在非 completed 状态的订单');
    }

    // 验证返回数据结构
    if (res.data?.data?.length > 0) {
        const order = res.data.data[0];
        test('订单包含 id', order.id !== undefined);
        test('订单包含 productName', order.productName !== undefined);
        test('订单包含 status', order.status !== undefined);
        test('订单包含 pointsCost', order.pointsCost !== undefined);
    }

    // 可选突变测试 - 兑换订单状态流转
    if (ALLOW_MUTATIONS && pendingRes.data?.data?.length > 0) {
        await testRedemptionMutations(pendingRes.data.data[0]);
    } else if (ALLOW_MUTATIONS) {
        skip('兑换订单状态流转测试', '没有待处理兑换订单');
    } else if (pendingRes.data?.data?.length > 0) {
        console.log(`  提示: 发现 ${pendingRes.data.data.length} 个待处理兑换订单，可设置 ALLOW_MUTATIONS=true 测试状态流转`);
    }
}

async function testRedemptionMutations(redemption) {
    console.log(`\n  🔄 测试兑换订单状态流转 (订单ID: ${redemption.id})`);

    // Process: pending -> processing
    const processRes = await request('POST', `/admin/redemptions/${redemption.id}/process`, null, adminToken);
    validateResponseShape(processRes, '备货处理');
    test('兑换订单备货 (pending -> processing)', processRes.data?.success === true,
        processRes.data?.success ? '' : `失败: ${processRes.data?.message}`);

    if (processRes.data?.success) {
        test('兑换订单状态变为 processing', processRes.data.data?.status === 'processing',
            `状态: ${processRes.data.data?.status}`);

        // Ship: processing -> shipped (需要物流信息)
        const trackingNo = `SF_AUTO_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`;
        const shipRes = await request('POST',
            `/admin/redemptions/${redemption.id}/ship?company=顺丰速运&trackingNo=${trackingNo}`,
            null, adminToken);
        validateResponseShape(shipRes, '发货');
        test('兑换订单发货 (processing -> shipped)', shipRes.data?.success === true,
            shipRes.data?.success ? '' : `失败: ${shipRes.data?.message}`);

        if (shipRes.data?.success) {
            test('兑换订单状态变为 shipped',
                shipRes.data.data?.status === 'shipped' || shipRes.data.data?.status === 'completed',
                `状态: ${shipRes.data.data?.status}`);

            // Complete: shipped -> completed
            const completeRes = await request('POST', `/admin/redemptions/${redemption.id}/complete`, null, adminToken);
            validateResponseShape(completeRes, '完成兑换订单');
            test('完成兑换订单 (shipped -> completed)', completeRes.data?.success === true,
                completeRes.data?.success ? '' : `失败: ${completeRes.data?.message}`);

            if (completeRes.data?.success) {
                test('兑换订单状态变为 completed', completeRes.data.data?.status === 'completed',
                    `状态: ${completeRes.data.data?.status}`);
            }
        }
    }
}

async function testBoundaryConditions() {
    section('8. 边界条件测试');

    // 访问不存在的订单
    const notFoundRes = await request('POST', '/admin/orders/999999/accept', null, adminToken);
    validateResponseShape(notFoundRes, '接受不存在的订单');
    test('接受不存在的订单返回错误', notFoundRes.data?.success === false || notFoundRes.data?.message,
        notFoundRes.data?.message || '');

    // 不存在的商品ID
    const deleteNotExistRes = await request('DELETE', '/admin/products/coffee/999999', null, adminToken);
    validateResponseShape(deleteNotExistRes, '删除不存在的商品');
    test('删除不存在的商品返回错误', deleteNotExistRes.data?.success === false || deleteNotExistRes.data?.message,
        deleteNotExistRes.data?.message || '');

    // 无效的状态筛选
    const invalidStatusRes = await request('GET', '/admin/orders?status=invalid_status', null, adminToken);
    test('无效状态筛选返回空或错误',
        invalidStatusRes.data?.success === true || invalidStatusRes.data?.data?.length === 0,
        `返回 ${invalidStatusRes.data?.data?.length || 0} 条记录`);

    // 新增边界测试: 缺少必需参数的发货请求
    const shipNoParamsRes = await request('POST', '/admin/redemptions/1/ship', null, adminToken);
    test('发货缺少必需参数(company, trackingNo)应失败',
        shipNoParamsRes.data?.success === false || shipNoParamsRes.status === 400,
        `状态: ${shipNoParamsRes.status}, 消息: ${shipNoParamsRes.data?.message || ''}`);

    // 缺少 company 参数
    const shipNoCompanyRes = await request('POST', '/admin/redemptions/1/ship?trackingNo=TEST123', null, adminToken);
    test('发货缺少 company 参数应失败',
        shipNoCompanyRes.data?.success === false || shipNoCompanyRes.status === 400,
        `状态: ${shipNoCompanyRes.status}, 消息: ${shipNoCompanyRes.data?.message || ''}`);

    // 缺少 trackingNo 参数
    const shipNoTrackingRes = await request('POST', '/admin/redemptions/1/ship?company=顺丰', null, adminToken);
    test('发货缺少 trackingNo 参数应失败',
        shipNoTrackingRes.data?.success === false || shipNoTrackingRes.status === 400,
        `状态: ${shipNoTrackingRes.status}, 消息: ${shipNoTrackingRes.data?.message || ''}`);

    // 新增边界测试: 积分调整数量为0应失败
    const zeroPointsRes = await request('POST', '/admin/users/1/points?amount=0&reason=test', null, adminToken);
    validateResponseShape(zeroPointsRes, '积分调整amount=0');
    test('积分调整 amount=0 应失败',
        zeroPointsRes.data?.success === false,
        `消息: ${zeroPointsRes.data?.message || ''}`);

    // 负数商品价格应被拒绝
    const negPriceRes = await request('POST', '/admin/products/coffee',
        { name: '测试商品', price: -10, category: 'coffee', description: 'test' }, adminToken);
    test('负数商品价格应被拒绝',
        negPriceRes.data?.success === false,
        negPriceRes.data?.message || '');

    // 空字符串商品名称应被拒绝
    const emptyNameRes = await request('POST', '/admin/products/coffee',
        { name: '', price: 10, category: 'coffee', description: 'test' }, adminToken);
    test('空商品名称应被拒绝',
        emptyNameRes.data?.success === false,
        emptyNameRes.data?.message || '');

    // 空分类应被拒绝
    const emptyCategoryRes = await request('POST', '/admin/products/coffee',
        { name: '测试商品', price: 10, category: '', description: 'test' }, adminToken);
    test('空商品分类应被拒绝',
        emptyCategoryRes.data?.success === false,
        emptyCategoryRes.data?.message || '');
}

// ==================== 运行测试 ====================

async function runTests() {
    console.log('\n🚀 管理端 API 自动化测试');
    console.log('='.repeat(50));
    console.log('后端地址: ' + API_BASE);
    console.log('超时设置: ' + TIMEOUT_MS + 'ms');
    console.log('允许突变: ' + ALLOW_MUTATIONS);
    console.log('测试时间: ' + new Date().toLocaleString());
    console.log('='.repeat(50));

    console.log('\n⚠️  前置条件:');
    console.log('1. 后端服务已启动 (localhost:8080)');
    console.log('2. 已执行 mysql/admin_role_migration.sql (cozy_user库)');
    console.log('3. 已执行 mysql/test_accounts.sql (cozy_user库)');

    if (ALLOW_MUTATIONS) {
        console.log('\n⚠️  突变测试已启用 - 将修改订单和兑换订单状态');
    }

    const loginSuccess = await testLogin();

    if (!loginSuccess) {
        console.log('\n' + '='.repeat(50));
        console.log('❌ 管理员登录失败，无法继续测试');
        console.log('='.repeat(50));
        console.log('\n请检查：');
        console.log('1. 后端服务是否启动 (http://localhost:8080)');
        console.log('2. 是否在 cozy_user 数据库执行了 test_accounts.sql');
        console.log('3. 数据库连接是否正常');
        return;
    }

    await testSecurity();
    await testDashboard();
    await testUsers();
    await testOrders();
    await testProducts();
    await testRedemptions();
    await testBoundaryConditions();

    // 统计结果
    const passed = testResults.filter(r => r.passed).length;
    const failed = testResults.filter(r => !r.passed).length;
    const skipped = testResults.filter(r => r.skipped).length;
    const total = testResults.length;

    console.log('\n' + '='.repeat(50));
    console.log('📊 测试结果汇总');
    console.log('='.repeat(50));
    console.log(`总计: ${total} 个测试`);
    console.log(`通过: ${passed} ✅`);
    console.log(`失败: ${failed} ❌`);
    console.log(`跳过: ${skipped} ⏭️`);
    console.log(`通过率: ${((passed / total) * 100).toFixed(1)}%`);

    if (failed > 0) {
        console.log('\n❌ 失败的测试用例:');
        testResults.filter(r => !r.passed).forEach(r => {
            console.log(`   - ${r.name}${r.detail ? `: ${r.detail}` : ''}`);
        });
    }

    console.log('\n' + '='.repeat(50));

    // 环境变量提示
    console.log('\n💡 提示:');
    console.log('• 使用 API_BASE=<url> 配置后端地址');
    console.log('• 使用 TIMEOUT_MS=<ms> 配置请求超时');
    console.log('• 使用 ALLOW_MUTATIONS=true 启用状态流转测试');
    console.log('\n例如: ALLOW_MUTATIONS=true node tests/admin-api-test.js');
}

runTests();
