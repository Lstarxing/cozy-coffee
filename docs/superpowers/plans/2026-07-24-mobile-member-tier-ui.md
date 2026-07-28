# 移动端会员等级主题系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在移动端 3 张会员专属卡片上还原 Web 端每级换肤效果，并提供 dev 悬浮按钮切换 5 个会员等级以便测试。

**Architecture:** SCSS token + JS theme map (`MEMBER_LEVEL_THEMES`) + `useMemberTheme` composable 注入卡片级 CSS 变量；store 的 `memberInfo` 从 ref 改 computed，dev 覆盖走独立 `devOverride` 通道，真实数据与测试数据分离；DevLevelSwitcher 仅 dev 构建渲染。

**Tech Stack:** uni-app 3.x、Vue 3.4 `<script setup>`、Pinia 2.1.7（Setup Store）、Vitest 2.x、SCSS。

## Global Constraints

- **设计禁令**（`src/uni.scss` 头部注释）：禁渐变文字、禁奶油底（大面积低对比米黄）、禁 Glassmorphism、禁卡片套卡片。渐变只用于卡片背景；black 金色文字用纯色不用 `background-clip:text`。
- **Pinia 固定 2.1.7**，不单独升级 Vue/Pinia/vue-router。
- **Store 命名**：本次新增代码（composable、DevLevelSwitcher）统一用 `useSessionStore`（`@/stores/session`），不引入 `useUserStore` 别名。既有页面 import 不动。
- **小程序 CSS 变量**：不用 `:root`，改卡片级 `:style` 注入 `--member-surface/--member-text/--member-accent`。
- **DEV 守卫**：所有 dev 通道（`setDevLevel`/`restoreDevOverride`/DevLevelSwitcher）均用 `import.meta.env.DEV` 守卫，生产构建强制失效。
- **commit message**：中文，`feat`/`refactor` 前缀（遵循用户全局偏好）。
- **运行命令**：`cd C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-mobile` 后 `npx vitest run`。

---

## File Structure

**新增文件：**
- `src/composables/useMemberTheme.js` -- 3 张卡共用，返回 `themeStyle`/`levelTheme`/`isDark`/`level`。
- `src/components/member/LevelBadge.vue` -- 5 等级 SVG 徽章，`color`/`size` prop。
- `src/components/dev/DevLevelSwitcher.vue` -- dev 悬浮等级切换器。
- `src/stores/session.test.js` -- store 改造的单元测试（dev 覆盖优先、clear、生产清 storage）。

**修改文件：**
- `src/uni.scss` -- 新增 5 套 `$member-*` token + `$member-black-pattern`。
- `src/constants/member.js` -- 新增 `MEMBER_LEVEL_THEMES` + `DEV_MEMBER_MOCK`。
- `src/stores/session.js` -- `memberInfo` ref -> computed、`devOverride`、新增 3 个 action、`restoreDevOverride` 加 DEV 守卫。
- `src/pages/index/index.vue` -- `.member-panel` 绑 themeStyle + per-level SCSS + LevelBadge。
- `src/pages/profile/profile.vue` -- `.member-hero` 绑 themeStyle + per-level SCSS + LevelBadge。
- `src/pages/benefits/index.vue` -- `.current-level` 补 per-level SCSS + LevelBadge。
- `src/App.vue` -- 根挂载 `<DevLevelSwitcher />`。

---

## Task 1: 会员等级主题 token 与主题映射

**Files:**
- Modify: `src/uni.scss`（末尾追加）
- Modify: `src/constants/member.js`（末尾追加）
- Test: `src/constants/member.test.js`（新建）

**Interfaces:**
- Produces: `MEMBER_LEVEL_THEMES`（`{ [level]: { surface, text, accent, isDark } }`）、`DEV_MEMBER_MOCK`（`{ [level]: {...MemberDTO 子集} }`）、SCSS token `$member-{level}-{bg|bg-start|bg-end|ink|accent}` + `$member-black-pattern`。

- [ ] **Step 1: 在 `uni.scss` 末尾追加会员等级 token**

打开 `src/uni.scss`，在第 115 行（`$tabbar-height: 100rpx;`）之后追加：

```scss

// ==================== 会员等级配色（仅会员专属卡片使用）====================
$member-basic-bg: #F7F5F3;
$member-basic-ink: #2B1E16;
$member-basic-accent: #8D6E63;

$member-silver-bg-start: #ECE8E4;
$member-silver-bg-end: #D6D0CA;
$member-silver-ink: #3E342E;
$member-silver-accent: #8C7B70;

$member-gold-bg-start: #E8D7A5;
$member-gold-bg-end: #C9A45C;
$member-gold-ink: #3B2920;
$member-gold-accent: #B8862D;

$member-diamond-bg-start: #DCE4E6;
$member-diamond-bg-end: #B8C6CA;
$member-diamond-ink: #263238;
$member-diamond-accent: #546E7A;

$member-black-bg: #171411;
$member-black-ink: #E6C97A;
$member-black-accent: #C9A227;
$member-black-pattern: rgba(201, 162, 39, 0.08);
```

