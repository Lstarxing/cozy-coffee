# 移动端会员等级主题系统设计

**日期**: 2026-07-24
**范围**: `cozy-coffee-mobile`
**目标**: 在 3 个会员专属卡片上还原 Web 端每级换肤效果，并提供 dev 悬浮按钮切换等级以便测试。

---

## 1. 背景与目标

### 现状问题

移动端 `userLevel`（`basic`/`silver`/`gold`/`diamond`/`black`）目前只是文字标签，5 个等级在视觉上完全无差异：

- 首页 `.member-panel`、个人中心 `.member-hero`、权益页 `.current-level` 三处会员卡都用同一个深棕背景 `$cozy-surface-alt`（`#2C1E18`）。
- 没有每级配色 token，没有徽章，进度条统一绿色 `$cozy-accent`。
- 没有任何测试入口可预览各等级，要"看到钻石/黑金效果"必须真积累到 9000 EXP。

### 目标

1. 忠实还原 Web 端 `MemberHeroCard.vue` 的每级换肤思路，但配色暖化、规避移动端设计禁令。
2. 换肤**仅限 3 个会员专属卡片**，不全站换肤（等级是身份，不是主题）。
3. 提供 dev 悬浮按钮，一键切换 5 个等级预览，生产构建自动剔除。

### 非目标

- 不改后端 `MemberDTO` 或任何接口。
- 不重构既有菜单/订单/积分商城等非会员专属页面的视觉。
- 不做全站主题切换系统（YAGNI）。

---

## 2. 设计原则

1. **局部换肤** -- 只在 3 张会员卡生效，菜单/订单等保持品牌纯白与暖灰。
2. **token 驱动** -- 配色集中在 `uni.scss` + `constants/member.js`，组件不写死等级色，便于后续新增等级（如 platinum/anniversary）。
3. **移动端合规** -- 遵守 `uni.scss` 现有禁令：禁渐变文字、禁玻璃拟态、禁卡片套卡片。渐变只用于卡片背景；black 的金色文字用纯色不用 `background-clip:text`。
4. **真实数据与测试覆盖分离** -- store 的 `memberInfo` 对外名不变（改 computed），dev 覆盖走独立 `devOverride` 通道，不污染业务数据。
5. **等级感克制** -- 配色暖化、去土豪感（金不土、黑不赌场），对齐 CozyCoffee 精品咖啡品牌。

---

## 3. Design Token（`uni.scss` 新增）

```scss
// 会员等级配色 token（仅会员专属卡片使用）
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
$member-black-pattern: rgba(201, 162, 39, .08);  // black 金箔纹理色，token 化便于品牌调整
```

### CSS 变量策略（小程序兼容）

**不使用 `:root`**（微信小程序对 `:root` 支持不稳定）。改为**卡片级动态注入**：每张会员卡通过 `:style="themeStyle"` 只注入 3 个运行时变量（`--member-surface` / `--member-text` / `--member-accent`），不污染全局。

链路：
```
scss token ($member-*)
    ↓
JS theme map (MEMBER_LEVEL_THEMES)
    ↓
useMemberTheme 返回 3 个 CSS 变量
    ↓
卡片 :style 注入（卡片作用域内）
    ↓
组件引用 var(--member-*)
```

仅 black 金箔纹理色单独走 `$member-black-pattern` token（见 §7.1）。等级 token 本身**不暴露为全局 CSS variable**，减少全局污染。

**配色说明**：
- basic 浅暖灰（替代 Web 奶油底 `#F5F0E6`，避开大面积低对比米黄）。
- silver 冷灰转暖灰（`#ECE8E4 -> #D6D0CA`），避免"银行卡/SaaS Pro"冷感。
- gold 降饱和（`#C9A45C` 而非 `#D4AF37`），走精品咖啡而非电商土豪金。
- diamond 用蓝灰（`#B8C6CA` + `#546E7A`）而非 Material 鲜蓝，与品牌暖调更协调。
- black 深棕黑 `#171411` + 暖金 `#E6C97A`，Aesop 式克制，避免赌场黑金。

---

## 4. Theme Mapping（`constants/member.js` 新增）

