# CozyCoffee Admin

CozyCoffee 运营管理后台，基于 Vue 3、Vite、Pinia、Element Plus 与 ECharts。面向门店运营与客服，覆盖：

- 运营看板（营业额/订单趋势、状态分布、商品排行，ECharts）
- 实时订单（SSE 推送新订单与状态变更，接单生成取餐码、完成/取消）
- 会员管理（用户列表与搜索、详情、积分调整）
- 商品管理（咖啡商品 / 积分商品 / 内容档案三页，含上下架与内容录入）
- 兑换管理（积分兑换订单的备货、发货填物流、完成）

## 项目结构

```text
cozy-coffee-admin/
├─ public/                     静态资源
├─ tests/
│  ├─ admin-api-test.js        管理端 API 自动化测试（Node 直跑）
│  ├─ unit/order.test.js       前端单测（Vitest）
│  └─ README.md                测试指南
├─ src/
│  ├─ api/
│  │  ├─ index.js              Axios 实例：Token 注入、统一错误处理
│  │  └─ sse.js                SSE 服务（重连、事件分发）
│  ├─ components/
│  │  ├─ dashboard/            看板图表卡（趋势、排行、状态环）
│  │  ├─ ui/                   通用 UI 组件
│  │  └─ GlobalNotifications.vue  全局通知（消费 SSE 事件）
│  ├─ composables/
│  │  ├─ useOrderList.js       订单列表状态 + SSE 增量更新
│  │  └─ useCountdown.js       待处理倒计时
│  ├─ constants/               coupon / order / product / redemption / user 枚举
│  ├─ layouts/AdminLayout.vue  侧边栏 + 顶栏布局
│  ├─ router/index.js          路由与登录守卫
│  ├─ stores/admin.js          管理员会话与权限
│  ├─ styles/                  全局样式
│  ├─ utils/
│  │  ├─ date.js               日期格式化（dayjs）
│  │  └─ image.js              图片 URL 解析（统一拼 VITE_IMAGE_BASE_URL）
│  └─ views/
│     ├─ Login.vue             登录
│     ├─ Dashboard.vue         运营看板
│     ├─ Orders.vue            实时订单（筛选、接单、完成、取消）
│     ├─ Users.vue             会员列表
│     ├─ UserDetail.vue        会员详情与积分调整
│     ├─ Redemptions.vue       兑换订单（备货 / 发货 / 完成）
│     └─ products/
│        ├─ CoffeeProducts.vue  咖啡商品管理
│        ├─ PointsProducts.vue  积分商品管理
│        └─ Archives.vue        内容档案（产区/豆单/拼配）
├─ .env.development            本地开发环境变量
├─ Dockerfile                  多阶段构建（Node → Nginx）
├─ nginx.conf                  Nginx SPA 路由 + 静态资源缓存
└─ vite.config.js              Vite + API 代理（端口 5174，strictPort）
```

路由：`/login`、`/`（看板）、`/users`、`/users/:id`、`/products`、`/products/coffee`、`/products/points`、`/products/archives`、`/orders`、`/redemptions`

## 环境要求

- Node.js 18 或 20
- npm 9+
- Java 17
- Maven 3.9+
- Docker Desktop

## 第一次安装

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-admin
npm install
```

## 启动后端开发环境

管理端与 Web / 小程序共用同一套后端。在仓库根目录启动基础设施：

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee
$env:MYSQL_ROOT_PASSWORD="123456"
docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker minio
docker compose ps
```

> `minio` 必须一起起：商品图与营销图都存在 MinIO 桶里（由 `VITE_IMAGE_BASE_URL` 指向），漏掉会整站图片 404。

在 IDEA 中依次启动 5 个微服务：User Provider → Member Provider → Order Provider → Mall Provider → Gateway。

Gateway 健康检查：

```text
http://localhost:8080/api/auth/test
```

