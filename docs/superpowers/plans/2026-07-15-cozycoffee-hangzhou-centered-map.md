# CozyCoffee 杭州中心品牌地图 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to continue unfinished items. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status:** 业务代码与用户视觉调整已完成；自动测试、生产构建和分类 commit 尚未执行。

**Goal:** 让世界八处咖啡产区通过统一 Camera 汇聚杭州，并保证桌面/移动构图、左侧永久顶部安全区以及上下滚动章节切换具有稳定、可维护的行为。

**Architecture:** Natural Earth 路径和产区数据保留在 Map Coordinate 中；不可变 Camera 负责 focus、anchor、zoom 和屏幕投影；SVG Renderer Adapter 负责生成 matrix；CoffeeBeltMap 按 Background、Decoration、Camera、Label 分层。OriginsJourney 使用 sticky 左栏和章节上下双 sentinel：向下滚动由 top sentinel 决定边界，向上滚动由 bottom sentinel 决定边界。

**Tech Stack:** Vue 3 `<script setup>`、Vue Composition API、SVG、JavaScript ES Modules、JSDoc、IntersectionObserver、Node.js `node:test`

---

## 最终实现摘要

### 1. 地图坐标与 Camera

```text
Natural Earth / Origin Data
            ↓
Map Coordinate 1000 × 500
            ↓
Immutable Camera
            ↓
SVG Renderer / Label Projection
```

最终 Camera Preset：

```js
desktop: {
  anchor: { x: 0.74, y: 0.34 },
  zoom: 1,
}

mobile: {
  anchor: { x: 0.66, y: 0.34 },
  zoom: 1,
}
```

设计含义：

- 桌面端杭州位于地图横向约 74%，形成偏右品牌焦点。
- 移动端杭州位于约 66%，给双语标签预留更大的右侧空间。
- 所有地图整体位置调整只能修改 Camera Preset。
- 禁止修改产区坐标、杭州坐标、国家路径或单条航线偏移来修正布局。

### 2. 地图图层

```text
Background Layer
      ↓
Decoration Layer   Coffee Belt
      ↓
Camera Layer       countries / routes / points / Hangzhou point
      ↓
Label Layer        active origin / Hangzhou / roastery
```

- Coffee Belt 不跟随 Camera 横向漂移。
- 国家、航线和节点统一接受同一个 Camera matrix。
- 杭州节点属于地理层，杭州文字属于 Label Layer。
- 标签使用 Camera 投影后的屏幕坐标，并增加背景描边保护。

### 3. 永久顶部安全区

当前桌面端：

```css
--origins-sticky-gap: clamp(70px, 7vh, 88px);
```

移动端：

```css
--origins-sticky-gap: 12px;
```

同一个变量同时控制：

```css
.origins-left-column {
  top: calc(var(--nav-height) + var(--origins-sticky-gap));
}

#origins.origins-layout {
  scroll-margin-top: calc(var(--nav-height) + var(--origins-sticky-gap));
}
```

维护约束：

> 锚点落点和 sticky 左栏顶部必须使用同一个 `--origins-sticky-gap`。如果分别设置，点击“豆源”后的初始留白和滚动过程中的固定留白会不一致。

### 4. 双 sentinel 章节边界

旧实现观察整个 `.origin-chapter`：

```text
向下滚动：章节顶部进入激活线
向上滚动：章节底部进入激活线
```

这导致向上滚动时，上一章节已经出现但仍保持置灰，航线切换滞后。

最终实现为每个章节分别添加顶部和底部两个 `1px` sentinel：

```vue
<span
  class="origin-chapter__sentinel origin-chapter__sentinel--top"
  data-origin-sentinel="top"
  :data-origin-id="chapter.id"
  aria-hidden="true"
></span>

<span
  class="origin-chapter__sentinel origin-chapter__sentinel--bottom"
  data-origin-sentinel="bottom"
  :data-origin-id="chapter.id"
  aria-hidden="true"
></span>
```

两个 Observer 各自只观察对应 sentinel：

```js
const topActivationLine = window.innerHeight * 0.23
const bottomActivationLine = getProgressTrackLine()
```

Observer 使用具有实际高度的方向区域，而不是让 `1px` sentinel 与 `1px` 感应带碰撞：

```text
向下区域：视口顶部 → 23% 激活线
向上区域：Progress Track 基线 → 视口底部
```

