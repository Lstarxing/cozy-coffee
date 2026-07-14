# CozyCoffee 首页重设计规格

**日期**: 2026-07-14
**状态**: 方向已冻结，待实现
**设计方向**: 余白の温度 (Warm Reserve) — 日式克制 + 咖啡的温度感

---

## 1. 设计目标

将首页从模板化的信息陈列页，转变为**品牌叙事 + 会员转化**驱动的体验。用户打开官网 → 被咖啡品质打动 → 理解积分生态的价值 → 产生注册冲动。

核心差异化：**独特的会员积分生态**。

---

## 2. 设计系统

### 2.1 色彩

策略：整体 Restrained（纯白底），会员叙事区形成唯一的 Committed Moment（深色背景）。

```css
:root {
  /* === 新品牌 Token（首页及重构组件使用） === */

  /* 浅色上下文 */
  --cozy-bg: oklch(1 0 0);                    /* 纯白 */
  --cozy-surface: oklch(0.97 0.003 42);        /* 极微暖灰，卡片/分区 */
  --cozy-primary: oklch(0.42 0.09 42);         /* #753A22 深烘咖啡棕 */
  --cozy-primary-hover: oklch(0.35 0.08 42);   /* 悬停态 */
  --cozy-on-primary: oklch(0.98 0 0);          /* 主色上文字 */
  --cozy-accent: oklch(0.50 0.07 135);         /* #526C43 釉面绿 */
  --cozy-accent-soft: oklch(0.92 0.03 135);    /* 绿色浅底色 */
  --cozy-on-accent: oklch(0.98 0 0);
  --cozy-ink: oklch(0.17 0.008 42);            /* 正文墨黑 */
  --cozy-muted: oklch(0.46 0.008 42);          /* 辅助文字 */
  --cozy-surface-alt: oklch(0.25 0.025 42);    /* #2C1E18 深色叙事区 */
  --cozy-border: oklch(0.89 0.004 42);         /* 分割线 */

  /* 深色上下文 */
  --cozy-on-surface-alt: oklch(0.98 0 0);      /* 深色底主文字 */
  --cozy-muted-on-alt: oklch(0.78 0.01 42);    /* 深色底辅助文字 */
  --cozy-border-on-alt: oklch(0.98 0 0 / 0.16);/* 深色底分割线 */
  --cozy-accent-on-alt: oklch(0.53 0.07 135);  /* 深色底釉面绿 ≥ 3:1 */

  /* 深色区 CTA — 反色按钮（近白底+深字） */
  --cozy-cta-alt-bg: oklch(0.95 0.002 42);
  --cozy-cta-alt-text: oklch(0.30 0.05 42);

  /* === 旧变量保持原值不变（登录/注册/About 仍在使用） === */
  --primary-color: #7C5CFC;
  --text-color: #000000;
  --border-color: #E5E5E5;
  --background-color: #F8F9FD;
  --card-background: #FFFFFF;
	/* 共享布局变量 */
	--nav-height: 70px;
}
```

**关键决策**：旧 CSS 变量保持原值不变。新 `--cozy-*` 前缀 token 仅用于首页 scoped 样式和重构后的组件。登录/注册/About 页不受影响。

**使用规则**：
- `cozy-primary` 集中在按钮、链接、Origins 当前航线状态和会员 CTA，不零散撒在标题/普通图标/装饰边框
- `cozy-accent`（釉面绿）仅用于：积分到账数字、进度填充、已解锁/已升级状态标记
- `cozy-surface` 不连续覆盖多个整屏 section
- `cozy-surface-alt` 仅在 Membership 区和 Footer 使用
- 深色区 CTA 使用反色按钮（`cozy-cta-alt-bg` / `cozy-cta-alt-text`），不能在 surface-alt 上直接使用 primary

### 2.2 字体

统一无衬线系统，不加载外部字体。

```css
--font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI",
  "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
```

**排印规则**：
- Hero 标题: `clamp(2.75rem, 6vw, 5rem)`, 字重 600
- Section 标题: `clamp(2rem, 4vw, 3.25rem)`, 字重 600
- 正文: 16-18px, 行高 1.65-1.8
- 中文标题字距: 0 至 0.02em（不使用 0.2em 松散字距）
- 英文大标题字距: 约 -0.02em
- 正文宽度: 中文约 28-34 字/行，英文 ≤ 65-75ch
- 深色区浅色文字行高 +0.05-0.1