```js
export const MEMBER_LEVEL_THEMES = {
  basic: {
    surface: 'var(--member-basic-bg)',
    text: 'var(--member-basic-ink)',
    accent: 'var(--member-basic-accent)',
    isDark: false
  },
  silver: {
    surface: 'linear-gradient(135deg, var(--member-silver-bg-start), var(--member-silver-bg-end))',
    text: 'var(--member-silver-ink)',
    accent: 'var(--member-silver-accent)',
    isDark: false
  },
  gold: {
    surface: 'linear-gradient(135deg, var(--member-gold-bg-start), var(--member-gold-bg-end))',
    text: 'var(--member-gold-ink)',
    accent: 'var(--member-gold-accent)',
    isDark: false
  },
  diamond: {
    surface: 'linear-gradient(135deg, var(--member-diamond-bg-start), var(--member-diamond-bg-end))',
    text: 'var(--member-diamond-ink)',
    accent: 'var(--member-diamond-accent)',
    isDark: false
  },
  black: {
    surface: 'var(--member-black-bg)',
    text: 'var(--member-black-ink)',
    accent: 'var(--member-black-accent)',
    isDark: true
  }
}
```

字段语义：`surface`（卡背景，可纯色可渐变）、`text`（文字色）、`accent`（进度条/徽章/强调）、`isDark`（是否暗背景，影响阴影与纹理）。

### DEV_MEMBER_MOCK

完整模拟 `MemberDTO` 形状，不只给等级，便于未来页面新增字段时无需重造 mock：

```js
export const DEV_MEMBER_MOCK = {
  basic:   { id: 90001, memberLevel: 'basic',   levelName: '基础会员', expTotal: 100,  currentPoints: 50,    couponCount: 0, exchangeCouponCount: 0 },
  silver:  { id: 90002, memberLevel: 'silver',  levelName: '白银会员', expTotal: 800,  currentPoints: 1200,  couponCount: 2, exchangeCouponCount: 1 },
  gold:    { id: 90003, memberLevel: 'gold',    levelName: '黄金会员', expTotal: 2000, currentPoints: 3600,  couponCount: 3, exchangeCouponCount: 2 },
  diamond: { id: 90004, memberLevel: 'diamond', levelName: '钻石会员', expTotal: 4500, currentPoints: 8888,  couponCount: 5, exchangeCouponCount: 3 },
  black:   { id: 90005, memberLevel: 'black',   levelName: '黑金会员', expTotal: 9500, currentPoints: 18888, couponCount: 8, exchangeCouponCount: 5 }
}
```

含 `levelName`，尽量接近 `MemberDTO` 形状；未来页面新增展示字段无需重造 mock。

---

## 5. Store Architecture（`stores/session.js`）

### 关键约束

`session.js` 是 **Pinia Setup Store**（`defineStore('session', () => {...})`）。`memberInfo` / `userLevel` 以 computed getter 形式暴露，页面访问 `store.memberInfo` 时 Pinia 自动 unwrap，**页面零改动**。

### Store 命名规范

仓库当前存在两套混用：`stores/session.js`（真身，`useSessionStore`）与 `stores/user.js`（别名 re-export `useUserStore`），10 个页面仍在用别名。这是既有技术债。

**本次新增代码（composable、DevLevelSwitcher）统一用 `useSessionStore`**，从 `@/stores/session` 导入，不再引入别名。既有页面的 import **本次不动**（避免扩大 blast radius），留作后续单独技术债清理。两者指向同一 store 实例，混用不影响功能。

### 改造