注意：**不**添加 `:root { --member-... }` 块。变量改为卡片级 `:style` 注入（Task 4）。

- [ ] **Step 2: 在 `constants/member.js` 末尾追加主题映射与 mock**

打开 `src/constants/member.js`，在文件末尾（第 21 行 `getMemberLevelName` 函数之后）追加：

```js

// 会员等级主题映射：surface（卡背景，可纯色可渐变）、text（文字色）、accent（进度条/徽章）、isDark（暗背景标记）
// CSS 变量在卡片级通过 :style 注入（见 useMemberTheme），不暴露全局 :root
export const MEMBER_LEVEL_THEMES = Object.freeze({
  basic: {
    surface: '#F7F5F3',
    text: '#2B1E16',
    accent: '#8D6E63',
    isDark: false
  },
  silver: {
    surface: 'linear-gradient(135deg, #ECE8E4, #D6D0CA)',
    text: '#3E342E',
    accent: '#8C7B70',
    isDark: false
  },
  gold: {
    surface: 'linear-gradient(135deg, #E8D7A5, #C9A45C)',
    text: '#3B2920',
    accent: '#B8862D',
    isDark: false
  },
  diamond: {
    surface: 'linear-gradient(135deg, #DCE4E6, #B8C6CA)',
    text: '#263238',
    accent: '#546E7A',
    isDark: false
  },
  black: {
    surface: '#171411',
    text: '#E6C97A',
    accent: '#C9A227',
    isDark: true
  }
})

// dev 测试覆盖用的 mock memberInfo，尽量接近 MemberDTO 形状
export const DEV_MEMBER_MOCK = Object.freeze({
  basic:   { id: 90001, memberLevel: 'basic',   levelName: '基础会员', expTotal: 100,  currentPoints: 50,    couponCount: 0, exchangeCouponCount: 0 },
  silver:  { id: 90002, memberLevel: 'silver',  levelName: '白银会员', expTotal: 800,  currentPoints: 1200,  couponCount: 2, exchangeCouponCount: 1 },
  gold:    { id: 90003, memberLevel: 'gold',    levelName: '黄金会员', expTotal: 2000, currentPoints: 3600,  couponCount: 3, exchangeCouponCount: 2 },
  diamond: { id: 90004, memberLevel: 'diamond', levelName: '钻石会员', expTotal: 4500, currentPoints: 8888,  couponCount: 5, exchangeCouponCount: 3 },
  black:   { id: 90005, memberLevel: 'black',   levelName: '黑金会员', expTotal: 9500, currentPoints: 18888, couponCount: 8, exchangeCouponCount: 5 }
})
```

注意：`surface` 直接写 hex/gradient 字符串，不经 SCSS 变量（JS 无法读 SCSS 变量）。SCSS 的 `$member-*` token 供组件内 SCSS 直接引用（如 black 纹理 `radial-gradient(#{$member-black-pattern} ...)`），两者值保持一致。

- [ ] **Step 3: 写测试验证主题映射结构**

创建 `src/constants/member.test.js`：

```js
import { describe, expect, it } from 'vitest'
import { MEMBER_LEVELS, MEMBER_LEVEL_THEMES, DEV_MEMBER_MOCK } from './member'

describe('member level themes', () => {
  it('every level has a complete theme', () => {
    for (const level of MEMBER_LEVELS) {
      const theme = MEMBER_LEVEL_THEMES[level]
      expect(theme).toBeDefined()
      expect(typeof theme.surface).toBe('string')
      expect(typeof theme.text).toBe('string')
      expect(typeof theme.accent).toBe('string')
      expect(typeof theme.isDark).toBe('boolean')
    }
  })

  it('black is the only dark theme', () => {
    const darks = MEMBER_LEVELS.filter(l => MEMBER_LEVEL_THEMES[l].isDark)
    expect(darks).toEqual(['black'])
  })
})

describe('dev member mock', () => {
  it('every level has a mock matching memberLevel', () => {
    for (const level of MEMBER_LEVELS) {
      const mock = DEV_MEMBER_MOCK[level]
      expect(mock).toBeDefined()
      expect(mock.memberLevel).toBe(level)
      expect(typeof mock.levelName).toBe('string')
      expect(mock.expTotal).toBeGreaterThan(0)
      expect(mock.currentPoints).toBeGreaterThanOrEqual(0)
    }
  })

  it('diamond mock exp clears the diamond threshold', () => {
    expect(DEV_MEMBER_MOCK.diamond.expTotal).toBeGreaterThanOrEqual(4000)
  })
})
```

- [ ] **Step 4: 运行测试**

```bash
npx vitest run src/constants/member.test.js
```
Expected: PASS（3 tests）。

- [ ] **Step 5: 全量测试确认无回归**

```bash
npx vitest run
```
Expected: 全部 PASS（现有 32 + 新增 3 = 35）。

- [ ] **Step 6: Commit**