### 2.3 间距

基于 4px 网格: 4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 / 96 / 128

### 2.4 形状与阴影

- 圆角: 图片 0-8px, 容器 8-12px, 卡片上限 16px
- 阴影: 默认无阴影，仅浮层和悬停使用轻微阴影
- 焦点态: 按钮和链接需可见的 keyboard focus ring（深色区使用 `cozy-on-surface-alt` 色）
- 触控目标: 交互元素至少 44×44px

### 2.5 动效

- 入口淡入仅用 opacity（不用 translateY）
- `@media (prefers-reduced-motion: reduce)` 回退为即时显示

### 2.6 图片性能

- Hero 必须使用 `<picture>/<img>`（非 CSS background-image）
- Hero 提供 AVIF/WebP/JPEG 三格式 `srcset` + `sizes`
- Hero 图片设置 `fetchpriority="high"`，不使用 lazy load
- Hero 图片设置 `width`/`height` 或 `aspect-ratio`，防止 CLS
- Menu 图片使用 `loading="lazy"`；Origins 使用内联静态 SVG，不产生额外图片请求
- 所有图片有描述性 `alt` 文本
- 加载失败：显示占位符，保持原始宽高比

---

## 3. 页面结构

### 3.1 导航栏（NavBar）

- 固定顶部，高度 `--nav-height: 70px`
- 默认：`background: rgba(255,255,255,0.95)` + `backdrop-filter: blur(10px)`
- 深色区适配：使用 `IntersectionObserver` 监听 `#membership`，进入后添加 `.navbar--on-dark` 类，切换为不透明白底
- 离开首页路由时停止 observer
- 所有锚点目标 section: `scroll-margin-top: var(--nav-height)`

### 3.2 Hero

**定位**: 产品优先，会员入口降级

```html
<picture>
  <source type="image/avif"
    srcset="hero-720.avif 720w, hero-1080.avif 1080w, hero-1440.avif 1440w, hero-1920.avif 1920w"
    sizes="100vw">
  <source type="image/webp"
    srcset="hero-720.webp 720w, hero-1080.webp 1080w, hero-1440.webp 1440w, hero-1920.webp 1920w"
    sizes="100vw">
  <img
    src="/images/hero-pourover.jpg"
    alt="手冲咖啡萃取落入玻璃分享壶"
    fetchpriority="high"
    width="1440" height="900"
    sizes="100vw"
  >
</picture>
```

- 高度: `min-height: min(560px, calc(100svh - var(--nav-height))); height: 70svh; max-height: 760px`
  （横屏设备 `min(560px, ...)` 防止首屏过高）
- 固定暗色 scrim overlay（`linear-gradient(rgba(0,0,0,0.25), rgba(0,0,0,0.55))`），不依赖照片本身亮度
- 标题: "把产地的风味，留在这一杯里" — 白色文字 ≥ 3:1 对比 scrim 最深区域
- 副标题: "全球八处产区 ｜ 小批次新鲜烘焙" — ≥ 4.5:1 对比
- 主 CTA: [查看菜单] — 实心 `cozy-primary` 按钮
- 次级链接: "会员如何回馈每一杯 →" — 白色文字链接，`href="#membership"`，hover 时加下划线（非颜色变化）
- 所有 `srcset` 候选图的裁切均需验证文字区域对比度

### 3.3 豆源故事（Origins）

**定位**: 证明品质，并把“全球产区 → 杭州烘焙 → 杯中表达”讲成首页的探索式品牌故事。

#### 3.3.1 核心概念与文案

中文主标题：

> 风味从土地开始

英文副标题：

> Eight Origins. One Cup.

地图收束文案：

> 世界八处产区，最终在杭州完成属于 Cozy Coffee 的风味语言。

进入 Menu 前的第二层转场：

> 最终，成为今天杯中的六种表达。

#### 3.3.2 产区范围与顺序

8 个产区分别直接连接杭州 Cozy Coffee 烘焙中心，云南与海外产区地位平等，不作为中转站：