```js
export const useSessionStore = defineStore('session', () => {
  const token = ref('')
  const userInfo = ref(emptyUser())
  const realMemberInfo = ref(emptyMember())     // 真实后端数据
  const devOverride = ref(null)                // null | 'basic'..'black'
  const restored = ref(false)

  // 对外 memberInfo 改为 computed：dev 覆盖优先，否则用真实数据
  const memberInfo = computed(() =>
    devOverride.value
      ? (DEV_MEMBER_MOCK[devOverride.value] || realMemberInfo.value)
      : realMemberInfo.value
  )
  const userLevel = computed(() => memberInfo.value.memberLevel || 'basic')
  const isLoggedIn = computed(() => Boolean(token.value))
  const isAuthenticated = isLoggedIn

  function restore() {
    token.value = uni.getStorageSync('token') || ''
    userInfo.value = { ...emptyUser(), ...readJson('userInfo', {}) }
    realMemberInfo.value = { ...emptyMember(), ...readJson('memberInfo', {}) }
    restoreDevOverride()
    restored.value = true
    return isLoggedIn.value
  }

  // DEV 环境保护：生产构建强制清除，避免体验版携带 dev 状态
  function restoreDevOverride() {
    if (!import.meta.env.DEV) {
      uni.removeStorageSync('dev_member_override')
      return
    }
    const saved = uni.getStorageSync('dev_member_override')
    if (saved && DEV_MEMBER_MOCK[saved]) devOverride.value = saved
  }

  function setMemberInfo(info = {}) {           // 写 realMemberInfo（不改名，保持调用方不变）
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
    // 现有 getMemberInfo 逻辑，写入 realMemberInfo（setMemberInfo）
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

  restore()

  return {
    token, userInfo, memberInfo, restored,
    isLoggedIn, isAuthenticated, userLevel,
    restore, setLoginInfo, setMemberInfo, clearSession, logout,
    setDevLevel, clearDevOverride, refreshMemberInfo  // 新增
  }
})
```

**要点**：
- `memberInfo` 名字不变、类型不变，仅从 ref 变 computed -- 所有 `userStore.memberInfo.xxx` 调用方无需修改。
- dev 状态独立通道，`setMemberInfo`（后端回写）只动 `realMemberInfo`，不会冲掉 dev 覆盖；dev 覆盖在 computed 层优先。
- 生产构建 `import.meta.env.DEV` 为 false，`restoreDevOverride` 强制清除 storage 残留。

### `.value` 调用兼容性检查

`memberInfo` 从 ref 变 computed 后，Pinia setup store 对外**自动 unwrap**：`store.memberInfo` 直接是对象（不是 ref），页面里 `userStore.memberInfo?.xxx` 全部安全。

风险点仅在「store 内部」或「页面再把 `store.memberInfo` 包一层 ref/computed 再 `.value`」。已排查：
- `session.js` 内部的 `memberInfo.value = ...` 会随重构改为操作 `realMemberInfo.value`（见上）。
- `pages/profile/profile.vue`、`pages/points/history.vue` 有 `const memberInfo = computed(() => userStore.memberInfo)` 后 `memberInfo.value.xxx` -- 这是包了一层 computed，`.value` 拿到的就是 store 已 unwrap 的对象，安全，无需改。

实现时再全量 `grep memberInfo.value` 复核一遍。

---

## 6. useMemberTheme Composable（`composables/useMemberTheme.js`）

3 张卡共用，避免重复拼 themeStyle。从 `@/stores/session` 导入真身，不用别名：

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

---

## 7. Component Changes

### 7.1 会员卡容器（3 处）

文件：`pages/index/index.vue`（`.member-panel`）、`pages/profile/profile.vue`（`.member-hero`）、`pages/benefits/index.vue`（`.current-level`）。

每处：
1. 引入 `useMemberTheme`。
2. 容器绑定 `:style="themeStyle"` + `:class="[userLevel, { 'is-dark': isDark }]"`。
3. SCSS 改为消费变量：
```scss
.member-card {
  position: relative;
  overflow: hidden;
  background: var(--member-surface);
  color: var(--member-text);
}
.member-card .progress-fill { background: var(--member-accent); }
.member-card .level-badge { color: var(--member-accent); }
.member-card > * { position: relative; z-index: 1; }
```
4. black 金箔纹理（仅 `.member-card.black::before`），用 `$member-black-pattern` token：
```scss
.member-card.black::before {
  content: '';
  position: absolute; inset: 0;
  z-index: 0;
  background-image: radial-gradient(#{$member-black-pattern} 1px, transparent 1px);
  background-size: 18rpx 18rpx;
  pointer-events: none;
}
```
极低透明度（0.08），Aesop 式克制，避免游戏黑卡粒子感。纹理色走 token，品牌调整黑金时改一处即可。

### 7.2 LevelBadge（`components/member/LevelBadge.vue`）

生产用 SVG，不用 emoji（跨系统渲染不一致）。5 个内联线性 SVG，统一 1.5px 描边、`currentColor`：