这样即使滚轮单帧移动几十到上百像素，sentinel 也不会直接跳过 Observer 的有效区域。

最终行为：

```text
向下滚动：下一章节 top sentinel 穿过视口约 23% 激活线 → 切换
向上滚动：上一章节 bottom sentinel 穿过 journey-progress__track 基线 → 切换
```

滚动方向只负责选择当前响应 top Observer 还是 bottom Observer，不再通过整个章节的顶部、底部可见状态推断章节边界。Progress Track 的固定屏幕 Y 根据 sticky 左栏内部偏移动态计算；窗口 resize 或左栏尺寸变化时会重建两个 Observer。

---

## 最终文件职责

### 新增文件

- `cozy-coffee-web/src/utils/coffeeMapCamera.js`
  - Camera Preset、Safe Area、断点、不可变 Camera 和坐标投影。
- `cozy-coffee-web/src/utils/coffeeMapSvgRenderer.js`
  - 将 Camera 转换成 SVG matrix，不依赖杭州或产区业务数据。
- `cozy-coffee-web/src/composables/useResponsiveMapCamera.js`
  - 根据统一的 `820px` 断点选择 desktop/mobile Camera，并清理媒体查询监听。

### 修改文件

- `cozy-coffee-web/src/components/home/CoffeeBeltMap.vue`
  - 地图分层、Camera matrix、标签投影和杭州品牌焦点。
- `cozy-coffee-web/src/components/home/OriginsJourney.vue`
  - sticky 左栏、永久顶部安全区、sentinel Observer 和章节状态切换。
- `cozy-coffee-web/src/components/home/OriginChapter.vue`
  - 章节顶部和底部 sentinel。
- `cozy-coffee-web/scripts/coffee-map.test.mjs`
  - Camera、Safe Area、Renderer、航线几何、地图分层和 sentinel 结构测试。

### 明确未修改的数据

- `cozy-coffee-web/src/data/coffeeOrigins.js`
- `cozy-coffee-web/src/data/worldCountryPaths.js`
- `cozy-coffee-web/scripts/generate-world-map.mjs`

---

## 执行记录

### Task 1: Camera 数学模型

- [x] 创建不可变 `createMapCamera()`。
- [x] 创建通用 `projectPoint()` 和 `projectMapPoint()`。
- [x] 创建 desktop/mobile Camera Preset。
- [x] 创建 desktop/mobile Safe Area。
- [x] 提取 `MAP_CAMERA_BREAKPOINT = 820` 和媒体查询常量。
- [x] 使用 JSDoc 定义不可变 Camera 值对象。
- [ ] 运行 Camera 自动测试。

### Task 2: SVG Renderer 与地图分层

- [x] 创建独立 `cameraToSvgMatrix()` Renderer Adapter。
- [x] 保持 Camera 模块不包含 SVG、Canvas 或 DOM 知识。
- [x] 拆分 Background、Decoration、Camera、Label 图层。
- [x] 将 Coffee Belt 移入 Decoration Layer。
- [x] 将杭州和当前产区文字移入 Label Layer。
- [x] 增加文字背景描边，避免航线穿过标签。
- [ ] 运行 Renderer 与组件结构自动测试。

### Task 3: 杭州中心品牌构图

- [x] 桌面 Camera anchor 调整为 `{ x: 0.74, y: 0.34 }`。
- [x] 移动 Camera anchor 调整为 `{ x: 0.66, y: 0.34 }`。
- [x] 保留 Natural Earth 比例和八个产区原始坐标。
- [x] 所有航线、节点和国家统一接受 Camera transform。
- [x] 编写杭州标签 Safe Area 测试。
- [x] 编写 Camera 不改变航线几何的测试。
- [ ] 运行地图坐标和航线自动测试。

### Task 4: Sticky 叙事布局

- [x] 左侧标题、地图和 Progress 保持 sticky。
- [x] 锚点落点与 sticky 顶部共用 `--origins-sticky-gap`。
- [x] 桌面安全区最终设置为 `clamp(70px, 7vh, 88px)`。
- [x] 移动端安全区保持 `12px`。
- [x] 根据安全区同步缩减 sticky 左栏可用高度。
- [x] 用户确认该部分视觉效果收尾。

### Task 5: 双 sentinel 章节切换