1. 埃塞俄比亚 Ethiopia — 咖啡的故乡 / 花香层次
2. 肯尼亚 Kenya — 东非明亮风味 / 莓果骨架
3. 巴西 Brazil — 稳定醇厚 / 浓缩基底
4. 哥伦比亚 Colombia — 安第斯平衡 / 平衡主体
5. 危地马拉 Guatemala — 火山土壤 / 香料层次
6. 巴拿马 Panama — 瑰夏代表 / 芳香高点
7. 印度尼西亚 Indonesia — 湿刨处理 / 醇厚深度
8. 中国云南 Yunnan — 东方探索 / 东方表达

终点：杭州 Hangzhou；地图下方小字为 `Cozy Coffee Roastery`。

#### 3.3.3 桌面滚动叙事

- 布局：约 60% sticky 世界地图 + 40% 滚动信息；地图位于导航下方，不遮挡标题
- 8 个产区章节各占约 55–65vh，不使用强制整屏吸附
- 第 9 个章节为统一数据结构中的 `summary` 章节，不创建特殊 `Final.vue`
- 当前章节进入视口中部时更新唯一状态源 `activeOrigin`
- 当前国家轮廓、产区点和航线使用 `cozy-primary` 咖啡棕
- 已浏览航线保留约 16% 透明度，形成逐步汇聚的视觉记忆
- 最终章节同时显示八条航线，杭州节点出现双环呼吸
- 最终信息面板显示 `Roasting → Blending → Cup`，随后进入 Menu

#### 3.3.4 地图视觉规则

- 使用预处理的真实世界地图轮廓，不使用 Mapbox、D3 运行时绘图或行政地图 UI
- Coffee Belt 使用 4–6% 透明度的暖金棕纬度光晕，边缘羽化；禁止画成实体横向色块
- 航线均为自动生成的大圆弧视觉（Quadratic Bézier），不使用直线
- 地图底色保持纯白，仅叠加约 2% 透明度的静态 `noise.png`，模拟咖啡包装纸纹理
- 禁止使用 SVG Filter 生成噪点，避免 Safari 掉帧
- 当前产区与杭州均使用咖啡棕，不使用釉面绿；杭州通过双环、尺寸和双语标签体现终点层级
- 地图内只显示 `杭州 / Hangzhou`，品牌身份放在下方 `Cozy Coffee Roastery`，不把 Logo 塞入地图

SVG 内部分层顺序：

```text
background
coffeeBelt
countries
routes
points
labels
hangzhou
```

#### 3.3.5 产区信息层级

右侧章节不使用悬浮卡片，直接通过留白与分隔线排版。每个产区展示：

- 产区定位 / 一句话叙事
- 海拔
- 处理法
- 代表品种
- 4 个风味标签
- 风味角色 Role：解释该产区为什么进入 Cozy Coffee 的风味体系

资料优先使用 World Coffee Research、Specialty Coffee Association 及各国咖啡协会；文案为品牌化摘要，不堆砌百科说明。

#### 3.3.6 数据与组件架构

```text
OriginsJourney.vue
├── CoffeeBeltMap.vue   # 纯 SVG Renderer
└── OriginChapter.vue   # v-for 数据驱动章节

coffeeOrigins.js        # 唯一内容数据源
```

`OriginsJourney.vue` 职责：

- 创建并销毁产区章节专用 `IntersectionObserver`
- 唯一持有 `activeOrigin` 与 `visitedOrigins`
- 通过显式 props 将状态传给 `CoffeeBeltMap` 和 `OriginChapter`
- 不使用 `provide/inject`，因为组件仅相隔一层

`CoffeeBeltMap.vue` 职责：

- 只接收 `activeOrigin`、只读 `visitedOrigins` 和产区数据
- 根据输入渲染 SVG，不创建 Observer、不监听滚动、不维护业务状态
- 根据归一化坐标自动计算 Quadratic Bézier 航线

`OriginChapter.vue` 职责：

- 通过 `v-for` 完全数据驱动渲染
- 不为具体国家写 `if/else`
- 同时支持 `origin` 与 `summary` 两种章节类型

产区数据不保存 SVG Path，只保存绘制输入：

