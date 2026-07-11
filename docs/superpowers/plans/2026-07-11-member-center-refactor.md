# MemberCenter.vue 拆分方案

## 现状

- 行数：806（template 377 行 / script 253 行 / style 176 行）
- 功能模块：5 个清晰可分的区块（Hero + Stats 合并）
- API 调用：4 处直接调用（`signIn`、`getMonthlyTask`、`getPointsTransactions`、`getSseTicket`）+ 2 处间接通过 `userStore`（`fetchUserInfo`、`fetchMemberInfo`）
- SSE 连接：有（`/api/member/sse/events`，监听 `order_completed` 事件）
- Pinia store 直接访问点：30+ 处（template 与 script 内）
- 路由：仅 `router/index.js` 第 50 行引用，无其他消费者

### 模板结构识别
1. Header + 过期积分提醒（lines 3-18）
2. Hero Card 会员信息卡 + Stats Bar 三栏统计（lines 20-102，合并为一个子组件）
3. Signin Widget 每日签到（lines 104-156）
4. Split Layout：左列积分指南（160-223）/ 右列本月挑战（225-335）
5. Points Detail Modal 积分明细弹窗（338-375，挂在 PointsGuidePanel 内部）

### 跨模块共享状态识别
| 状态 | 共享方 | 处置 |
|---|---|---|
| `userStore.userInfo` | 全部子组件 | 父组件读取后 props 下发 |
| `userStore.userLevel` | 全部子组件 | 父组件读取后 props 下发 |
| `isSignedToday` (computed) | SigninWidget + PointsGuidePanel | **上移到父组件**，props 双传 |
| `nextLevelPoints` (computed) | MemberHeroCard + PointsGuidePanel | **上移到父组件**，props 双传 |
| `profileComplete` (computed) | 仅 PointsGuidePanel | 父组件计算后下发（保留单传） |
| `monthlyTaskData` + 4 个 `isXxxTaskCompleted` | 仅 MonthlyChallengePanel | 完全内聚到子组件 |
| `accelerateProgressPercent` | 仅 MonthlyChallengePanel | 内聚 |
| SSE EventSource | 父组件持有，需触发 MonthlyChallengePanel 刷新 | 子组件 `defineExpose` refresh 方法，父组件用 ref 调用 |

### API 调用分布
| API | 当前调用点 | 拆分后归属 |
|---|---|---|
| `signIn()` | `handleSignIn` | SigninWidget.vue 内部调用，emit `signin-success(result)` 上抛让父组件更新 store |
| `getMonthlyTask()` | `loadMonthlyTaskData` | MonthlyChallengePanel.vue 内部调用 + 自管 polling |
| `getPointsTransactions()` | `openPointsDetailModal` | PointsDetailModal.vue 内部调用（挂在 PointsGuidePanel 内部） |
| `getSseTicket()` | `connectSSE` | MemberCenter.vue 保留（SSE 属容器级协调） |
| `userStore.fetchUserInfo/fetchMemberInfo` | `onMounted` + SSE 回调 | MemberCenter.vue 保留 |

## 拆分方案（5 个子组件）

### 子组件 1: MemberHeroCard.vue
- **职责**：会员信息卡片 + 三栏统计条（合并原 Hero + Stats，二者职责接近，都是会员概览，UI 也连着）
- **对应源行**：template 20-102 / style 650-687
- **props**：`userInfo: Object`, `userLevel: String`, `nextLevelPoints: Number`（父组件计算后传入）
- **emits**：无（纯展示）
- **内部 computed**：`levelName`
- **备注**：徽章 SVG 分支（black/diamond/gold-silver/bean）整体迁入；统计条含成长值 / 连续签到 / 距下一级 三栏
- **图标依赖**：`TrendingUp`, `CalendarCheck`, `Target` (lucide)

### 子组件 2: SigninWidget.vue
- **职责**：每日签到（7 天进度轨道 + 礼包 + 签到按钮）
- **对应源行**：template 104-156 / style 689-711 / script `isSignedToday`/`effectiveSignInDays`/`currentSignInCycleDay`/`handleSignIn`/`getSigninPointsByLevel`（仅签到相关）
- **props**：`userInfo: Object`, `isSignedToday: Boolean`
- **emits**：`signin-success(result)` -- 父组件接收后执行原 store 字段更新与 localStorage 持久化
- **API**：内部调用 `signIn()`
- **关键约束**：`getSigninPointsByLevel()` 返回常量 2，迁入此组件

