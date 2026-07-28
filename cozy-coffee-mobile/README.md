# CozyCoffee Mobile

CozyCoffee 移动端基于 Vue 3、uni-app、Pinia 和 Vite，当前开发目标是：

- H5 浏览器开发与验证
- 微信开发者工具和微信真机开发验证
- 固定单门店自提
- 模拟支付

当前阶段不包含多门店、外卖、真实微信支付、正式域名和 Android/iOS App。

## 项目结构

```text
cozy-coffee-mobile/
├─ src/
│  ├─ api/                    后端 HTTP 接口定义
│  │  ├─ auth.js              登录、微信开发登录、资料、密码重置
│  │  ├─ order.js             购物车校验、创建订单、订单查询与取消
│  │  ├─ product.js           菜单、搜索、Banner、积分商品
│  │  ├─ member.js            会员、积分、签到、地址与兑换
│  │  └─ request.js           Base URL、Token、统一错误转换
│  ├─ components/
│  │  ├─ order/               商品规格、购物车、门店与结算组件
│  │  └─ states/              加载、空数据、失败与离线状态
│  ├─ domain/
│  │  ├─ cart/                购物车条目标识与旧缓存迁移
│  │  ├─ checkout/            价格预览纯函数与 Checkout 状态机
│  │  └─ member/              等级、兑换折扣、限购与月度任务规则
│  ├─ config/
│  │  └─ store.js             当前固定自提门店配置
│  ├─ constants/
│  │  └─ member.js            会员等级、阈值与显示名称
│  ├─ services/
│  │  ├─ checkout/            CheckoutWorkflow 与 Preview Service
│  │  ├─ session/             微信开发会话与身份恢复
│  │  ├─ payment/             PaymentService 与 MockPaymentAdapter
│  │  ├─ order/               订单 API 编排、幂等和再来一单恢复
│  │  ├─ network/             uni-app 网络状态
│  │  ├─ errors/              Auth/Business/Network/Validation Error
│  │  └─ logging/             脱敏日志
│  ├─ stores/
│  │  ├─ session.js           Token、用户和会员信息
│  │  ├─ cart.js              持久化购物车
│  │  ├─ checkout.js          结算意图和状态
│  │  └─ user.js              旧页面兼容入口
│  ├─ pages/
│  │  ├─ index/               首页与 Banner
│  │  ├─ menu/                菜单、规格和商品详情
│  │  ├─ order/               订单确认、结果、列表和详情
│  │  ├─ login/               登录与开发密码重置
│  │  ├─ mall/                积分商城、数量兑换与兑换记录
│  │  ├─ points/              积分明细
│  │  ├─ profile/             个人中心、资料、头像与邀请码
│  │  └─ ...                  会员、签到、优惠券和设置
│  ├─ App.vue                 App 生命周期、会话与网络恢复
│  ├─ pages.json              页面路由和 TabBar
│  └─ uni.scss                全局设计 Token
├─ .env.development           本地开发接口地址
├─ .env.test                  自动化测试配置
├─ package.json               启动、构建和测试命令
└─ vite.config.js             H5 代理与 uni-app 构建配置
```

## 环境要求

- Node.js 18 或 20
- npm 9+
- Java 17
- Maven 3.9+
- Docker Desktop
- 微信开发者工具

## 第一次安装

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-mobile
npm install
```

项目将 Pinia 固定为 `2.1.7`，用于兼容当前 uni-app 所使用的 Vue 3.4.21。不要单独升级 Vue、Pinia 或 vue-router。

## 启动后端开发环境

在仓库根目录启动基础设施：

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee
$env:MYSQL_ROOT_PASSWORD="123456"
docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker
docker compose ps
```

首次使用新的移动端 Checkout 代码时执行数据库迁移：

```powershell
Get-Content -Raw .\cozy-coffee-backend\mysql\migrations\V20260714_014__add_order_idempotency_key.sql |
  docker exec -i cozy-mysql mysql -uroot -p123456 cozy_order
```

然后在 IDEA 中依次启动：

1. User Provider
2. Member Provider
3. Order Provider
4. Mall Provider
5. Gateway

Gateway 健康检查：

```text
http://localhost:8080/api/auth/test
```

## H5 运行与人工验证

启动：

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-mobile
npm run dev:h5
```

浏览器打开终端输出的地址，通常为 `http://localhost:5173`。

### H5 验收清单