```bash
cd C:/Users/dell/Desktop/CozyCoffee/cozy-coffee && git -C cozy-coffee-mobile add src/uni.scss src/constants/member.js src/constants/member.test.js 2>/dev/null || (cd cozy-coffee-mobile && git add src/uni.scss src/constants/member.js src/constants/member.test.js)
git commit -m "feat(member): 新增会员等级主题 token 与主题映射"
```
注：仓库根在 `cozy-coffee/`，移动端在 `cozy-coffee/cozy-coffee-mobile/`。若 mobile 不是独立 git 仓库，则在 `cozy-coffee/` 根提交。按用户记忆，commit 在 `cozy-coffee` 仓库内。

---

## Task 2: session store 支持 dev 等级覆盖

**Files:**
- Modify: `src/stores/session.js`
- Test: `src/stores/session.test.js`（新建）

**Interfaces:**
- Consumes: `DEV_MEMBER_MOCK`（Task 1 产出）。
- Produces: store 暴露 `memberInfo`（computed）、`userLevel`（computed）、`setDevLevel(level)`、`clearDevOverride()`、`refreshMemberInfo()`、`devOverride`（ref，dev 切换器读）。

**关键约束**：`session.js` 是 Pinia Setup Store（`defineStore('session', () => {...})`）。`memberInfo` 从 ref 改 computed 后，对外访问签名不变（`store.memberInfo` 自动 unwrap），页面零改动。

- [ ] **Step 1: 写失败测试 -- dev 覆盖优先**

创建 `src/stores/session.test.js`：

```js
import { beforeEach, describe, expect, it, vi } from 'vitest'

// 在导入 store 前先 stub uni 全局与 DEV 标记
const storage = new Map()
vi.stubGlobal('uni', {
  getStorageSync: (k) => storage.get(k) ?? '',
  setStorageSync: (k, v) => storage.set(k, v),
  removeStorageSync: (k) => storage.delete(k)
})
vi.stubGlobal('import', { meta: { env: { DEV: true } } })

const { useSessionStore } = await import('./session')
const { DEV_MEMBER_MOCK } = await import('@/constants/member')

beforeEach(() => { storage.clear() })

describe('session store dev member override', () => {
  it('memberInfo reflects devOverride when set', () => {
    const store = useSessionStore()
    store.setDevLevel('diamond')
    expect(store.userLevel).toBe('diamond')
    expect(store.memberInfo.expTotal).toBe(DEV_MEMBER_MOCK.diamond.expTotal)
    expect(store.memberInfo.currentPoints).toBe(DEV_MEMBER_MOCK.diamond.currentPoints)
  })

  it('clearDevOverride restores real member info', () => {
    const store = useSessionStore()
    store.setDevLevel('gold')
    expect(store.userLevel).toBe('gold')
    store.clearDevOverride()
    expect(store.userLevel).toBe('basic')  // realMemberInfo 默认 basic
  })

  it('setMemberInfo writes realMemberInfo without clobbering dev override', () => {
    const store = useSessionStore()
    store.setDevLevel('black')
    store.setMemberInfo({ expTotal: 9999, memberLevel: 'basic' })  // 模拟后端回写
    // dev 覆盖仍优先
    expect(store.userLevel).toBe('black')
    // 清除覆盖后露出真实数据
    store.clearDevOverride()
    expect(store.memberInfo.expTotal).toBe(9999)
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

```bash
npx vitest run src/stores/session.test.js
```
Expected: FAIL（`setDevLevel is not a function` 或 `memberInfo` 不是预期值）。

- [ ] **Step 3: 改造 `src/stores/session.js`**

用以下内容完整替换 `src/stores/session.js`：

```js
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { logout as logoutApi } from '@/api/auth'
import { DEV_MEMBER_MOCK } from '@/constants/member'

const emptyUser = () => ({
  id: null,
  username: '',
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  birthday: '',
  inviteCode: '',
  hasAppliedInviteCode: false
})
const emptyMember = () => ({ memberLevel: 'basic', currentPoints: 0, totalPoints: 0, expTotal: 0, couponCount: 0 })

function readJson(key, fallback) {
  try {
    const value = uni.getStorageSync(key)
    return value ? JSON.parse(value) : fallback
  } catch (_) {
    return fallback
  }
}

