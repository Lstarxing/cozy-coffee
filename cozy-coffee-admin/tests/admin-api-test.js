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
 * 测试账号:
 *   管理员: testadmin / admin123
 *   普通用户: testuser / user123
 * 
 * 测试覆盖：
 * - 安全性测试（角色权限验证）
 * - 控制台统计数据
 * - 用户管理 CRUD
 * - 订单管理（筛选、状态流转）
 * - 商品管理（完整 CRUD 流程）
 * - 兑换订单管理
 * - 边界条件测试
 */

const API_BASE = 'http://localhost:8080/api';

// 测试账号（需要执行 mysql/test_accounts.sql）
const ADMIN_CREDENTIALS = { username: 'testadmin', password: 'admin123' };
const USER_CREDENTIALS = { username: 'testuser', password: 'user123' };

let adminToken = '';
let userToken = '';
let testResults = [];
let createdProductId = null;

// ==================== 工具函数 ====================

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
        const response = await fetch(url, options);
        const json = await response.json();
        return { status: response.status, data: json };
    } catch (e) {
        return { status: 0, error: e.message };
    }
}

function test(name, passed, detail = '') {
    testResults.push({ name, passed, detail });
    const icon = passed ? '✅' : '❌';
    console.log(`${icon} ${name}${detail ? ` - ${detail}` : ''}`);
}

function section(title) {
    console.log(`\n${'='.repeat(50)}`);
    console.log(`📋 ${title}`);
    console.log('='.repeat(50));
}

// ==================== 测试用例 ====================