```js
{
  id: 'ethiopia',
  type: 'origin',
  countryCode: 'ET',
  name: '埃塞俄比亚',
  englishName: 'Ethiopia',
  origin: { x: 0.58, y: 0.48 }, // SVG viewBox 归一化坐标
  destination: 'hangzhou',
  routeBend: -0.18,             // 可选曲率微调，不保存 path
  region: '...',
  altitude: '...',
  process: '...',
  varieties: ['...'],
  flavors: ['...'],
  role: '花香层次'
}
```

杭州坐标由地图组件单独定义。路线控制点由起点、终点、中点法向量、距离和可选 `routeBend` 标量计算，因此调整 viewBox 或杭州坐标时无需重画 8 条路径。

`visitedOrigins` 使用响应式 `Set<string>`；更新时创建新 Set 以保证 Vue 响应：

```js
visitedOrigins.value = new Set(visitedOrigins.value).add(id)
```

不使用 `ResizeObserver`：坐标与路线均位于 SVG viewBox 坐标系，响应式缩放由 `viewBox` + `preserveAspectRatio` 自动完成。只有未来引入动态地图投影或标签碰撞计算时才增加尺寸测量。

#### 3.3.7 动效与移动端

- 状态变化只更新 Vue class；CSS/SVG 完成国家填充、航线描边、圆点呼吸和文字淡入
- 不使用 GSAP ScrollTrigger；GSAP 仅保留为未来单点复杂动效的可选增强
- 可选咖啡豆飞行动画使用 CSS Motion Path，但 v1 不实现，避免噪声和移动端负担
- 移动端地图 sticky 于顶部约 38–42svh，章节从下方滚动
- 移动端仅显示当前产区与杭州标签，当前点使用 `1 → 1.1 → 1` 轻微呼吸
- 移动端保留当前航线与已访问淡线，不显示粒子
- `prefers-reduced-motion: reduce` 下取消航线描边、呼吸和淡入，状态即时切换
- `IntersectionObserver` 不可用时正文仍按 DOM 顺序完整显示，地图保持静态，不阻碍阅读

地图设为 `aria-hidden="true"`；所有信息由右侧语义化章节和进度文本提供，避免屏幕阅读器重复朗读。

#### 3.3.8 未来扩展性

未来增加 Costa Rica、Rwanda、Peru、Honduras、El Salvador 等产区时，只需在 `coffeeOrigins.js` 添加数据与坐标。默认曲率由地图组件计算，必要时用单个 `routeBend` 标量处理路线重叠，无需修改 `CoffeeBeltMap.vue` 或手绘 SVG Path。

品牌综合奖项仍放在 Menu 后作为独立信任证明，不嵌入溯源地图。

### 3.4 精品推荐（Menu）

**定位**: 制造欲望

- 桌面: `grid-template-columns: repeat(3, minmax(0, 1fr))` 锁定三列
- 平板: 两列，手机: 一列
- 价格始终可见
- 风味描述通过 `:hover` + `:focus-within` 同步强化显示
- 在 `@media (hover: none), (pointer: coarse)` 中始终显示操作
- 每个产品卡片有 "去点单 →" 按钮，链接至 `/member/order`（未登录由路由守卫跳转登录）
- 转场语: "选好这一杯，也开始记录下一杯。"

### 3.5 会员叙事（Membership）— Committed Moment

**定位**: 积分生态吸引 → 注册转化。深色背景 `cozy-surface-alt`。

#### CTA 状态矩阵

| 状态 | 主 CTA | 次级 CTA |
|------|--------|----------|
| 未登录 | [立即入会]（反色按钮，链接 `/register`） | 展开完整权益（`<details>` 内联） |
| 已登录 - 加载中 | 按钮 disabled / 骨架屏 | — |
| 已登录 - 成功 | [去积分商城]（链接 `/member/mall`） | [进入会员中心 →] |
| 已登录 - 失败 | [重试读取] | 查看示例权益 |

#### 会员数据契约（已登录 - 成功）

**认证状态机**（Home.vue 管理）:

```text
auth-resolving（userStore.init() 未完成）
→ anonymous（init 完成，无 token）
→ member-loading（有 token，开始拉数据）
→ member-success（全部成功）/ member-partial（月度任务失败但会员信息成功）/ member-failed（会员信息失败）
```

`auth-resolving` 期间不渲染会员区，避免短暂闪现未登录示例。