1. 分别注册手机号账号和邮箱账号，确认两种账号都能登录。
2. 进入“我的 → 个人资料与邀请码”，维护昵称、手机号、邮箱、生日和头像；刷新后数据仍应存在。
3. 复制本人邀请码；使用另一个未绑定邀请关系的账号补填一次，重复绑定应由后端拒绝。
4. 在“忘记密码”中设置新密码，退出后用新密码登录。
5. 首页确认 Banner、推荐商品和搜索均来自真实接口；“门店信息”只能看到固定中心店，不得出现虚假定位或多家 Mock 门店。
6. 菜单及商品详情选择温度、杯型、糖度、奶型和浓度；不同规格应形成不同购物车条目，详情加购必须使用真实商品 ID。
7. 在购物车修改规格，确认价格和条目正确更新。
8. 进入结算页，确认显示固定门店自提和模拟支付说明；取消模拟支付后购物车仍在。
9. 再次提交并确认支付，查看订单结果、订单号和取餐码；连续快速点击提交只能生成一张订单。
10. 保持订单详情页面打开，在管理端接单或完成订单；页面应在 5 秒内刷新，会员积分、EXP 和等级随后同步。
11. 对已完成或已取消订单点击“再来一单”，确认仍在售商品和可兼容规格被恢复，并自动展开菜单购物车；下架商品应被跳过并提示。
12. 从个人中心点击不同订单状态，确认 TabBar 订单页打开正确筛选。
13. 每日签到固定增加 2 积分；连续 7 天说明应为赠送满 35 减 10 券。
14. 会员权益页应加载真实等级阈值、本月权益状态和三个自提范围月度任务；可领取时领取后状态更新。
15. 积分商城选择不同数量，确认按会员折扣后的积分校验；兑换后可在“兑换记录”查看权益码或取货码并取消允许取消的订单。
16. 关闭后端服务后，菜单、会员、积分商城和订单页面应显示真实错误与重试入口，不得回退到 Mock 数据。

## 微信开发者工具运行与人工验证

构建：

```powershell
npm run build:mp-weixin
```

微信开发者工具导入：

```text
C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-mobile\dist\build\mp-weixin
```

在“详情 → 本地设置”中，开发阶段可勾选“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。

### 微信验收清单

1. 清除小程序缓存后进入登录页，同意协议并点击“微信登录”。
2. 前端调用 `uni.login`，后端 `/api/auth/wechat/session` 创建或恢复稳定的开发用户。
3. 再次清缓存以外的普通重启，确认仍能恢复同一个用户。
4. 完成菜单、规格、购物车、结算、模拟支付、结果和详情完整链路。
5. 在订单结果页停留，管理端改变订单状态，确认页面约 3 秒自动刷新。
6. 在订单详情页切后台再回来，确认轮询恢复；离开页面后轮询停止。
7. 在开发者工具中切换“离线”，确认结算被阻止且购物车保留；恢复网络后可以继续。
8. 检查 iPhone 刘海屏和底部安全区，购物车条、提交按钮和底部弹层不能被遮挡。

开发微信登录使用本地持久化设备 ID 映射开发账号，不请求真实微信 `code2Session`。它只适用于当前开发环境。

## 自动化验证

```powershell
npm test
npm run build:h5
npm run build:mp-weixin
```

当前测试覆盖：

- 购物车规格唯一键与缓存迁移
- 商品详情标准购物车结构与历史订单恢复
- Checkout 价格预览
- Checkout 状态机
- 统一错误类型
- 订单 Preview 与 Idempotency-Key
- 模拟支付成功、取消和失败
- CheckoutWorkflow 登录、离线和重复提交恢复
- 微信开发设备 ID 的稳定复用
- 会员等级阈值、积分兑换折扣、月度限购和月度任务规则
- 订单完成后的会员资料刷新

## 常见问题

### 微信工具请求不到后端

微信开发者工具在电脑上可以使用 `127.0.0.1:8080`。如果使用手机预览，需要把 `.env.development` 的 `VITE_API_BASE_URL` 改为电脑局域网 IP，然后重新构建。

### 提示 Unknown column `idempotency_key`

说明尚未执行 `V20260714_014__add_order_idempotency_key.sql`。

### npm install 出现 Vue/Pinia 冲突

确认 `package.json` 中 Pinia 为精确版本 `2.1.7`，不要改回 `^2.1.7`。

### Sass 出现 `@import` 弃用警告

当前不影响开发构建；这是后续升级 Sass 模块系统时处理的技术债。