数据库表与种子数据由各 Provider 启动时的 **Flyway 自动迁移**（`src/main/resources/db/migration`），无需手工执行 SQL。

### 准备一个管理员账号

仓库**不包含管理员种子账号**。先在前端注册一个普通账号，再把它提升为管理员（`users` 表在 `cozy_user` 库）：

```powershell
docker exec -i cozy-mysql mysql -uroot -p123456 -e "UPDATE cozy_user.users SET role='admin' WHERE email='<你注册的邮箱>';"
```

再注册一个普通账号（`role='user'`）留着，用于验证「普通用户进不了管理端」的权限拦截。

## 运行与验证

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-admin
npm run dev
```

浏览器打开 `http://localhost:5174`（端口写死 strictPort，被占用会直接报错而不是换端口）。

### 验收清单

1. `role='user'` 的账号登录后被拒绝进入管理端；管理员账号可进入
2. 看板：营业额/订单量趋势、状态分布环、商品排行图表均渲染且数据来自接口
3. 订单页：全部/待处理/制作中/已完成筛选、订单号搜索、日期范围筛选正常
4. 订单页接单生成取餐码；完成与取消状态流转正确，列表与看板同步刷新
5. 另开一个浏览器用用户端下单，管理端**无需刷新**应收到新订单通知（SSE）
6. 会员列表搜索、进入详情、积分调整后数值与用户端一致
7. 咖啡商品：列表含下架商品，新增、编辑、上下架、删除均生效
8. 积分商品与内容档案页可正常编辑保存
9. 兑换订单：备货、填写物流发货、完成三个动作状态正确
10. 退出登录后回到登录页，直接访问 `/` 被守卫拦回登录页

## 自动化验证

```powershell
npm test              # Vitest 单测（tests/unit）
npm run test:unit     # 同上
npm run lint
npm run format -- --check
```

管理端 **API 自动化测试**（Node 直跑，覆盖登录、角色权限、看板统计、用户/订单/商品/兑换的接口行为）：

```powershell
node tests/admin-api-test.js
```

- 需要后端已启动（默认 `http://localhost:8080/api`），并已准备**管理员 + 普通用户**两个账号
- 可用环境变量覆盖：`API_BASE`、`TIMEOUT_MS`（默认 10000）、`VITE_API_BASE_URL`（兼容前端约定，自动读 `.env` / `.env.development`）
- 默认**不执行会改数据的状态流转**；要跑写操作用 `ALLOW_MUTATIONS=true node tests/admin-api-test.js`
- 有用例失败或登录失败时进程以非零码退出，可接入 CI

## 部署形态

构建产物由 `Dockerfile` 打进 Nginx 镜像（`docker-compose.prod.yml` 的 `admin` 服务，`${ADMIN_PORT:-5174}:80`），静态图片基址由 CI 构建镜像时以 `--build-arg` 注入，**改了必须重发镜像**。

服务器上 5174 通常不对公网开放，用 SSH 隧道访问：

```bash
ssh -N -L 5174:127.0.0.1:5174 deploy@<服务器IP>   # 然后浏览器开 http://127.0.0.1:5174
```

## 常见问题

### 登录后接口全部 401 / 被踢回登录页

确认 Gateway 已启动、`VITE_API_TARGET`（开发，默认 `http://localhost:8080`）指向正确。管理端走 Token 认证，Token 失效会由 Axios 拦截器统一处理。

### 图片不显示

图片由 `VITE_IMAGE_BASE_URL` 统一拼接（DB 只存相对路径）。本地应指向本地 MinIO（`.env.development` 默认 `http://127.0.0.1:9000/cozycoffee`），并确认 `minio` 容器在跑、桶已 seed。参见 `surx-note/CozyCoffee/部署上线/服务器图片迁移.md`。

### 端口 5174 被占用

`vite.config.js` 配了 `strictPort: true`，不会自动换端口。先释放 5174 或改配置。
