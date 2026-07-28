# CozyCoffee Web

CozyCoffee 品牌官网与会员中心，基于 Vue 3、Vite、Pinia 和 Element Plus。当前覆盖：

- 品牌首页（hero、产地旅程、菜单、会员板块）
- 会员中心（仪表盘、签到、积分、等级权益）
- 咖啡点单（菜单浏览、购物车、规格定制、下单）
- 积分商城（商品兑换、优惠券）
- 个人中心（资料编辑、地址管理、订单历史）

## 项目结构

```text
cozy-coffee-web/
├─ public/images/              静态资源（hero、菜单、产地、图标等）
├─ scripts/
│  ├─ coffee-map.test.mjs      咖啡地图路径/投影测试
│  ├─ homepage-membership.test.mjs  会员积分计算测试
│  ├─ generate-world-map.mjs   从 TopoJSON 生成 SVG 路径
│  └─ generate-noise.py        生成噪点纹理
├─ src/
│  ├─ api/
│  │  ├─ request.js            Axios 实例：cookie 认证、统一错误处理、401 跳转登录
│  │  ├─ auth.js               登录、注册、登出、个人资料
│  │  ├─ member.js             会员信息、签到、积分、地址、SSE
│  │  ├─ mall.js               积分商城、兑换、优惠券
│  │  └─ order.js              咖啡产品、创建订单、订单查询与取消
│  ├─ assets/
│  │  └─ styles/               12 个按视图拆分的 CSS 样式文件
│  ├─ components/
│  │  ├─ NavBar.vue            站点导航（透明/滚动/深色三态）
│  │  ├─ Footer.vue            页脚（联系方式、社交媒体、版权）
│  │  ├─ CoffeeOrderView.vue   完整点单体验（分类、产品卡、购物车）
│  │  ├─ ProductCustomizer.vue 规格定制弹窗（杯型/甜度/温度/浓度/奶型）
│  │  ├─ address/              地址表单弹窗（省市区三级联动）
│  │  ├─ cart/                 购物车（商品列表、用餐方式、地址、优惠券、价格）
│  │  ├─ coupon/               优惠券卡片与标签列表
│  │  ├─ home/                 首页模块（地图、菜单、会员、产地旅程）
│  │  └─ order/                优惠券选择弹窗
│  ├─ composables/
│  │  ├─ useCart.js            购物车状态（localStorage 持久化）
│  │  ├─ useAddresses.js       地址 CRUD
│  │  ├─ useCoupons.js         优惠券加载与筛选
│  │  ├─ useCouponDisplay.js   优惠券主题/图标映射
│  │  ├─ useHomeMembership.js  首页会员进度与积分逻辑
│  │  └─ useResponsiveMapCamera.js  地图视口响应式参数
│  ├─ constants/
│  │  ├─ coupon.js             COUPON_TYPE、COUPON_STATUS 枚举
│  │  ├─ order.js              ORDER_STATUS 枚举
│  │  └─ user.js               MEMBER_LEVEL 枚举（含倍率、配色）
│  ├─ data/
│  │  ├─ china-regions.json    中国省-市-区三级地区数据
│  │  ├─ coffeeOrigins.js      8 个咖啡产区数据 + 杭州汇总结论
│  │  ├─ homeMenu.js           首页菜单产品 + 风味分类图片
│  │  └─ worldCountryPaths.js  世界地图 SVG 路径（TopoJSON 生成）
│  ├─ router/index.js          Vue Router：14 个路由 + 登录守卫 + 滚动行为
│  ├─ stores/user.js           Pinia 用户 store（cookie 认证、签到乐观更新）
│  ├─ utils/
│  │  ├─ coffeeMap.js          咖啡带坐标转换与路线计算
│  │  ├─ coffeeMapCamera.js    地图视口相机与安全区域
│  │  ├─ coffeeMapSvgRenderer.js  相机状态转 SVG 变换矩阵
│  │  ├─ couponRules.js        纯函数：6 种券型验证与折扣计算
│  │  ├─ date.js               日期/时间格式化
│  │  ├─ homepageMembership.js 积分收益、兑换成本、进度计算
│  │  └─ image.js              图片 URL 解析（API 前缀 / CDN 回退）
│  └─ views/
│     ├─ Home.vue              首页（hero + 产地旅程 + 菜单 + 会员板块）
│     ├─ About.vue             关于页（品牌故事、团队、拼配实验室）
│     ├─ Login.vue             登录页（用户名/手机/邮箱 + 密码）
│     ├─ Register.vue          注册页（含邀请码）
│     └─ member/               会员子页面（需登录）
│        ├─ MemberLayout.vue   会员仪表盘布局（侧边栏 + 路由视图）
│        ├─ MemberCenter.vue   仪表盘（等级卡、签到、积分指南、月度挑战）
│        ├─ CoffeeOrder.vue    点单页
│        ├─ PointsMall.vue     积分商城（分类标签、兑换弹窗）
│        ├─ CoffeeOrdersHistory.vue   咖啡订单历史
│        ├─ RedeemOrdersHistory.vue   兑换订单历史
│        ├─ PersonalInfo.vue   个人信息编辑（昵称/手机/生日）
│        ├─ MyCoupons.vue      我的券包
│        ├─ MemberBenefits.vue 会员权益对比（5 等级）
│        └─ components/        会员仪表盘子组件
├─ .env.development            本地开发环境变量
├─ .eslintrc.cjs               ESLint 配置
├─ .prettierrc                 Prettier 配置
├─ Dockerfile                  多阶段构建（Node 20 → Nginx）
├─ nginx.conf                  Nginx SPA 路由 + 静态资源缓存
├─ index.html                  入口 HTML
├─ package.json                依赖与脚本
└─ vite.config.js              Vite + Element Plus 自动导入 + API 代理
```

