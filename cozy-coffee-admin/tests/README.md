# 管理端测试指南

## 快速开始

### 1. 前置准备（只需执行一次）

数据库表与种子数据由各 Provider 启动时的 **Flyway 自动迁移**，**无需手工执行 SQL**。

唯一要手动做的是准备两个测试账号 —— 脚本里账号是**写死的**，所以必须同名同密码：

| 角色 | 账号 | 密码 | 用途 |
|---|---|---|---|
| 管理员 | `testadmin` | `123456` | 正常访问 /api/admin/* |
| 普通用户 | `testuser` | `123456` | 验证权限拦截 |

```bash
# 先在前端注册这两个账号，再把 testadmin 提升为管理员（users 表在 cozy_user 库）
docker exec -i cozy-mysql mysql -uroot -p123456 -e \
  "UPDATE cozy_user.users SET role='admin' WHERE email='<testadmin 注册用的邮箱>';"
```

不改账号的话就照上表造；想用别的账号，改 `tests/admin-api-test.js` 里的 `ADMIN_CREDENTIALS` / `USER_CREDENTIALS` 常量。

> ⚠️ 早期文档里的 `cozy_coffee` 库、`mysql/admin_role_migration.sql`、`mysql/test_accounts.sql` **都已不存在**，
> 脚本头部注释里那两条 `mysql -u root -p cozy_coffee < ...` 命令也已失效（注释里的密码 `admin123`/`user123` 同样与代码不符）。

### 2. 启动服务

```bash
# 后端：在仓库根目录起基础设施（注意要含 minio），再用 IDE 启动 5 个微服务
docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker minio

# 前端管理端
cd cozy-coffee-admin
npm run dev
```

### 3. 运行 API 自动化测试

```bash
cd cozy-coffee-admin
node tests/admin-api-test.js
```

---

## 测试类型说明

### API 自动化测试 ✅ 无需人工参与

位置: `cozy-coffee-admin/tests/admin-api-test.js`

**覆盖内容：**
- 登录验证
- 安全性（角色权限）
- 控制台统计数据
- 用户管理 CRUD
- 订单管理（筛选、操作）
- 商品管理（完整 CRUD 流程）
- 兑换订单管理

**运行方式：**
```bash
node tests/admin-api-test.js
```

**输出示例：**
```
🚀 开始管理端 API 自动化测试

📋 登录测试
✅ 管理员登录
✅ 普通用户登录

🔒 安全性测试
✅ 无token拒绝访问
✅ 普通用户拒绝访问管理端
✅ 管理员可访问管理端

📊 控制台测试
✅ 获取统计数据
...

==================================================
📊 测试结果: 25 通过 / 0 失败
==================================================
```

### 手动 UI 测试 ⚠️ 需要人工参与

某些场景需要人工验证：

| 场景 | 原因 |
|------|------|
| 界面布局和样式 | 需要视觉确认 |
| 复杂交互流程 | 如对话框、拖拽等 |
| 实时数据更新 | 如取餐码生成后显示 |

---

## 测试检查清单

### 安全性 🔒
- [ ] 普通用户无法访问 /api/admin/* 接口
- [ ] 用户A无法取消用户B的订单
- [ ] 管理员可以正常登录和操作

### 控制台 📊
- [ ] 统计数据显示正确
- [ ] 最近订单按时间排序

### 用户管理 👥
- [ ] 列表正常加载
- [ ] 搜索功能正常
- [ ] 积分调整功能正常

### 订单管理 📦
- [ ] 全部/待处理/制作中/已完成 切换正常
- [ ] 订单号搜索正常
- [ ] 日期范围筛选正常
- [ ] 接单生成取餐码
- [ ] 完成和取消订单

### 商品管理 ☕
- [ ] 咖啡商品列表（含下架）
- [ ] 添加新商品
- [ ] 编辑商品信息
- [ ] 切换上下架状态
- [ ] 删除商品

### 兑换管理 🎁
- [ ] 订单列表加载
- [ ] 处理（备货）操作
- [ ] 发货（填写物流）
- [ ] 完成订单

---

## 常见问题

### Q: 测试脚本报错 "管理员登录失败"
A: 检查：
1. 后端服务是否启动（默认 `http://localhost:8080/api`）
2. 是否已注册 `testadmin` / `testuser` 两个账号（账号写死在脚本第 81~82 行）
3. `testadmin` 的 `role` 是否为 `'admin'`（`cozy_user.users` 表）

### Q: 安全性测试失败
A: 检查：
1. `testadmin` 的 role 是否已改为 `admin`
2. Gateway 是否加载了 `AdminAuthInterceptor`（见 `cozy-gateway` 的 `WebConfig`）
3. 重新编译并重启后端服务

### Q: 商品CRUD测试失败
A: 检查：
1. Order Provider 是否正常启动（商品接口走 order 域）
2. Gateway 的管理端控制器是否齐全：`AdminOrderController` / `AdminUserController` / `AdminMallController` / `AdminDashboardController`
3. 接口返回的 DTO 字段是否与前端一致（如 `CoffeeProductDTO`）