**数据获取策略**: Home.vue 直接调用 `getMemberInfo()` 和 `getMonthlyTask()` API（非 Store action），使用 `Promise.allSettled` 判断真实网络结果。Store 内部 `fetchMemberInfo()` 会自行捕获错误不抛出，不能用于 `allSettled` 判断。成功后按需同步 Store。

| 字段 | API | 失败处理 |
|------|-----|----------|
| 等级 | `getMemberInfo()` → `data.memberLevel` | → member-failed |
| 积分余额 | `getMemberInfo()` → `data.currentPoints` | → member-failed |
| 兑换目标 | 固定"5元代金券"（150积分 × 等级折扣），未登录标注"以 5 元代金券为目标示例"。已登录不额外查询库存——只作示例展示，不承诺实时可兑换 | — |
| 本月订单数 | `getMonthlyTask()` → `data.monthlyOrderCount` | → member-partial（仅隐藏订单数，不影响主体渲染） |

#### 未登录默认示例（普通日消费）

```
以白银会员为例 · 普通日消费示例

¥32 手冲咖啡
基础积分 32  +  等级加成 3
35 积分到账

当前积分 131 / 147
━━━━━━━━━━━━━━━━░░ 89%
距 5 元代金券还差 16 积分

商品原价 150 积分 · 白银会员价 147 积分（9.8 折）

白银会员 · 每杯 1.1×
每周五会员日额外 +0.5×  ·  生日买一赠一券

[立即入会]    ▸ 展开完整等级权益
```

**后端规则——积分数学（已全部验证）**:

| 规则 | 值 | 源码 |
|------|-----|------|
| 积分倍率 | basic=1.0x, silver=1.1x, gold=1.2x, diamond=1.3x, black=1.5x | `OrderRewardService.java:24` |
| 周五会员日 | 额外 +0.5×（白银周五 = 1.6×） | `OrderRewardService.java:36` |
| 积分计算 | `金额 × 倍率`，四舍五入 | `OrderCommandService.java:149` |
| 兑换折扣 | silver=0.98, gold=0.95, diamond=0.90, black=0.85 | `PointsMallServiceImpl.java:1548` |
| 兑换计算 | `ceil(原价 × 数量 × 折扣)` | `PointsMallServiceImpl.java:1559` |
| 5元代金券 | 150 积分 | `cozy_mall.sql:346` |

#### 示例推导

```
白银会员 × 5元代金券:
  ceil(150 × 1 × 0.98) = ceil(147) = 147 积分

131 积分 / 147 = 89%
差 16 积分
下一杯预计获得 35 积分，足够解锁 5 元代金券
```

#### 完整等级权益（`<details>` 内联展开）

```html
<details>
  <summary>展开完整等级权益</summary>
  <table aria-label="会员等级速览">
    <caption>等级成长体系</caption>
    <thead>
      <tr>
        <th scope="col">等级</th>
        <th scope="col">门槛</th>
        <th scope="col">倍率</th>
        <th scope="col">兑换折扣</th>
        <th scope="col">代表权益</th>
      </tr>
    </thead>
    <tbody>
      <tr><td>基础 Classic</td><td>0 EXP</td><td>1.0×</td><td>—</td><td>周五额外 +0.5× 积分</td></tr>
      <tr><td>白银 Silver</td><td>500 EXP</td><td>1.1×</td><td>9.8 折</td><td>生日买一赠一券</td></tr>
      <tr><td>黄金 Gold</td><td>1,500 EXP</td><td>1.2×</td><td>9.5 折</td><td>生日买一赠一券 + 8.8折券×2</td></tr>
      <tr><td>钻石 Diamond</td><td>4,000 EXP</td><td>1.3×</td><td>9.0 折</td><td>免单券 + 买一赠一券×2</td></tr>
      <tr><td>黑金 Black</td><td>9,000 EXP</td><td>1.5×</td><td>8.5 折</td><td>免单券×2 + 无限免配送</td></tr>
    </tbody>
  </table>
</details>
```

- 新增"兑换折扣"列——白银 9.8 折 / 黄金 9.5 折 / 钻石 9.0 折 / 黑金 8.5 折（与后端一致）
- 移动端: 通过 Vue `v-if` 条件渲染 `<dl>` 定义列表替代 `<table>`（非 CSS 切换——语义元素无法通过媒体查询改变）。桌面渲染 `<table>`。两份结构 `v-if`/`v-else` 互斥，避免屏幕阅读器重复朗读
- 当前等级用釉面绿 + 文字标记双重表达