## 环境要求

- Node.js 18 或 20
- npm 9+
- Java 17
- Maven 3.9+
- Docker Desktop

## 第一次安装

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-web
npm install
```

## 启动后端开发环境

Web 端与移动端共用同一套后端。在仓库根目录启动基础设施：

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee
$env:MYSQL_ROOT_PASSWORD="123456"
docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker
docker compose ps
```

在 IDEA 中依次启动 5 个微服务：

1. User Provider
2. Member Provider
3. Order Provider
4. Mall Provider
5. Gateway

Gateway 健康检查：

```text
http://localhost:8080/api/auth/test
```

## 运行与验证

启动开发服务器：

```powershell
cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-web
npm run dev
```

浏览器打开 `http://localhost:5173`。

### 验收清单

1. 首页 hero 图片与 CTA 正常渲染，点击"探索产地风味之旅"滚动至产地旅程
2. 产地旅程滚动时地图路线高亮同步，章节卡片逐个进入视口
3. 菜单板块：6 种风味路线可切换，产品图片按 avif→webp→jpg 格式降级
4. 会员板块：匿名状态显示模拟进度，登录后显示实际 EXP 与积分倍率
5. 注册一个账号 → 登录 → 进入会员中心
6. 会员仪表盘：等级卡、签到（7 天进度条）、积分指南、月度挑战
7. 点击"点单"进入点单页：分类标签、产品卡、规格定制、加入购物车
8. 购物车：用餐方式切换、优惠券选择、价格明细、提交订单
9. 订单历史：列表含商品详情、状态、取餐码，待处理订单可取消
10. 积分商城：商品分类筛选、兑换确认弹窗
11. 优惠券页：可使用/已使用/已过期三态切换
12. 个人信息：编辑昵称、手机号（内联编辑）、生日（每年限改一次）
13. 会员权益页：5 个等级卡，黑金可领取月度权益包
14. 退出登录后页面自动跳转首页，会员入口切换为登录按钮
15. 响应式：桌面端和移动端均正常显示（NavBar 折叠、地图视口自适应）

## 自动化验证

```powershell
npm run build
npm run lint
npm run format -- --check
npm run test:coffee-map
node scripts/homepage-membership.test.mjs
```

测试覆盖：

- 咖啡地图：8 条产区路线、坐标投影、安全区域、SVG 路径生成
- 会员积分：消费积分计算、兑换折扣、进度百分比
- 会员日（每月第 3 天）+0.5 倍加成

## 环境变量

| 变量 | 开发环境值 | 说明 |
|------|-----------|------|
| `VITE_API_BASE_URL` | （空） | API 基础路径，为空时走 Vite 代理 `/api` |
| `VITE_API_TARGET` | `http://localhost:8080` | 代理目标地址 |
| `VITE_ASSET_BASE_URL` | OSS CDN URL | 静态资源基址，为空时用本地图片 |
| `VITE_MENU_IMAGE_BASE` | OSS CDN 路径 | 菜单位图具体路径 |

## 常见问题

### 401 跳转登录

后端的 cookie 会话在页面刷新后通过 `/api/auth/me` 恢复。如果该接口返回 401，导航守卫会自动跳转至 `/login`。

### 菜单或首页图片不显示

生产环境图片托管在阿里云 OSS，通过 `VITE_ASSET_BASE_URL` 和 `VITE_MENU_IMAGE_BASE` 配置。本地开发时优先使用 `public/images/` 中的本地图片。如果图片缺失，`getImageUrl()` 有回退逻辑。

### Element Plus 组件未注册

项目使用 `unplugin-vue-components` 自动按需注册 Element Plus 组件，无需手动 import。如果某组件报未注册错误，检查 `vite.config.js` 中 `ElementPlusResolver` 是否启用。

### Docker 构建

```powershell
docker build -t cozy-coffee-web .
docker run -p 80:80 cozy-coffee-web
```

构建产物通过 Nginx 提供静态服务，所有 SPA 路由指向 `index.html`，`/assets/` 下的文件缓存 30 天。