### 子组件 3: PointsGuidePanel.vue
- **职责**：积分获取渠道（签到/资料/消费三张卡）+ 升级提示 + 促销 banner；内嵌积分明细弹窗触发
- **对应源行**：template 160-223 / style 713-740 / script `profileComplete`/`nextLevelName`/`getConsumeMultiplier`/`openPointsDetailModal` 触发
- **props**：`userInfo: Object`, `userLevel: String`, `isSignedToday: Boolean`, `profileComplete: Boolean`, `nextLevelPoints: Number`
- **emits**：`navigate(path)` -- 路由跳转上抛父组件统一执行（去完善/去下单/查看权益）
- **内部 computed**：`nextLevelName`
- **内部组合**：包含子组件 `PointsDetailModal`（点"明细>"按钮打开，局部状态，不提升到父组件）
- **API**：通过内嵌 `PointsDetailModal` 间接调用 `getPointsTransactions()`

### 子组件 4: MonthlyChallengePanel.vue
- **职责**：本月挑战任务列表（4 任务）+ 黑卡加速包 + 刷新按钮 + 30s 轮询
- **对应源行**：template 225-335 / style 742-777 / script `monthlyTaskData`/4 个 `isXxxTaskCompleted`/`accelerateProgressPercent`/`isRefreshingTask`/`taskRefreshKey`/`loadMonthlyTaskData`/`handleRefreshMonthlyTask`/`startMonthlyTaskPolling`/`stopMonthlyTaskPolling`
- **props**：`userInfo: Object`, `userLevel: String`
- **emits**：无
- **API**：内部调用 `getMonthlyTask()`
- **defineExpose**：`refreshTaskData()` -- 暴露给父组件 SSE 回调调用（等价于原 `loadMonthlyTaskData()`）
- **生命周期**：`onMounted` 自启 `loadMonthlyTaskData` + `startMonthlyTaskPolling`；`onUnmounted` 调 `stopMonthlyTaskPolling`
- **图标依赖**：`CalendarCheck`, `Sun`, `Truck`, `ShoppingBag`, `RefreshCw`

### 子组件 5: PointsDetailModal.vue
- **职责**：积分明细弹窗（余额汇总 + 流水列表）
- **对应源行**：template 338-375 / style 779-803 / script `showPointsDetailModal`/`pointsTransactions`/`isLoadingTransactions`/`getSourceTypeName`/`formatDateTime`/`openPointsDetailModal`
- **props**：`visible: Boolean`（v-model），`userInfo: Object`
- **emits**：`update:visible`
- **API**：内部调用 `getPointsTransactions({ limit: 50 })`
- **图标依赖**：`List`
- **挂载位置**：在 `PointsGuidePanel.vue` 内部使用（局部状态，不提升到父组件）

### MemberCenter.vue 保留内容
- **容器布局**：`dashboard-view` 外壳、`content-header`（标题 + Cozy Day 徽章 + 日期）、`expiring-alert`（过期积分提醒 + 跳转按钮）、`dashboard-split-layout` 双栏网格
- **Pinia store 访问**：`useUserStore` 唯一消费点，`fetchUserInfo/fetchMemberInfo` 在 `onMounted` 调用
- **共享 computed**：`isSignedToday`、`nextLevelPoints`、`profileComplete`、`isCozyDay`（父组件计算后 props 下发）
- **SSE 连接管理**：`connectSSE` / `sseEventSource` / `onUnmounted` 关闭逻辑保留在父组件
- **SSE 回调协调**：监听 `order_completed` 后调用 `userStore.fetchMemberInfo()` + `monthlyChallengePanelRef.value?.refreshTaskData()`
- **路由守卫**：`onMounted` 内 `if (!userStore.isLoggedIn) router.push('/login')`
- **signin-success 处理器**：接收 SigninWidget emit 后执行原 store 字段更新 + localStorage 持久化
- **navigate 处理器**：接收子组件 emit 后 `router.push(path)`

## 实施步骤