#### 视觉规则
- 不使用悬浮卡片——交易、进度、权益直接排版在深色 section 内
- 釉面绿仅标记: 到账数字、进度填充、已解锁状态
- [立即入会] 使用 `cozy-cta-alt-bg` 背景 + `cozy-cta-alt-text` 文字（反色按钮）
- 进度条使用 `<progress value="131" max="147" aria-label="距5元代金券进度 89%">`
- `<details>` 内容使用 `cozy-surface-alt` 背景 + `cozy-on-surface-alt` 文字

### 3.6 页脚（Footer）

- 背景: `cozy-surface-alt` (与 Membership 同色)
- `border-top: 1px solid var(--cozy-border-on-alt)` 完整分隔线
- 保持现有三列布局: 联系方式 / 社交媒体 / 版权
- 所有文字使用深色上下文 token

---

## 4. 关键交互路由

| 按钮/链接 | 目标 | 说明 |
|-----------|------|------|
| 查看菜单 (Hero) | `#menu` 锚点 | 页内滚动 |
| 会员如何回馈每一杯 (Hero) | `#membership` 锚点 | 页内滚动，不触发路由导航 |
| 去点单 (Menu 卡片) | `/member/order` | 未登录由路由守卫跳转 `/login` |
| 立即入会 (Membership 未登录) | `/register` | — |
| 展开完整等级权益 (Membership 未登录) | `<details>` 内联 | 不离开页面 |
| 去积分商城 (Membership 已登录) | `/member/mall` | — |
| 进入会员中心 (Membership 已登录) | `/member` | — |
| 重试读取 (Membership 失败) | 重新 fetch | 页面内重试 |

---

## 5. 页面间过渡

- Origins 地图收束: "世界八处产区，最终在杭州完成属于 Cozy Coffee 的风味语言。"
- Origins → Menu: "最终，成为今天杯中的六种表达。"
- Menu → Membership: "选好这一杯，也开始记录下一杯。"

---

## 6. 实现范围与样式隔离

### 文件策略

| 文件 | 策略 |
|------|------|
| `src/assets/styles/style.css` | 新增 `--cozy-*` token + 字体 + reset。旧变量 `--primary-color` 等保持原值不变。删除旧全局 Hero/Menu/Membership/Footer 样式 |
| `src/views/Home.vue` | 编排 Hero、OriginsJourney、Menu、Membership；承载会员认证状态机与 API 数据。Home 本身不创建 Observer |
| `src/components/home/OriginsJourney.vue` | 管理 Origins sticky 布局、章节 Observer、`activeOrigin` 与 `visitedOrigins` |
| `src/components/home/CoffeeBeltMap.vue` | 内联静态世界地图 SVG；纯 props 驱动渲染国家、航线、点位、标签与杭州节点 |
| `src/components/home/OriginChapter.vue` | 数据驱动渲染 8 个产区章节及第 9 个 summary 章节 |
| `src/data/coffeeOrigins.js` | 8 个产区与 summary 数据、归一化坐标、可选曲率微调；不保存 SVG Path |
| `src/assets/images/noise.png` | 低对比度静态纸张噪点；仅用于地图区域约 2% 透明度叠加 |
| `src/components/NavBar.vue` | 独自管理 Membership 导航适配 Observer；不管理 Origins 章节状态 |
| `src/components/Footer.vue` | **迁移**: 将旧 Footer 完整结构与响应式样式从 `style.css` 迁移至 `<style scoped>`（三列 Grid、图标尺寸、间距、移动端折叠），**然后**调整背景色为 `cozy-surface-alt`，文字改为深色上下文 token |

### 不改动
- 登录/注册页面（旧 CSS 变量原值不变，无回归）
- 会员中心内部页面
- 管理后台
- 移动端 (uni-app)

---

## 7. 绝对禁令