export const useSessionStore = defineStore('session', () => {
  const token = ref('')
  const userInfo = ref(emptyUser())
  const realMemberInfo = ref(emptyMember())
  const devOverride = ref(null)
  const restored = ref(false)

  const memberInfo = computed(() =>
    devOverride.value
      ? (DEV_MEMBER_MOCK[devOverride.value] || realMemberInfo.value)
      : realMemberInfo.value
  )
  const isLoggedIn = computed(() => Boolean(token.value))
  const isAuthenticated = isLoggedIn
  const userLevel = computed(() => memberInfo.value.memberLevel || 'basic')

  function restoreDevOverride() {
    if (!import.meta.env.DEV) {
      uni.removeStorageSync('dev_member_override')
      return
    }
    const saved = uni.getStorageSync('dev_member_override')
    if (saved && DEV_MEMBER_MOCK[saved]) devOverride.value = saved
  }

  function restore() {
    token.value = uni.getStorageSync('token') || ''
    userInfo.value = { ...emptyUser(), ...readJson('userInfo', {}) }
    realMemberInfo.value = { ...emptyMember(), ...readJson('memberInfo', {}) }
    restoreDevOverride()
    restored.value = true
    return isLoggedIn.value
  }

  function setLoginInfo(tokenValue, user = {}) {
    token.value = tokenValue || ''
    userInfo.value = { ...userInfo.value, ...user }
    uni.setStorageSync('token', token.value)
    uni.setStorageSync('userInfo', JSON.stringify(userInfo.value))
  }

  function setMemberInfo(info = {}) {
    realMemberInfo.value = { ...realMemberInfo.value, ...info }
    uni.setStorageSync('memberInfo', JSON.stringify(realMemberInfo.value))
  }

  function setDevLevel(level) {
    if (!import.meta.env.DEV) return
    devOverride.value = level
    uni.setStorageSync('dev_member_override', level)
  }

  function clearDevOverride() {
    devOverride.value = null
    uni.removeStorageSync('dev_member_override')
  }

  async function refreshMemberInfo() {
    try {
      const { getMemberInfo } = await import('@/api/member')
      const res = await getMemberInfo()
      const data = res?.data ?? res
      if (data) setMemberInfo(data)
    } catch (_) {
      // 静默失败：dev 恢复时后端可能不可达
    }
  }

  function clearSession() {
    token.value = ''
    userInfo.value = emptyUser()
    realMemberInfo.value = emptyMember()
    devOverride.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('memberInfo')
    uni.removeStorageSync('dev_member_override')
  }

  async function logout() {
    try {
      if (token.value) await logoutApi()
    } finally {
      clearSession()
    }
  }

  restore()

  return {
    token,
    userInfo,
    memberInfo,
    realMemberInfo,
    devOverride,
    restored,
    isLoggedIn,
    isAuthenticated,
    userLevel,
    restore,
    setLoginInfo,
    setMemberInfo,
    setDevLevel,
    clearDevOverride,
    refreshMemberInfo,
    clearSession,
    logout
  }
})
```

- [ ] **Step 4: 运行测试确认通过**

```bash
npx vitest run src/stores/session.test.js
```
Expected: PASS（3 tests）。

- [ ] **Step 5: 全量测试确认无回归**

```bash
npx vitest run
```
Expected: 全部 PASS。重点确认既有调用 `userStore.memberInfo.xxx` 的页面相关逻辑（若有测试）仍通过。

- [ ] **Step 6: Commit**

```bash
cd C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-mobile && git add src/stores/session.js src/stores/session.test.js
git commit -m "refactor(session): 支持会员等级开发覆盖"
```

---

## Task 3: useMemberTheme composable + LevelBadge 组件

**Files:**
- Create: `src/composables/useMemberTheme.js`
- Create: `src/components/member/LevelBadge.vue`

**Interfaces:**
- Consumes: `useSessionStore`（Task 2）、`MEMBER_LEVEL_THEMES`（Task 1）。
- Produces: `useMemberTheme()` 返回 `{ themeStyle, levelTheme, isDark, level }`；`<LevelBadge :level color size />`。

- [ ] **Step 1: 创建 `useMemberTheme`**

创建 `src/composables/useMemberTheme.js`：

```js
import { computed } from 'vue'
import { useSessionStore } from '@/stores/session'
import { MEMBER_LEVEL_THEMES } from '@/constants/member'

export function useMemberTheme() {
  const store = useSessionStore()
  const level = computed(() => store.userLevel)
  const levelTheme = computed(() => MEMBER_LEVEL_THEMES[level.value] || MEMBER_LEVEL_THEMES.basic)
  const themeStyle = computed(() => ({
    '--member-surface': levelTheme.value.surface,
    '--member-text': levelTheme.value.text,
    '--member-accent': levelTheme.value.accent
  }))
  const isDark = computed(() => Boolean(levelTheme.value.isDark))
  return { themeStyle, levelTheme, isDark, level }
}
```

- [ ] **Step 2: 创建 LevelBadge 组件**

创建 `src/components/member/LevelBadge.vue`：

```vue
<template>
  <view class="level-badge" :style="{ color, width: size + 'rpx', height: size + 'rpx' }">
    <!-- basic: 咖啡豆 -->
    <view v-if="level === 'basic'" class="svg">
      <view class="bean" />
    </view>
    <!-- silver: 勋章 -->
    <view v-else-if="level === 'silver'" class="svg medal" />
    <!-- gold: 星 -->
    <view v-else-if="level === 'gold'" class="svg star" />
    <!-- diamond: 钻石 -->
    <view v-else-if="level === 'diamond'" class="svg diamond" />
    <!-- black: 王冠 -->
    <view v-else-if="level === 'black'" class="svg crown" />
  </view>
</template>

<script setup>
defineProps({
  level: { type: String, default: 'basic' },
  color: { type: String, default: 'currentColor' },
  size: { type: Number, default: 48 }
})
</script>