1. **创建子组件 1 `MemberHeroCard.vue`**：迁移 template 20-102 + 相关 style（合并 Hero + Stats）；接 props `userInfo/userLevel/nextLevelPoints`；内含 `levelName` computed；导入 TrendingUp/CalendarCheck/Target
2. **创建子组件 2 `SigninWidget.vue`**：迁移 template 104-156 + style + `handleSignIn`/`effectiveSignInDays`/`currentSignInCycleDay`/`getSigninPointsByLevel`；调用 `signIn()` 后 emit `signin-success(result)`；接 props `userInfo/isSignedToday`
3. **创建子组件 5 `PointsDetailModal.vue`**：迁移 template 338-375 + style + `pointsTransactions`/`isLoadingTransactions`/`getSourceTypeName`/`formatDateTime`/`openPointsDetailModal`；v-model `visible`；接 props `userInfo`
4. **创建子组件 3 `PointsGuidePanel.vue`**：迁移 template 160-223 + style + `nextLevelName`/`getConsumeMultiplier`；内嵌 `PointsDetailModal`；emit `navigate(path)`；接 props `userInfo/userLevel/isSignedToday/profileComplete/nextLevelPoints`
5. **创建子组件 4 `MonthlyChallengePanel.vue`**：迁移 template 225-335 + style + `monthlyTaskData`/4 个 `isXxxTaskCompleted`/`accelerateProgressPercent`/`isRefreshingTask`/`taskRefreshKey`/`loadMonthlyTaskData`/`handleRefreshMonthlyTask`/polling；`defineExpose({ refreshTaskData })`；接 props `userInfo/userLevel`
6. **改造 `MemberCenter.vue`**：删除已迁移的 template/style/script 片段；保留容器布局、store 访问、SSE、共享 computed、生命周期；引用 5 个子组件；新增 `signin-success` / `navigate` 事件处理器；新增 `monthlyChallengePanelRef` 用于 SSE 回调
7. **构建验证**：`npm run build` 通过；浏览器手测 6 条主路径 -- 签到、签到后积分增长、月度任务刷新、积分明细弹窗、SSE 推送后任务进度更新、过期积分跳转

## 风险

- **SSE 跨组件触发刷新**：父组件持有 EventSource，子组件 `MonthlyChallengePanel` 自管 taskData。需用 `ref + defineExpose` 让父组件在 `order_completed` 回调里调用子组件的 `refreshTaskData()`。若忘 expose 或 ref 失效，SSE 推送后任务进度不会更新（功能回退但不崩溃）
- **签到后 store 写操作迁移**：原 `handleSignIn` 直接写 `userStore.userInfo.currentPoints/totalPoints/signInDays/consecutiveSignDays/lastSigninDate` + localStorage。迁移后这些写操作上移到父组件的 `onSigninSuccess` 处理器，需确保字段名与原代码逐字一致（注意 `lastSigninDate` 拼写，与 store 中的 `lastSignIn` 不同）
- **共享 computed 双传**：`isSignedToday` 和 `nextLevelPoints` 同时被两个子组件使用，父组件计算一次后分别 props 下发；若子组件各自重复计算会导致状态不一致风险（不推荐）
- **样式作用域**：原文件 `<style scoped>` 是单一作用域。拆分后每个子组件需自带相关样式片段，且部分依赖全局选择器（如 `.dashboard-split-layout` 的 grid 模板）。子组件根元素 class 需保留以维持布局
- **Lucide 图标重复导入**：`CalendarCheck` 同时被 HeroCard 和 MonthlyChallengePanel 使用，需在各自组件 import（不可跨组件共享 import）
- **路由跳转集中化**：原代码 4 处 `router.push` 散落在不同位置（过期积分/去完善/去下单/查看权益）。迁移后通过 `navigate` emit 集中到父组件，需逐一核对路径字符串
- **Polling 生命周期**：原 polling 在父组件 `onMounted` 启动。迁移到 MonthlyChallengePanel 后由子组件自管；需确认子组件 `onMounted` 时机不晚于父组件 SSE 连接建立（Vue 子组件 mount 早于父组件 mount，安全）
- **`taskRefreshKey` 触发动画**：原代码用 `:key="taskRefreshKey"` 强制重渲染 task-list-premium。迁移后此 ref 内聚到 MonthlyChallengePanel，行为不变

## 关键决策摘要

文件 806 行拆为 1 容器 + 5 个子组件。父组件 MemberCenter.vue 独占 Pinia store 访问权、SSE 连接、共享 computed（isSignedToday/nextLevelPoints/profileComplete）、路由守卫与跳转。子组件 props 接收 userInfo/userLevel，emits 上报 signin-success/navigate 事件。API 调用留在最相关子组件内（signIn->SigninWidget、getMonthlyTask->MonthlyChallengePanel、getPointsTransactions->PointsDetailModal 挂在 PointsGuidePanel 内），仅 getSseTicket 留父组件。MonthlyChallengePanel 自管 30s polling，通过 defineExpose 暴露 refreshTaskData 给父组件 SSE 回调调用。HeroCard 合并 StatsBar（都是会员概览，UI 连着）。PointsDetailModal 继续挂 PointsGuidePanel 内部（局部状态，不提升）。预期 MemberCenter.vue 从 806 行降至约 200 行。