- ❌ 渐变文字
- ❌ 侧边条纹边框装饰
- ❌ Glassmorphism 默认使用
- ❌ 每个 section 上方小号全大写 eyebrow
- ❌ 卡片套卡片
- ❌ 奶油色/米色页面底色
- ❌ 价格仅在 hover 显示
- ❌ 五张等权彩色等级卡片
- ❌ 操作按钮仅 hover 路径可达
- ❌ Hero 使用 CSS background-image（必须用 `<picture>/<img>`）
- ❌ 旧 CSS 变量被修改（保持登录/注册页隔离）

---

## 8. 验证清单

- [ ] 纯白底色不出现奶油/米色
- [ ] 旧 CSS 变量（`--primary-color` 等）值未被修改
- [ ] 所有价格始终可见
- [ ] 釉面绿仅用于状态标记（到账、进度、解锁）
- [ ] 会员示例: 131/147, 89%, 差16积分（白银 9.8 折后价）
- [ ] 会员示例文案: "下一杯预计获得 35 积分，足够解锁 5 元代金券"（非"两杯"）
- [ ] 等级表新增"兑换折扣"列
- [ ] 基础等级代表权益为"周五额外 +0.5× 积分"
- [ ] 钻石英文名为 Diamond（非 Platinum）
- [ ] "本月订单数"文案（非"本月杯数"）
- [ ] 认证状态机: auth-resolving → anonymous → member-loading → success/partial/failed
- [ ] auth-resolving 期间不渲染会员区（不闪现示例）
- [ ] Home.vue 直接调用 API（非 Store action），`Promise.allSettled` 判断真实网络结果
- [ ] 深色区 CTA 使用反色按钮，对比度合格
- [ ] 深色区釉面绿（cozy-accent-on-alt: 0.53）≥ 3:1
- [ ] --nav-height 定义在 :root，所有锚点和 Hero 引用
- [ ] 锚点跳转不被导航遮挡（scroll-margin-top）
- [ ] NavBar.vue 独自管理 Membership Observer；OriginsJourney.vue 独自管理产区章节 Observer；Home.vue 不创建 Observer
- [ ] Origins 使用 8 个数据驱动产区，均直接连接杭州，不以云南为中转站
- [ ] Coffee Belt 是 4–6% 透明度羽化光晕，不是实体横条
- [ ] 当前产区、航线和杭州使用咖啡棕；地图不使用釉面绿
- [ ] 航线由归一化坐标自动生成 Quadratic Bézier，不在数据文件存 SVG Path
- [ ] `activeOrigin` 是 OriginsJourney 中的唯一状态源，CoffeeBeltMap 是纯 Renderer
- [ ] `visitedOrigins` 使用响应式 Set，历史航线以低透明度保留
- [ ] SVG 按 background / coffeeBelt / countries / routes / points / labels / hangzhou 分层
- [ ] 地图噪点来自静态 PNG，不使用 SVG Filter
- [ ] 第 9 个 summary 章节与产区章节共用 Observer 与 OriginChapter 数据结构
- [ ] 最终双层文案先收束杭州，再自然过渡到六款 Menu
- [ ] 移动端地图 38–42svh sticky，只保留当前标签、杭州标签与轻微点位呼吸
- [ ] 减少动画模式下地图状态即时切换，无航线描边和呼吸动画
- [ ] Hero 使用 svh + min-height + max-height（横屏不溢出）
- [ ] Hero 使用 `<picture>/<img>` + width 描述符 srcset + scrim overlay + alt 文本
- [ ] Hero 文字与 scrim overlay 最深区域对比 ≥ 3:1(标题) / ≥ 4.5:1(副标题)
- [ ] Menu 网格桌面锁定三列，平板两列，手机一列
- [ ] Menu "去点单"按钮在 hover/focus-within/hover:none 均可达
- [ ] `<details>` 内联等级权益可键盘展开
- [ ] 等级表移动端通过 v-if/v-else 渲染 `<dl>`（非 CSS 切换）
- [ ] IntersectionObserver 在离开首页时 disconnect
- [ ] 已登录 fetch 使用 `Promise.allSettled`
- [ ] monthlyTask 失败不触发全局错误状态（仅隐藏订单数）
- [ ] Footer 完整结构已从 style.css 迁移至 scoped（非仅颜色）
- [ ] About 页面 Footer 无回归
- [ ] 登录/注册页无回归
- [ ] `prefers-reduced-motion` 回退生效