| 等级 | 图形 |
|------|------|
| basic | 咖啡豆 |
| silver | 勋章 |
| gold | 星 |
| diamond | 钻石 |
| black | 王冠 |

组件接收 `level`、`color`、`size` prop，徽章用 **accent 色**（非 text 色），层级更清晰：
```vue
<template>
  <view class="level-badge" :style="{ color, width: size + 'rpx', height: size + 'rpx' }">
    <!-- 内联对应 SVG，stroke="currentColor" -->
  </view>
</template>
<script setup>
defineProps({
  level: String,
  color: { type: String, default: 'currentColor' },
  size: { type: Number, default: 48 }   // rpx，首页 48 / 权益页 64 / 详情页 80 等
})
</script>
```
调用：`<LevelBadge :level="level" color="var(--member-accent)" :size="48" />`

### 7.3 DevLevelSwitcher（`components/dev/DevLevelSwitcher.vue`）

```vue
<template>
  <view v-if="isDev" class="dev-switcher">
    <view class="dev-fab" @click="open = true">⚙️</view>
    <view v-if="open" class="dev-sheet" @click.self="open = false">
      <view class="dev-sheet-inner" @click.stop>
        <view class="dev-head">
          <text class="dev-title">开发模式 · 当前: {{ currentLabel }}</text>
          <text class="dev-close" @click="open = false">×</text>
        </view>
        <text class="dev-source">来源: {{ store.devOverride ? '● DEV MOCK' : '○ API' }}</text>
        <text class="dev-meta">EXP: {{ memberInfo.expTotal }} · 积分: {{ memberInfo.currentPoints }}</text>
        <view class="dev-grid">
          <view v-for="lvl in LEVELS" :key="lvl"
                class="dev-btn" :class="[lvl, { active: currentLevel === lvl }]"
                @click="select(lvl)">
            {{ LABELS[lvl] }}
          </view>
        </view>
        <view class="dev-restore" @click="restore">恢复真实等级</view>
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
function select(l) { store.setDevLevel(l); open.value = false }
function restore() { store.clearDevOverride(); store.refreshMemberInfo(); open.value = false }
</script>
```
- `v-if="isDev"` -- 生产构建整体剔除。
- 悬浮齿轮右下角；点开底部弹层。
- 弹层支持：点击遮罩关闭（`@click.self`）、关闭按钮 `×`。
- 显示来源标记（DEV MOCK / API）+ EXP/积分，调试会员体系时一眼看清当前数据来源。
- 挂在 `App.vue` 根，全页面可用。

---

## 8. Development Mode

- dev 构建渲染 DevLevelSwitcher；生产构建 `import.meta.env.DEV` 为 false，组件 `v-if` 失效，且 tree-shaking 剔除相关代码。
- `setDevLevel` / `restoreDevOverride` 内部均带 `import.meta.env.DEV` 守卫，生产环境即使误调用也不生效。
- dev 覆盖持久化到 `dev_member_override` storage，跨重启保留（方便多页面验证）；生产 restore 时强制清除。

---

## 9. Acceptance Checklist

### 视觉验收（微信开发者工具）
1. `npm run dev:mp-weixin` -> 微信工具导入 `dist/dev/mp-weixin`。
2. 点齿轮 -> 依次切 basic / silver / gold / diamond / black。
3. 每级检查 3 张卡（首页 `.member-panel` / 个人中心 `.member-hero` / 权益页 `.current-level`）：背景、文字色、徽章色、进度条 accent 色正确。
4. black：确认金箔纹理（低透明金点）、文字纯金非渐变。
5. 跳转菜单页 / 订单页 / 积分商城，确认**未**换肤（保持品牌纯白/暖灰）。
6. gold 确认不"土豪"（降饱和生效）；silver 暖灰不冷；diamond 蓝灰不跳。

### 状态验收
7. dev 切 diamond -> 刷新页面 -> 仍为 diamond。
8. 关闭微信开发者工具 -> 重新打开 -> 仍为 diamond（验证 `dev_member_override` 持久化）。
9. 点「恢复真实等级」-> 回到真实等级；若后端可达，`refreshMemberInfo` 拉回真实数据。