<style lang="scss" scoped>
.level-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
}
.svg { width: 100%; height: 100%; }
// 咖啡豆: 椭圆 + 中缝
.bean { width: 60%; height: 80%; border: 3rpx solid currentColor; border-radius: 50%; position: relative; margin: auto; transform: rotate(15deg); }
.bean::after { content: ''; position: absolute; left: 50%; top: 8%; bottom: 8%; width: 2rpx; background: currentColor; }
// 勋章: 圆 + 下飘带
.medal { width: 70%; height: 70%; border: 3rpx solid currentColor; border-radius: 50%; margin: auto; position: relative; }
.medal::after { content: ''; position: absolute; bottom: -30%; left: 10%; right: 10%; height: 40%; border: 3rpx solid currentColor; border-top: none; clip-path: polygon(0 0, 100% 0, 80% 100%, 50% 70%, 20% 100%); }
// 星: 5 角星用 clip-path
.star { width: 75%; height: 75%; margin: auto; background: currentColor; clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%); }
// 钻石: 菱形
.diamond { width: 60%; height: 75%; margin: auto; background: currentColor; clip-path: polygon(50% 0%, 100% 40%, 50% 100%, 0% 40%); }
// 王冠: 梯形 + 3 个尖
.crown { width: 75%; height: 55%; margin: auto; background: currentColor; clip-path: polygon(0% 100%, 0% 50%, 20% 70%, 30% 0%, 50% 50%, 70% 0%, 80% 70%, 100% 50%, 100% 100%); }
</style>
```

注：小程序不支持内联 `<svg>` 标签，用 `view` + `clip-path` / `border` 模拟线性图标。`currentColor` 跟随 `color` prop（即 `var(--member-accent)`）。

- [ ] **Step 3: 确认文件可被解析（lint/编译检查）**

```bash
npx vitest run
```
Expected: 全部 PASS（这两个文件无独立测试，但不影响现有测试；若 vitest 报模块解析错误则需修 import 路径）。

- [ ] **Step 4: Commit**

```bash
cd C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-mobile && git add src/composables/useMemberTheme.js src/components/member/LevelBadge.vue
git commit -m "feat(member): 新增主题 composable 与等级徽章组件"
```

---

## Task 4: 三张卡应用等级主题

**Files:**
- Modify: `src/pages/index/index.vue`（`.member-panel`，约 69-92 行 + `<style>`）
- Modify: `src/pages/profile/profile.vue`（`.member-hero`，约 4-41 行 + `<style>`）
- Modify: `src/pages/benefits/index.vue`（`.current-level`，约 7 行 + `<style>`）

**Interfaces:**
- Consumes: `useMemberTheme`（Task 3）、`LevelBadge`（Task 3）。

**关键点**：三处都要 (1) 引入 `useMemberTheme` + `LevelBadge`，(2) 容器加 `:style="themeStyle"` + `:class="userLevel"`，(3) SCSS 改为消费 `var(--member-*)`，(4) 加 LevelBadge。

- [ ] **Step 1: 改 `pages/index/index.vue` 的 `.member-panel`**

打开 `src/pages/index/index.vue`。

(a) 在 `<script setup>` 顶部 import 区（约 145-147 行附近）加：

```js
import { useMemberTheme } from '@/composables/useMemberTheme'
import LevelBadge from '@/components/member/LevelBadge.vue'

const { themeStyle, isDark, level: themeLevel } = useMemberTheme()
```

(b) 模板第 69 行 `<view class="member-panel" @click="handleMemberClick">` 改为：

```html
<view class="member-panel" :class="[themeLevel, { 'is-dark': isDark }]" :style="themeStyle" @click="handleMemberClick">
```

(c) 在 `.member-head` 内（约 70-76 行），`<view>` 包含 member-brand 的那个 view 内，在 `<text class="member-level ...">` 之前插入 LevelBadge：

```html
<view>
  <view class="member-badge-row">
    <LevelBadge :level="themeLevel" color="var(--member-accent)" :size="40" />
    <text class="member-brand">COZY MEMBER</text>
  </view>
  <text class="member-level cozy-display">{{ isLoggedIn ? getLevelName(userLevel) : '会员日常，从一杯开始' }}</text>