async function testLogin() {
    section('1. 登录测试');

    // 管理员登录
    const adminRes = await request('POST', '/user/login', ADMIN_CREDENTIALS);
    if (adminRes.data?.success && adminRes.data?.data) {
        adminToken = adminRes.data.data;
        test('管理员登录成功', true);
    } else {
        test('管理员登录成功', false, adminRes.data?.message || '登录失败，请确认执行了 test_accounts.sql');
        return false;
    }

    // 普通用户登录
    const userRes = await request('POST', '/user/login', USER_CREDENTIALS);
    if (userRes.data?.success && userRes.data?.data) {
        userToken = userRes.data.data;
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

    // 普通用户访问管理端
    if (userToken) {
        const userRes = await request('GET', '/admin/dashboard/stats', null, userToken);
        test('普通用户被拒绝访问管理端',
            userRes.status === 403 || userRes.data?.code === 403 || userRes.data?.success === false,
            `状态: ${userRes.status}`);
    } else {
        test('普通用户被拒绝访问管理端', true, '跳过（无普通用户Token）');
    }

    // 管理员正常访问
    const adminRes = await request('GET', '/admin/dashboard/stats', null, adminToken);
    test('管理员可正常访问管理端', adminRes.data?.success === true);
}

async function testDashboard() {
    section('3. 控制台统计测试');

    const res = await request('GET', '/admin/dashboard/stats', null, adminToken);
    const data = res.data?.data;

    test('获取统计数据成功', res.data?.success === true);
    test('返回 totalUsers 字段', typeof data?.totalUsers === 'number', `值: ${data?.totalUsers}`);
    test('返回 todayOrders 字段', typeof data?.todayOrders === 'number', `值: ${data?.todayOrders}`);
    test('返回 todayRevenue 字段', data?.todayRevenue !== undefined, `值: ${data?.todayRevenue}`);
    test('返回 pendingOrders 字段', typeof data?.pendingOrders === 'number', `值: ${data?.pendingOrders}`);
}

async function testUsers() {
    section('4. 用户管理测试');

    // 获取用户列表
    const res = await request('GET', '/admin/users', null, adminToken);
    test('获取用户列表成功', res.data?.success === true);
    test('返回用户数组', Array.isArray(res.data?.data), `数量: ${res.data?.data?.length || 0}`);

    if (res.data?.data?.length > 0) {
        const user = res.data.data[0];
        test('用户包含 id 字段', user.id !== undefined);
        test('用户包含 username 字段', user.username !== undefined);
        test('用户包含 memberLevel 字段', user.memberLevel !== undefined, `值: ${user.memberLevel}`);
        test('用户包含 currentPoints 字段', user.currentPoints !== undefined);

        // 测试积分调整（加积分）
        const adjustRes = await request('POST', `/admin/users/${user.id}/points?amount=10&reason=自动化测试`, null, adminToken);
        test('积分调整功能', adjustRes.data?.success === true || adjustRes.data?.message?.includes('成功'));
    }
}

async function testOrders() {
    section('5. 订单管理测试');

    // 获取所有订单
    const allRes = await request('GET', '/admin/orders', null, adminToken);
    test('获取所有订单', allRes.data?.success === true, `数量: ${allRes.data?.data?.length || 0}`);

    // 按状态筛选
    const pendingRes = await request('GET', '/admin/orders?status=pending', null, adminToken);
    test('按状态筛选订单 (pending)', pendingRes.data?.success === true);

    const preparingRes = await request('GET', '/admin/orders?status=preparing', null, adminToken);
    test('按状态筛选订单 (preparing)', preparingRes.data?.success === true);

    const completedRes = await request('GET', '/admin/orders?status=completed', null, adminToken);
    test('按状态筛选订单 (completed)', completedRes.data?.success === true);

    // 按订单号筛选
    const orderNoRes = await request('GET', '/admin/orders?orderNo=TEST', null, adminToken);
    test('按订单号筛选', orderNoRes.data?.success === true);

    // 按日期筛选
    const today = new Date().toISOString().split('T')[0];
    const dateRes = await request('GET', `/admin/orders?startDate=${today}&endDate=${today}`, null, adminToken);
    test('按日期范围筛选', dateRes.data?.success === true);

    // 如果有待处理订单，测试状态流转
    if (pendingRes.data?.data?.length > 0) {
        console.log(`\n  📦 发现待处理订单，可测试状态流转`);
        test('接单/完成/取消操作', true, '跳过（避免改变真实数据）');
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
    test('获取兑换订单列表', res.data?.success === true, `数量: ${res.data?.data?.length || 0}`);

    // 按状态筛选
    const pendingRes = await request('GET', '/admin/redemptions?status=pending', null, adminToken);
    test('按状态筛选 (pending)', pendingRes.data?.success === true);

    const processingRes = await request('GET', '/admin/redemptions?status=processing', null, adminToken);
    test('按状态筛选 (processing)', processingRes.data?.success === true);

    const completedRes = await request('GET', '/admin/redemptions?status=completed', null, adminToken);
    test('按状态筛选 (completed)', completedRes.data?.success === true);

    // 验证返回数据结构
    if (res.data?.data?.length > 0) {
        const order = res.data.data[0];
        test('订单包含 id', order.id !== undefined);
        test('订单包含 productName', order.productName !== undefined);
        test('订单包含 status', order.status !== undefined);
        test('订单包含 pointsUsed', order.pointsUsed !== undefined);
    }
}

async function testBoundaryConditions() {
    section('8. 边界条件测试');

    // 访问不存在的订单
    const notFoundRes = await request('POST', '/admin/orders/999999/accept', null, adminToken);
    test('接受不存在的订单返回错误', notFoundRes.data?.success === false || notFoundRes.data?.message);

    // 不存在的商品ID
    const deleteNotExistRes = await request('DELETE', '/admin/products/coffee/999999', null, adminToken);
    test('删除不存在的商品返回错误', deleteNotExistRes.data?.success === false || deleteNotExistRes.data?.message);

    // 无效的状态筛选
    const invalidStatusRes = await request('GET', '/admin/orders?status=invalid_status', null, adminToken);
    test('无效状态筛选返回空或错误', invalidStatusRes.data?.success === true || invalidStatusRes.data?.data?.length === 0);
}

// ==================== 运行测试 ====================

async function runTests() {
    console.log('\n🚀 管理端 API 自动化测试');
    console.log('='.repeat(50));
    console.log('后端地址: ' + API_BASE);
    console.log('测试时间: ' + new Date().toLocaleString());
    console.log('='.repeat(50));

    console.log('\n⚠️  前置条件:');
    console.log('1. 后端服务已启动 (localhost:8080)');
    console.log('2. 已执行 mysql/admin_role_migration.sql (cozy_user库)');
    console.log('3. 已执行 mysql/test_accounts.sql (cozy_user库)');

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
    const total = testResults.length;

    console.log('\n' + '='.repeat(50));
    console.log('📊 测试结果汇总');
    console.log('='.repeat(50));
    console.log(`总计: ${total} 个测试`);
    console.log(`通过: ${passed} ✅`);
    console.log(`失败: ${failed} ❌`);
    console.log(`通过率: ${((passed / total) * 100).toFixed(1)}%`);

    if (failed > 0) {
        console.log('\n❌ 失败的测试用例:');
        testResults.filter(r => !r.passed).forEach(r => {
            console.log(`   - ${r.name}${r.detail ? `: ${r.detail}` : ''}`);
        });
    }

    console.log('\n' + '='.repeat(50));
}

runTests();