### 自动化验收
10. `npm test` 通过（现有 32 测试 + 新增 store computed 行为测试：dev 覆盖优先、clearDevOverride 后恢复真实、生产 restore 清 storage）。

---

## 10. Rollback Plan

风险低，均为新增与局部替换：

1. **store 改造** -- `memberInfo` 从 ref 变 computed 是唯一对调用方有影响的点；若回归出问题，回退该处即可（页面访问签名不变，理论上零影响）。
2. **token / theme map** -- 纯新增，删除即回退。
3. **3 张卡 SCSS** -- 每处保留原 `$cozy-surface-alt` 背景的备份注释，回退时恢复 class 绑定即可。
4. **DevLevelSwitcher** -- `v-if="isDev"` 生产自动剔除，dev 可随时移除组件挂载点回退。

---

## 11. Commit 拆分建议

分 4 个 commit，便于逐步验证与回滚。commit message 用中文，`feat`/`refactor` 前缀：

1. **`feat(member): 新增会员等级主题 token 与主题映射`**
   - `src/uni.scss`（5 套 `$member-*` token + `$member-black-pattern`）
   - `src/constants/member.js`（`MEMBER_LEVEL_THEMES` + `DEV_MEMBER_MOCK`）
   - 纯新增，不影响页面，可独立验证 token 渲染。

2. **`refactor(session): 支持会员等级开发覆盖`**
   - `src/stores/session.js`（`memberInfo` ref -> computed、`devOverride`、`setDevLevel`/`clearDevOverride`/`refreshMemberInfo`、`restoreDevOverride` 加 DEV 守卫）
   - 新增/补充 session store 测试（dev 覆盖优先、clear 后恢复、生产清 storage）。
   - store 单独可测，不依赖 UI。

3. **`feat(member): 应用会员等级主题到三张卡`**
   - `src/composables/useMemberTheme.js`（新增）
   - `src/components/member/LevelBadge.vue`（新增，5 个 SVG）
   - `src/pages/index/index.vue`、`pages/profile/profile.vue`、`pages/benefits/index.vue`（绑定 themeStyle + per-level SCSS + LevelBadge）
   - 视觉可逐卡验证。

4. **`feat(dev): 新增会员等级切换器`**
   - `src/components/dev/DevLevelSwitcher.vue`（新增）
   - `src/App.vue`（根挂载）
   - 最后接入，方便测试前三步的视觉结果。

---

## 受影响文件清单

| 文件 | 改动 |
|------|------|
| `src/uni.scss` | 新增 5 套 `$member-*` token + `$member-black-pattern`（不暴露全局 `:root` 变量，卡片级注入） |
| `src/constants/member.js` | 新增 `MEMBER_LEVEL_THEMES` + `DEV_MEMBER_MOCK` |
| `src/stores/session.js` | `memberInfo` ref -> computed（含 devOverride）、新增 `setDevLevel`/`clearDevOverride`/`refreshMemberInfo`、`restoreDevOverride` 加 DEV 守卫 |
| `src/composables/useMemberTheme.js` | 新增 |
| `src/components/member/LevelBadge.vue` | 新增（5 个 SVG） |
| `src/components/dev/DevLevelSwitcher.vue` | 新增 |
| `src/App.vue` | 根挂载 `<DevLevelSwitcher />` |
| `src/pages/index/index.vue` | `.member-panel` 绑定 themeStyle + per-level SCSS |
| `src/pages/profile/profile.vue` | `.member-hero` 绑定 themeStyle + per-level SCSS |
| `src/pages/benefits/index.vue` | `.current-level` 补 per-level SCSS（已有 `:class="currentLevel"`） |
| `src/stores/session.test.js`（新增）或既有测试 | 补 memberInfo computed / devOverride 行为测试 |

---

## 设计参考

- Web 端 `MemberHeroCard.vue`、`MemberBenefits.vue`、`MemberLayout.vue` 的每级换肤思路（已暖化、去土豪、合规化）。
- 移动端 `uni.scss` 现有设计语言（暖白底、咖啡棕主色、釉面绿语义色、禁渐变文字/玻璃/奶油底/卡片套卡片）。
- 评审中用户对配色的修正：silver 暖化、gold 降饱和、diamond 去鲜蓝、black 去赌场感。