</view>
```

(d) 在 `<style>` 中找到 `.member-panel` 规则，改为：

```scss
.member-panel {
  position: relative;
  overflow: hidden;
  margin: 32rpx 28rpx 0;
  padding: 30rpx;
  border-radius: $cozy-radius-lg;
  background: var(--member-surface, #{$cozy-surface-alt});
  color: var(--member-text, #{$cozy-on-dark});
  transition: background $cozy-duration $cozy-ease-out;
}
.member-panel > * { position: relative; z-index: 1; }
.member-panel .member-badge-row { display: flex; align-items: center; gap: 12rpx; }
.member-panel .progress-fill { background: var(--member-accent, #{$cozy-accent}); }
.member-panel.is-dark.black::before {
  content: '';
  position: absolute; inset: 0; z-index: 0;
  background-image: radial-gradient(#{$member-black-pattern} 1px, transparent 1px);
  background-size: 18rpx 18rpx;
  pointer-events: none;
}
```

- [ ] **Step 2: 改 `pages/profile/profile.vue` 的 `.member-hero`**

打开 `src/pages/profile/profile.vue`。

(a) import 区（约 128-138 行）加：

```js
import { useMemberTheme } from '@/composables/useMemberTheme'
import LevelBadge from '@/components/member/LevelBadge.vue'

const { themeStyle, isDark, level: themeLevel } = useMemberTheme()
```

(b) 第 4 行 `<view class="member-hero">` 改为：

```html
<view class="member-hero" :class="[themeLevel, { 'is-dark': isDark }]" :style="themeStyle">
```

(c) 第 7 行 `COZY MEMBER` 那个 view 内，`<text class="hero-brand">` 前加 LevelBadge：

```html
<view class="hero-brand-row">
  <LevelBadge :level="themeLevel" color="var(--member-accent)" :size="36" />
  <text class="hero-brand">COZY MEMBER</text>
</view>
```

(d) `<style>` 中 `.member-hero` 改为消费变量（保留原 layout，只换背景/文字/进度条色源）：

```scss
.member-hero {
  position: relative;
  overflow: hidden;
  padding: /* 保留原 padding 值 */;
  background: var(--member-surface, #{$cozy-surface-alt});
  color: var(--member-text, #{$cozy-on-dark});
}
.member-hero > * { position: relative; z-index: 1; }
.member-hero .hero-brand-row { display: flex; align-items: center; gap: 12rpx; }
.member-hero .progress-fill { background: var(--member-accent, #{$cozy-accent}); }
.member-hero.is-dark.black::before {
  content: '';
  position: absolute; inset: 0; z-index: 0;
  background-image: radial-gradient(#{$member-black-pattern} 1px, transparent 1px);
  background-size: 18rpx 18rpx;
  pointer-events: none;
}
```
注：原 `.member-hero` 的 padding 等其他属性保留，只改 `background`/`color` 两行并加 `position:relative; overflow:hidden;`。实现时读原文件确认 padding 值，不要丢失。

- [ ] **Step 3: 改 `pages/benefits/index.vue` 的 `.current-level`**

打开 `src/pages/benefits/index.vue`。该页已 `:class="currentLevel"`（第 7 行），只需补 SCSS + LevelBadge。

(a) import 区加：

```js
import { useMemberTheme } from '@/composables/useMemberTheme'
import LevelBadge from '@/components/member/LevelBadge.vue'

const { themeStyle, isDark, level: themeLevel } = useMemberTheme()
```

(b) 第 7 行 `<view class="current-level" :class="currentLevel">` 改为：

```html
<view class="current-level" :class="[currentLevel, { 'is-dark': isDark }]" :style="themeStyle">
```

(c) `.level-info` 内 `<text class="level-name">` 前加 LevelBadge：

```html
<view class="level-info">
  <LevelBadge :level="themeLevel" color="var(--member-accent)" :size="44" />
  <text class="level-name">{{ getLevelName(currentLevel) }}</text>
  <text class="level-desc">{{ getLevelDesc(currentLevel) }}</text>
</view>
```

(d) `<style>` 中 `.current-level` 改为：

```scss
.current-level {
  position: relative;
  overflow: hidden;
  /* 保留原 padding/margin/radius */
  background: var(--member-surface, #{$cozy-surface-alt});
  color: var(--member-text, #{$cozy-on-dark});
}
.current-level > * { position: relative; z-index: 1; }
.current-level .progress-fill { background: var(--member-accent, #{$cozy-accent}); }
.current-level.is-dark.black::before {
  content: '';
  position: absolute; inset: 0; z-index: 0;
  background-image: radial-gradient(#{$member-black-pattern} 1px, transparent 1px);
  background-size: 18rpx 18rpx;
  pointer-events: none;
}
```

- [ ] **Step 4: 全量测试**

```bash
npx vitest run
```
Expected: 全部 PASS。

- [ ] **Step 5: 手动构建验证 mp-weixin 可编译**

```bash
npx uni build -p mp-weixin
```
Expected: 构建成功，无 SCSS 报错。检查 `dist/build/mp-weixin` 生成。

- [ ] **Step 6: Commit**

```bash
cd C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-mobile && git add src/pages/index/index.vue src/pages/profile/profile.vue src/pages/benefits/index.vue
git commit -m "feat(member): 三张会员卡应用等级主题"
```

---

## Task 5: DevLevelSwitcher 切换器 + 挂载

**Files:**
- Create: `src/components/dev/DevLevelSwitcher.vue`
- Modify: `src/App.vue`（template 根挂载）

**Interfaces:**
- Consumes: `useSessionStore`（Task 2）、`MEMBER_LEVELS`/`MEMBER_LEVEL_NAMES`（既有）。
- Produces: 全页面 dev 悬浮切换器，调用 `store.setDevLevel` / `clearDevOverride` / `refreshMemberInfo`。

- [ ] **Step 1: 创建 DevLevelSwitcher 组件**

创建 `src/components/dev/DevLevelSwitcher.vue`：

```vue
<template>
  <view v-if="isDev" class="dev-switcher">
    <view class="dev-fab" @click="open = true">
      <text class="dev-fab-glyph">⚙️</text>
    </view>
    <view v-if="open" class="dev-mask" @click="open = false">
      <view class="dev-sheet" @click.stop>
        <view class="dev-head">
          <text class="dev-title">开发模式 · 当前: {{ currentLabel }}</text>
          <text class="dev-close" @click="open = false">×</text>
        </view>
        <view class="dev-source">来源: {{ store.devOverride ? '● DEV MOCK' : '○ API' }}</view>
        <view class="dev-meta">EXP: {{ memberInfo.expTotal }} · 积分: {{ memberInfo.currentPoints }}</view>
        <view class="dev-grid">
          <view
            v-for="lvl in LEVELS"
            :key="lvl"
            class="dev-btn"
            :class="[lvl, { active: currentLevel === lvl }]"
            @click="select(lvl)"
          >
            <text>{{ LABELS[lvl] }}</text>
          </view>
        </view>
        <view class="dev-restore" @click="restore">
          <text>恢复真实等级</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useSessionStore } from '@/stores/session'
import { MEMBER_LEVELS, MEMBER_LEVEL_NAMES } from '@/constants/member'

const isDev = import.meta.env.DEV
const store = useSessionStore()
const open = ref(false)
const LEVELS = MEMBER_LEVELS
const LABELS = MEMBER_LEVEL_NAMES
const currentLevel = computed(() => store.userLevel)
const memberInfo = computed(() => store.memberInfo)
const currentLabel = computed(() => MEMBER_LEVEL_NAMES[currentLevel.value] || currentLevel.value)

function select(lvl) {
  store.setDevLevel(lvl)
  open.value = false
}
function restore() {
  store.clearDevOverride()
  store.refreshMemberInfo()
  open.value = false
}
</script>

<style lang="scss" scoped>
.dev-switcher { position: fixed; z-index: 9999; }
.dev-fab {
  position: fixed; right: 32rpx; bottom: calc(180rpx + env(safe-area-inset-bottom));
  width: 88rpx; height: 88rpx; border-radius: 50%;
  background: rgba(44, 30, 24, 0.88); display: flex; align-items: center; justify-content: center;
  box-shadow: $cozy-shadow-raised;
}
.dev-fab-glyph { font-size: 40rpx; }
.dev-mask {
  position: fixed; inset: 0; background: $cozy-overlay; z-index: 9998;
  display: flex; align-items: flex-end;
}
.dev-sheet {
  width: 100%; background: $cozy-bg; border-radius: $cozy-radius-lg $cozy-radius-lg 0 0;
  padding: 32rpx 28rpx calc(48rpx + env(safe-area-inset-bottom));
}
.dev-head { display: flex; align-items: center; justify-content: space-between; }
.dev-title { font-size: $font-size-md; font-weight: 700; color: $cozy-ink; }
.dev-close { font-size: 44rpx; color: $cozy-muted; padding: 0 12rpx; }
.dev-source { margin-top: 12rpx; font-size: $font-size-xs; color: $cozy-muted; }
.dev-meta { margin-top: 4rpx; font-size: $font-size-xs; color: $cozy-muted; }
.dev-grid { margin-top: 24rpx; display: flex; flex-wrap: wrap; gap: 16rpx; }
.dev-btn {
  flex: 1 1 calc(33.333% - 16rpx); min-width: 180rpx; height: 88rpx;
  border-radius: $cozy-radius-md; display: flex; align-items: center; justify-content: center;
  background: $cozy-surface; color: $cozy-muted; font-size: $font-size-sm; font-weight: 600;
}
.dev-btn.active { background: $cozy-primary; color: #fff; }
.dev-btn.basic.active { background: #8D6E63; }
.dev-btn.silver.active { background: #8C7B70; }
.dev-btn.gold.active { background: #B8862D; }
.dev-btn.diamond.active { background: #546E7A; }
.dev-btn.black.active { background: #171411; color: #E6C97A; }
.dev-restore {
  margin-top: 24rpx; height: 88rpx; border-radius: $cozy-radius-md;
  border: 1rpx solid $cozy-border; display: flex; align-items: center; justify-content: center;
  color: $cozy-ink; font-size: $font-size-sm;
}
</style>
```

- [ ] **Step 2: 在 App.vue 根挂载**

打开 `src/App.vue`。

(a) `<script setup>` import 区（约第 9-14 行）末尾加：

```js
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'
```

(b) 在 `<template>` 根 view 内（App.vue 当前 template 是 `<view>` 包裹或直接是根），在最后加：

```html
<DevLevelSwitcher />
```
注：App.vue 的 template 在 uni-app 中通常是空根 + 各页面独立。确认 App.vue 是否有 `<template>` 块；若没有显式 template（uni-app 默认 App.vue 不渲染自身模板，页面由 pages.json 驱动），则**改为在每个页面挂载不现实**。

**正确做法**：uni-app 的 `App.vue` 模板不渲染页面级内容。DevLevelSwitcher 需作为**全局组件**用 `vue-plugin` 或在每个 tab 页面单独引入。

**调整方案（优先）**：在 3 个会员相关页面（index/profile/benefits）的 template 根末尾各加 `<DevLevelSwitcher />` 并 import，这样 dev 切换时切回这几个页面即可看到效果，且最简单。步骤改为：

- `src/pages/index/index.vue`：import + template 末尾加 `<DevLevelSwitcher />`
- `src/pages/profile/profile.vue`：同上
- `src/pages/benefits/index.vue`：同上

这样无需改 App.vue，blast radius 更小。DevLevelSwitcher 自己 `v-if="isDev"` 控制生产剔除。

- [ ] **Step 3: 在 3 个会员页面挂载 DevLevelSwitcher**

对 `src/pages/index/index.vue`、`src/pages/profile/profile.vue`、`src/pages/benefits/index.vue` 各做：

(a) import 区加：

```js
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'
```

(b) template 根 `<view>` 末尾（闭合 `</view>` 之前）加：

```html
<DevLevelSwitcher />
```

- [ ] **Step 4: 全量测试**

```bash
npx vitest run
```
Expected: 全部 PASS。

- [ ] **Step 5: 构建验证**

```bash
npx uni build -p mp-weixin
```
Expected: 构建成功。

- [ ] **Step 6: Commit**

```bash
cd C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-mobile && git add src/components/dev/DevLevelSwitcher.vue src/pages/index/index.vue src/pages/profile/profile.vue src/pages/benefits/index.vue
git commit -m "feat(dev): 新增会员等级切换器"
```

---

## Task 6: 微信开发者工具人工验收

**Files:** 无代码改动，仅运行验证。

- [ ] **Step 1: dev 构建**

```bash
cd C:/Users/dell/Desktop/CozyCoffee/cozy-coffee/cozy-coffee-mobile
npm run dev:mp-weixin
```
（保持 watch 运行）

- [ ] **Step 2: 微信工具导入 dist/dev/mp-weixin**

微信开发者工具导入路径：
```
C:\Users\dell\Desktop\CozyCoffee\cozy-coffee\cozy-coffee-mobile\dist\dev\mp-weixin
```

- [ ] **Step 3: 视觉验收 -- 5 等级逐一切换**

点右下角齿轮 ⚙️ -> 依次切 basic / silver / gold / diamond / black，每级检查 3 张卡（首页 / 个人中心 / 权益页）：
- 卡片背景按等级变色
- 文字色对比可读
- 徽章（accent 色）显示正确
- 进度条用 accent 色
- black：金箔纹理（低透明金点）、文字纯金非渐变
- gold 不土豪；silver 暖灰不冷；diamond 蓝灰不跳

- [ ] **Step 4: 隔离验收 -- 非会员页未换肤**

跳转菜单页、订单页、积分商城，确认保持品牌纯白/暖灰，未受等级主题影响。

- [ ] **Step 5: 状态验收 -- dev 持久化**

切 diamond -> 刷新页面 -> 仍 diamond。关闭微信工具 -> 重开 -> 仍 diamond（验证 `dev_member_override` storage）。

- [ ] **Step 6: 恢复验收**

点「恢复真实等级」-> 回到真实等级（若后端可达，拉回真实数据；来源标记变 `○ API`）。

- [ ] **Step 7: 关闭来源显示验证**

点遮罩或 `×` 能关闭弹层，不卡死。

- [ ] **Step 8: 全量自动化测试**

```bash
npx vitest run
```
Expected: 全部 PASS（35+ tests）。

---

## Self-Review 结果

**1. Spec coverage：** spec 的 10 节（token/theme map/store/composable/3 卡/LevelBadge/DevLevelSwitcher/dev mode/验收/rollback + commit 拆分）均有对应 task。✅

**2. Placeholder scan：** Task 4 Step 2/3 的"保留原 padding 值"指令是故意的 -- 不同页面原 padding 值不同，实现时需读原文件。这属于"读原文件确认值"而非占位符 TODO。其余无 TBD/TODO。✅

**3. Type consistency：** `useMemberTheme` 返回 `themeStyle/levelTheme/isDark/level`；Task 4 三处均用 `{ themeStyle, isDark, level: themeLevel }` 解构，命名一致。`LevelBadge` props `level/color/size` 在 Task 3 定义、Task 4 调用一致。store 的 `setDevLevel/clearDevOverride/refreshMemberInfo/devOverride` 在 Task 2 定义、Task 5 调用一致。✅

**4. 一个已知风险**：Task 5 Step 2 发现 uni-app App.vue 模板不渲染页面级内容，已在 Step 2 内联调整为"3 个会员页面各挂载"，并更新 Step 3/6 commit 范围。无需额外 task。✅