- [x] 每章增加 top sentinel 和 bottom sentinel。
- [x] 向下滚动只响应 top sentinel。
- [x] top 激活线保持在视口约 23%。
- [x] 向上滚动只响应 bottom sentinel。
- [x] bottom 激活线动态对齐 `.journey-progress__track`。
- [x] ResizeObserver 和 window resize 会重建 Observer。
- [x] 方向只负责选择 Observer，不负责推测章节边界。
- [x] Observer 使用上下方向区域，避免 sentinel 跳过 1px 感应带。
- [x] 编写双 sentinel 结构测试。
- [ ] 运行双 sentinel 自动测试。

### Task 6: 验证与交付

- [x] 用户确认 Origins 模块可以收尾。
- [x] 明确后续未经用户批准不得自行使用 Chrome。
- [x] 明确后续未经用户批准不得运行测试或构建。
- [ ] 用户批准后运行 `npm.cmd run test:coffee-map`。
- [ ] 用户额外批准后运行 `npm.cmd run build`。
- [ ] 按功能区分类提交 commit。

### Task 7: 云南标签与杭州汇聚收束

- [x] 云南标签使用独立 Label Layer 偏移，放到云南节点下方。
- [x] 不修改云南地理坐标、杭州坐标或 Camera Preset。
- [x] 总结章航线在 700ms 内渐强到统一的咖啡棕层级。
- [x] 八个产区节点按 65ms 间隔依次苏醒。
- [x] 总结章降低国家轮廓存在感。
- [x] 杭州节点执行一次克制的呼吸动画并保持为视觉焦点。
- [x] 杭州标签下方淡入 `Eight Origins. One Cup.` 收束文案。
- [x] 为 reduced motion 提供无错峰、无呼吸的静态状态。
- [x] 编写云南标签和总结章状态的静态结构测试。
- [ ] 运行地图总结章自动测试。

---

## 待批准验证命令

### Coffee Map 测试

```powershell
cd cozy-coffee-web
npm.cmd run test:coffee-map
```

预期覆盖：

- 八个产区和杭州数据完整性。
- Camera 不可变性和 desktop/mobile anchor。
- Safe Area。
- Renderer 与 Camera 解耦。
- 航线几何在 Camera transform 后保持一致。
- CoffeeBeltMap 图层结构。
- Origins sentinel 结构和自然页面滚动约束。

### 生产构建

仅在用户明确额外批准后运行：

```powershell
cd cozy-coffee-web
npm.cmd run build
```

---

## 后续 AI 修改规则

后续 AI 或开发者处理 Origins 模块时必须遵守：

1. 地图整体构图只允许修改 `MAP_CAMERA_PRESETS`。
2. 不得修改单个国家、产区、杭州或航线偏移来修正整体构图。
3. Coffee Belt 属于 Decoration Layer，不跟随 Camera 横向移动。
4. 标签属于 Label Layer，通过 `projectPoint()` 获取屏幕位置。
5. `--origins-sticky-gap` 必须同时用于锚点和 sticky top。
6. 调整顶部留白时，应优先修改 `clamp(70px, 7vh, 88px)`，不得只改 `scroll-margin-top`。
7. 向下必须由 top sentinel 触发，向上必须由 bottom sentinel 触发，不得重新观察整个章节。
8. bottom Observer 激活线必须动态对齐 `.journey-progress__track`。
9. 滚动方向只允许选择 Observer，不允许推测章节边界。
10. 未经用户批准不得使用 Chrome、运行测试或运行构建。
11. commit message 使用标准英文类型前缀与中文描述，例如：

```text
feat: 建立豆源地图相机模型
refactor: 拆分地图图层与章节边界
style: 优化豆源顶部安全区
test: 增加地图相机与章节切换测试
docs: 更新豆源地图执行文档
```

---

## 完成标准

- [x] 杭州成为地图偏右品牌焦点。
- [x] desktop/mobile 使用独立 Camera Preset。
- [x] 国家、航线、节点和标签没有通过修改业务坐标修正布局。
- [x] 顶部安全区在右侧章节滚动时持续保留。
- [x] 向下使用 top sentinel，向上使用与 Progress Track 对齐的 bottom sentinel。
- [x] 地图、章节置灰和航线状态使用同一 active chapter。
- [x] 用户确认当前 Origins 模块收尾。
- [ ] 自动测试通过。
- [ ] 生产构建通过。
- [ ] 当前改动已分类提交。
