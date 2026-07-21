<template>
  <main class="warm-home">
    <section id="home" class="warm-hero" aria-labelledby="hero-title">
      <picture class="warm-hero__media">
        <img
          :src="heroImage"
          alt="阳光下的 Cozy Coffee 拿铁与杭州烘焙名片"
          fetchpriority="high"
          width="3600"
          height="1600"
          @error="handleImageError"
        >
      </picture>
      <div class="warm-hero__content">
        <h1 id="hero-title">
          <span class="hero-title-line">循着风味</span>
          <span class="hero-title-line">走进一段地理日志</span>
        </h1>
        <p class="hero-description">
          <span class="hero-description-line">从世界八大产区，到杭州匠心烘焙</span>
          <span class="hero-description-line">每一杯，都是土地与时间留下的答案</span>
        </p>
      </div>
      <a class="hero-cta" href="#origins">
        <img class="hero-cta__arrow" :src="heroArrow" alt="" aria-hidden="true">
        <span class="hero-cta__text">探索产地风味之旅</span>
      </a>
    </section>

    <OriginsJourney />

    <EditorialMenu :products="menuItems" :flavor-routes="flavorRoutes" @image-error="handleImageError" />

    <!-- Bridge：菜单 → 荣誉的节奏过渡。无 CTA，只保留向下视觉引导。
         SVG line+chevron opacity 0.4 持续 2.4s 呼吸，告诉用户下面还有内容。
         荣誉 banner 自带「了解我们的坚持 →」按钮，避免 CTA 重复。 -->
    <section class="menu-bridge" aria-label="menu to brand rhythm">
      <div class="warm-shell menu-bridge__shell">
        <p class="menu-bridge__kicker">OUR PHILOSOPHY</p>
        <span class="menu-bridge__rule" aria-hidden="true"></span>
        <h2 class="menu-bridge__title">
          <span>每一颗豆子</span>
          <span>都值得认真对待</span>
        </h2>
        <span class="menu-bridge__hint" aria-hidden="true">
          <svg width="14" height="40" viewBox="0 0 14 40">
            <line x1="7" y1="0" x2="7" y2="28" stroke="currentColor" stroke-width="1" />
            <polyline points="3,25 7,30 11,25" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <span class="menu-bridge__rule" aria-hidden="true"></span>
      </div>
    </section>

    <section class="honor-banner" aria-labelledby="honor-banner-title">
      <picture class="honor-banner__media" aria-hidden="true">
        <source
          type="image/avif"
          :srcset="honorSrcsetAvif"
          sizes="(min-width: 1716px) 1716px, 100vw"
        >
        <source
          type="image/webp"
          :srcset="honorSrcsetWebp"
          sizes="(min-width: 1716px) 1716px, 100vw"
        >
        <img
          :src="honorFallback"
          :srcset="honorSrcsetJpg"
          sizes="(min-width: 1716px) 1716px, 100vw"
          alt="Cozy Coffee 连续五年获得 IIAC 金奖"
          loading="lazy"
          width="4096"
          height="928"
        >
      </picture>
      <div class="honor-banner__scrim" aria-hidden="true"></div>
      <div class="warm-shell honor-banner__content">
        <h2 id="honor-banner-title">荣誉与坚持</h2>
        <p>连续 5 年 IIAC 金奖认可，源自对品质的执着。</p>
        <router-link class="honor-banner__link" to="/about">了解我们的坚持 <span aria-hidden="true">→</span></router-link>
      </div>
    </section>

    <section id="membership" class="warm-membership" aria-labelledby="membership-title">
      <div class="warm-shell membership-shell">
        <template v-if="membershipState === 'auth-resolving'">
          <div class="membership-skeleton" aria-label="正在确认登录状态" aria-busy="true">
            <span></span><span></span><span></span>
          </div>
        </template>

        <template v-else>
          <header class="membership-header">
            <p class="membership-header__kicker">MEMBERSHIP JOURNEY</p>
            <h2 id="membership-title">这一杯，也在回馈下一杯</h2>
            <p>消费 · 积累 · 成长 · 解锁更多专属体验</p>
          </header>

          <div v-if="membershipState === 'member-loading'" class="membership-skeleton membership-skeleton--content" aria-label="正在读取会员数据" aria-busy="true">
            <span></span><span></span><span></span>
          </div>

          <div v-else-if="membershipState === 'member-failed'" class="membership-error" role="status">
            <h3>暂时没有读到你的会员数据</h3>
            <p>网络恢复后可以在这里继续查看积分进度。</p>
            <div class="membership-actions">
              <button class="warm-button warm-button--inverse" type="button" @click="loadMemberData">重试读取</button>
              <details class="benefits-details benefits-details--compact">
                <summary>查看示例权益</summary>
                <p>普通日消费 ¥32，白银会员可获得 35 积分；周五会员日额外 +0.5×。</p>
              </details>
            </div>
          </div>

          <template v-else>
            <!-- 等级 + 成长进度 - 居中单列 -->
            <div class="membership-progress-section">
              <p class="membership-progress__kicker">CURRENT MEMBERSHIP</p>
              <p class="membership-progress__level-en">{{ levelProgress.currentLevel.name.split(' ')[1] }}</p>
              <p class="membership-progress__level-cn">{{ levelProgress.currentLevel.name.split(' ')[0] }}会员</p>
              <p class="membership-progress__exp-val">{{ membershipState === 'anonymous' ? 850 : levelProgress.current }}<span class="membership-progress__exp-unit"> EXP</span></p>

              <div v-if="!levelProgress.isMax" class="membership-progress__line">
                <span class="membership-progress__line-dot" :style="{ left: levelProgress.percentage + '%' }"></span>
              </div>

              <p v-if="!levelProgress.isMax" class="membership-progress__next">
                <span class="membership-progress__next-en">{{ levelProgress.nextLevelName }}</span>
                <span class="membership-progress__next-cn">距离下一阶段，还有 {{ levelProgress.remaining }} EXP</span>
              </p>
              <p v-else class="membership-progress__max">COZY BLACK MEMBER · 品牌大使</p>
            </div>

            <div class="membership-hairline"></div>

            <!-- 消费 & 兑换示例 - 双栏 -->
            <div class="membership-activity">

              <div class="membership-activity__grid">
                <!-- 01: 消费得积分 -->
                <div class="membership-activity__col">
                  <p class="membership-activity__header">
                    RECENT MOMENT<span class="membership-activity__header-dot"> · </span><span class="membership-activity__seq">01</span>
                  </p>
                  <div class="membership-activity__row">
                    <span class="membership-activity__name">手冲咖啡</span>
                    <span class="membership-activity__price">¥35</span>
                  </div>
                  <p class="membership-activity__earned-kicker">EARNED</p>
                  <div class="membership-activity__earned-row">
                    <span class="membership-activity__earned-num">35</span>
                    <span class="membership-activity__earned-unit">EXP</span>
                    <span class="membership-activity__earned-plus">+</span>
                    <span class="membership-activity__earned-num">{{ earnedPoints }}</span>
                    <span class="membership-activity__earned-unit">POINTS</span>
                  </div>
                  <p class="membership-activity__bonus-note">{{ levelLabel }} {{ currentRate }}× 倍率</p>
                  <p class="membership-activity__col-narrative">"因为一次咖啡选择，积累下一次相遇"</p>
                </div>

                <!-- 02: 积分兑换 -->
                <div class="membership-activity__col">
                  <p class="membership-activity__header">
                    RECENT MOMENT<span class="membership-activity__header-dot"> · </span><span class="membership-activity__seq">02</span>
                  </p>
                  <div class="membership-activity__row">
                    <span class="membership-activity__name">拿铁兑换券</span>
                    <span>                    <span class="membership-activity__price membership-activity__price--strike">150</span></span>
                  </div>
                  <p class="membership-activity__earned-kicker">REDEEM</p>
                  <div class="membership-activity__earned-row">
                    <span class="membership-activity__earned-num">{{ rewardTarget }}</span>
                    <span class="membership-activity__earned-unit">POINTS</span>
                  </div>
                  <p class="membership-activity__bonus-note">{{ levelLabel }} {{ currentDiscount }}兑换</p>
                  <p class="membership-activity__col-narrative">"用积分换一杯心仪，也是给自己的犒赏"</p>
                </div>
              </div>
            </div>

            <div class="membership-hairline"></div>

            <!-- 权益亮点 - 单列编号纵向 -->
            <div class="membership-benefits">
              <p class="membership-benefits__kicker">MEMBERSHIP JOURNEY</p>

              <article class="benefit-item">
                <span class="benefit-item__seq">01</span>
                <div class="benefit-item__body">
                  <p class="benefit-item__en">MONTHLY BENEFIT</p>
                  <p class="benefit-item__cn">月度权益</p>
                  <p class="benefit-item__desc">每个月，一份属于会员的日常心意</p>
                </div>
              </article>

              <article class="benefit-item">
                <span class="benefit-item__seq">02</span>
                <div class="benefit-item__body">
                  <p class="benefit-item__en">BIRTHDAY REWARD</p>
                  <p class="benefit-item__cn">生日礼遇</p>
                  <p class="benefit-item__desc">生日这个月，有一杯咖啡算我们的</p>
                </div>
              </article>

              <article class="benefit-item">
                <span class="benefit-item__seq">03</span>
                <div class="benefit-item__body">
                  <p class="benefit-item__en">MEMBERSHIP JOURNEY</p>
                  <p class="benefit-item__cn">会员成长</p>
                  <p class="benefit-item__desc">每一次停留，都让下一次体验更丰富</p>
                </div>
              </article>
            </div>

            <div class="membership-hairline"></div>

            <!-- 完整等级对比表 -->
            <details class="benefits-details">
              <summary>Explore Membership Levels -></summary>
              <div v-if="!isMobile" class="benefits-table-wrap">
                <table aria-label="会员等级速览">
                  <thead>
                    <tr><th scope="col">等级</th><th scope="col">EXP门槛</th><th scope="col">积分</th><th scope="col">会员日</th><th scope="col">折扣</th><th scope="col">每月权益</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="level in levels" :key="level.key" :class="{ 'is-current': isCurrentLevel(level.key) }">
                      <td>{{ level.name }} <span v-if="isCurrentLevel(level.key)" class="current-marker">当前</span></td>
                      <td>{{ level.threshold }}</td><td>{{ level.rate }}</td><td>{{ level.cozyDay }}</td><td>{{ level.discount }}</td><td>{{ level.benefit }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="benefits-list" aria-label="会员等级速览">
                <dl v-for="level in levels" :key="level.key" :class="{ 'is-current': isCurrentLevel(level.key) }">
                  <div class="benefits-list__title"><dt>等级</dt><dd>{{ level.name }} <span v-if="isCurrentLevel(level.key)" class="current-marker">当前</span></dd></div>
                  <div><dt>EXP门槛</dt><dd>{{ level.threshold }}</dd></div>
                  <div><dt>积分倍率</dt><dd>{{ level.rate }}</dd></div>
                  <div><dt>会员日</dt><dd>{{ level.cozyDay }}</dd></div>
                  <div><dt>兑换折扣</dt><dd>{{ level.discount }}</dd></div>
                  <div><dt>每月权益</dt><dd>{{ level.benefit }}</dd></div>
                </dl>
              </div>
            </details>

            <div class="membership-hairline"></div>

            <div class="membership-journey__cta">
              <router-link
                class="membership-journey__cta-link"
                :to="membershipState === 'anonymous' ? '/register' : '/member'"
              >
                {{ membershipState === 'anonymous' ? '开始你的咖啡旅程 ->' : '进入会员中心 →' }}
              </router-link>
            </div>
          </template>
        </template>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getMemberInfo, getMonthlyTask } from '@/api/member'
import OriginsJourney from '@/components/home/OriginsJourney.vue'
import EditorialMenu from '@/components/home/EditorialMenu.vue'
import { useUserStore } from '@/stores/user'
import { HOME_FLAVOR_ROUTES, HOME_MENU_PRODUCTS } from '@/data/homeMenu'
import {
  calculateEarnedPoints,
  calculateProgress,
  calculateRedemptionCost,
  POINTS_RATES_BY_LEVEL
} from '@/utils/homepageMembership'

// 静态资源：有 OSS 时走 OSS，否则用本地占位图
const OSS_BASE = import.meta.env.VITE_ASSET_BASE_URL || ''
const asset = (ossPath, localPath) => OSS_BASE ? `${OSS_BASE}${ossPath}` : localPath

const heroImage = asset(
  '/images/home/hero-prototype-20260717-v3.jpg',
  '/images/home/hero-prototype-20260717-v3.jpg'
)
const heroArrow = asset('/images/hero/hero-arrow.svg', '/images/hero/hero-arrow.svg')

const honorBannerBase = OSS_BASE
  ? `${OSS_BASE}/images/home/honor-banner`
  : '/images/home/honor-banner'
const honorSrcsetAvif = [768, 1440, 1920].map(w => `${honorBannerBase}-${w}.avif ${w}w`).join(', ')
const honorSrcsetWebp = [768, 1440, 1920].map(w => `${honorBannerBase}-${w}.webp ${w}w`).join(', ')
const honorSrcsetJpg = [768, 1440, 1920].map(w => `${honorBannerBase}-${w}.jpg ${w}w`).join(', ')
const honorFallback = `${honorBannerBase}-1440.jpg`

const userStore = useUserStore()
const membershipState = ref('auth-resolving')
const memberInfo = ref(null)
const monthlyTask = ref(null)
const isMobile = ref(false)
let mediaQuery = null
let disposed = false

const menuItems = HOME_MENU_PRODUCTS
const flavorRoutes = HOME_FLAVOR_ROUTES

const levels = [
  { key: 'basic', name: '基础 Classic', threshold: '0 EXP', rate: '1.0×', cozyDay: '1.5×', discount: '—', benefit: '免费加浓缩券×1' },
  { key: 'silver', name: '白银 Silver', threshold: '500 EXP', rate: '1.1×', cozyDay: '1.6×', discount: '9.8 折', benefit: '配送券×1+加浓缩券×2 · 生日BOGO' },
  { key: 'gold', name: '黄金 Gold', threshold: '1,500 EXP', rate: '1.2×', cozyDay: '1.7×', discount: '9.5 折', benefit: 'BOGO×1+8.8折券×2+配送券×2 · 生日免单券' },
  { key: 'diamond', name: '钻石 Diamond', threshold: '4,000 EXP', rate: '1.3×', cozyDay: '1.8×', discount: '9.0 折', benefit: '免单券×1 + BOGO×2 + 配送券×5 + 新品5折券 · 生日蛋糕5折' },
  { key: 'black', name: '黑金 Black Gold', threshold: '9,000 EXP', rate: '1.5×', cozyDay: '2.0×', discount: '8.5 折', benefit: '1.7x加速包 · 免单券×2 + BOGO×5 · 无限免配送 · 新品试饮券 · 生日免单+蛋糕+888积分' }
]

const normalizedLevel = computed(() => {
  if (membershipState.value === 'anonymous') return 'silver'
  const level = memberInfo.value?.memberLevel || memberInfo.value?.level || 'basic'
  return POINTS_RATES_BY_LEVEL[level] ? level : 'basic'
})
const currentLevel = computed(() => levels.find(level => level.key === normalizedLevel.value) || levels[0])
const currentLevelLabel = computed(() => currentLevel.value.name.split(' ')[0] + '会员')
const currentRate = computed(() => POINTS_RATES_BY_LEVEL[normalizedLevel.value] || 1)
const currentPoints = computed(() => membershipState.value === 'anonymous' ? 131 : Number(memberInfo.value?.currentPoints || 0))
const rewardTarget = computed(() => calculateRedemptionCost(150, normalizedLevel.value))
const earnedPoints = computed(() => calculateEarnedPoints(35, normalizedLevel.value, false))
const levelLabel = computed(() => currentLevel.value.name.split(' ')[0])
const currentDiscount = computed(() => currentLevel.value.discount)
const remainingPoints = computed(() => Math.max(0, rewardTarget.value - currentPoints.value))
const progressPercent = computed(() => calculateProgress(currentPoints.value, rewardTarget.value))

const EXP_THRESHOLDS = { basic: 0, silver: 500, gold: 1500, diamond: 4000, black: 9000 }
const NEXT_LEVEL_MAP = { basic: 'silver', silver: 'gold', gold: 'diamond', diamond: 'black' }

const levelProgress = computed(() => {
  const exp = membershipState.value === 'anonymous' ? 850 : (memberInfo.value?.expTotal || 0)
  const currentLvl = normalizedLevel.value
  const nextKey = NEXT_LEVEL_MAP[currentLvl]
  const target = nextKey ? (EXP_THRESHOLDS[nextKey] || 500) : 99999
  const currentThreshold = EXP_THRESHOLDS[currentLvl] || 0
  const relativeExp = exp - currentThreshold
  const relativeTarget = target - currentThreshold
  const pct = relativeTarget <= 0 ? 100 : Math.max(0, Math.min(100, Math.round((relativeExp / relativeTarget) * 100)))
  const nextLevel = levels.find(l => l.key === nextKey)
  return {
    current: exp, target, percentage: pct,
    remaining: Math.max(0, target - exp),
    isMax: !nextKey,
    currentLevel: currentLevel.value,
    nextLevel,
    nextLevelName: nextLevel?.name?.split(' ')[0] || ''
  }
})
const monthlyOrderCount = computed(() => monthlyTask.value?.monthlyOrderCount ?? null)
const representativeBenefit = computed(() => currentLevel.value.benefit)
const redemptionDiscountLabel = computed(() => currentLevel.value.discount === '—' ? '' : `（${currentLevel.value.discount}）`)
const membershipKicker = computed(() => {
  if (membershipState.value === 'anonymous') return '以白银会员为例 · 普通日消费示例'
  if (membershipState.value === 'member-partial') return '你的积分进度 · 月度订单暂不可用'
  if (membershipState.value === 'member-success') return '你的实时会员进度'
  return '会员积分生态'
})

function isCurrentLevel(level) {
  return membershipState.value !== 'anonymous' && normalizedLevel.value === level
}

function handleImageError(event) {
  const image = event.currentTarget
  if (image.dataset.fallbackApplied) return
  image.dataset.fallbackApplied = 'true'
  image.srcset = ''
  image.src = '/images/beans.jpg'
  image.classList.add('image-fallback')
}

function updateMobileState(event) {
  isMobile.value = event.matches
}

async function loadMemberData() {
  membershipState.value = 'member-loading'
  memberInfo.value = null
  monthlyTask.value = null

  const [memberResult, taskResult] = await Promise.allSettled([
    getMemberInfo(),
    getMonthlyTask()
  ])
  if (disposed) return

  if (memberResult.status === 'rejected' || !memberResult.value?.data) {
    membershipState.value = 'member-failed'
    return
  }

  memberInfo.value = memberResult.value.data
  if (userStore.userInfo) Object.assign(userStore.userInfo, memberResult.value.data)

  if (taskResult.status === 'fulfilled' && taskResult.value?.data) {
    monthlyTask.value = taskResult.value.data
    membershipState.value = 'member-success'
  } else {
    membershipState.value = 'member-partial'
  }
}

onMounted(async () => {
  mediaQuery = window.matchMedia('(max-width: 700px)')
  isMobile.value = mediaQuery.matches
  mediaQuery.addEventListener('change', updateMobileState)

  await userStore.init()
  if (disposed) return

  if (!userStore.isLoggedIn) {
    membershipState.value = 'anonymous'
    return
  }
  await loadMemberData()
})

onUnmounted(() => {
  disposed = true
  mediaQuery?.removeEventListener('change', updateMobileState)
})
</script>
<style>
.warm-hero h1, .warm-hero h1 span,
.warm-hero .hero-cta__text {
  font-family: "Source Han Serif SC", "思源宋体", "Noto Serif SC", "Songti SC", "SimSun", serif;
}

/* Hero 图片底部淡出：图片底边自然消融到 #eee1d2，消除与下一段的硬切 */
.warm-hero__media img {
  mask-image: linear-gradient(to bottom, black 85%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, black 85%, transparent 100%);
}

/* Hero 底部渐变：从透明过渡到下一段背景色，消除硬切留白 */
.warm-hero::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 20%;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(to bottom, transparent 0%, var(--cozy-bg) 100%);
}
</style>

<style scoped>
.warm-home {
  background: var(--cozy-bg);
  color: var(--cozy-ink);
  font-family: var(--font-sans);
  overflow: clip;
}

.warm-shell {
  width: min(var(--content-max), calc(100% - var(--content-gutter) - var(--content-gutter)));
  margin-inline: auto;
}

.warm-home :where(a, button, summary):focus-visible {
  outline: 3px solid var(--cozy-primary);
  outline-offset: 4px;
}

.warm-home :where(#home, #origins, #menu, #membership) {
  scroll-margin-top: var(--nav-height);
}

.warm-hero {
  position: relative;
  aspect-ratio: 3600 / 1730;
  width: 100%;
  min-height: 520px;
  container-type: size;
  isolation: isolate;
  overflow-x: clip;
  overflow-y: visible;
  background: #eee1d2;
}

/* Bridge 升级：上下短分隔线 + OUR PHILOSOPHY 副标 + 两行标题 + ↓ 呼吸 SVG（无 CTA）。
   padding-block 收紧让 Series → Slogan 间距落在 100–140px，不空。 */
.menu-bridge {
  padding-block: clamp(40px, 4vw, 56px);
  background: var(--cozy-surface);
  color: var(--cozy-ink);
}
.menu-bridge__shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: clamp(14px, 2vw, 22px);
}
.menu-bridge__kicker {
  margin: 0;
  color: var(--cozy-muted);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
.menu-bridge__rule {
  width: clamp(40px, 6vw, 96px);
  height: 1px;
  background: color-mix(in oklch, var(--cozy-primary) 35%, transparent);
}
.menu-bridge__title {
  margin: 0;
  text-align: center;
  font-family: var(--font-display);
  font-size: clamp(1.4rem, 2.4vw, 2rem);
  font-weight: 500;
  color: var(--cozy-primary);
  letter-spacing: .02em;
  line-height: 1.4;
  text-wrap: balance;
}
.menu-bridge__title span { display: block; }

/* ↓ 视觉引导：opacity 0.4，2.4s 呼吸 translateY(4px)；reduced-motion 关掉动画。 */
.menu-bridge__hint {
  display: block;
  color: var(--cozy-primary);
  opacity: .4;
  animation: menu-bridge-breathe 2.4s ease-in-out infinite;
}
@keyframes menu-bridge-breathe {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(4px); }
}

@media (max-width: 560px) {
  .menu-bridge { padding-block: 48px; }
  .menu-bridge__shell { gap: 16px; }
}

.warm-hero__media,
.warm-hero__media img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.warm-hero__media img {
  object-fit: cover;
  object-position: center;
}

/* 文字组：原型画布 3600×1600 内 left:569，top 下移让出顶部留白 */
.warm-hero__content {
  position: absolute;
  z-index: 2;
  left: 15.805556cqw;
  top: 32cqh;
  width: 35.083333cqw;
  margin: 0;
  padding: 0;
  color: #2d170a;
  animation: warm-fade 700ms ease-out both;
}

/* H1 主标题：思源宋体（--font-display），macOS→Noto Serif SC，Windows→SimSun 兜底 */
.warm-hero h1 {
  margin: 0;
  max-width: none;
  font-family: var(--font-display);
  font-weight: 400;
  line-height: 1.355556;
  letter-spacing: 0;
  text-align: left;
  text-wrap: wrap;
  color: #2d170a;
}

.warm-hero h1 .hero-title-line {
  display: block;
}

.warm-hero h1 .hero-title-line:nth-child(1) {
  font-size: 3.35cqw;
}

.warm-hero h1 .hero-title-line:nth-child(2) {
  font-size: 3.45cqw;
}

/* 副文案两行：字号 48、行高 68、行1字距 2px 行2字距 7px、色 #3a1f12、微软雅黑 Regular */
.warm-hero__content .hero-description {
  margin: 4.05cqh 0 0;
  padding: 0;
  font-family: var(--font-sans);
  font-size: 1.333333cqw;
  font-weight: 400;
  line-height: 1.416667;
  text-align: left;
  text-wrap: wrap;
  color: #3a1f12;
}

.warm-hero__content .hero-description .hero-description-line {
  display: block;
}

.warm-hero__content .hero-description .hero-description-line:nth-child(1) {
  letter-spacing: 0.0416667em;
}

.warm-hero__content .hero-description .hero-description-line:nth-child(2) {
  letter-spacing: 0.145833em;
}

/* CTA：左右居中、贴 Hero 底部。直接挂在 .warm-hero 下（与 __content 同级），
   绝对定位参考 .warm-hero 容器；cq 单位按容器宽高缩放。
   呼吸动画驱动 scale，keyframes 内同时保留 translateX(-50%) 以维持居中。 */
.warm-hero .hero-cta {
  position: absolute;
  z-index: 2;
  bottom: 2cqh;
  left: 50%;
  width: 15.361111cqw;
  height: 14.4375cqh;
  display: block;
  transform: translateX(-50%) scale(1);
  transform-origin: center bottom;
  animation: hero-cta-breathe 2.6s ease-in-out infinite;
  text-decoration: none;
  color: #3b3028;
  cursor: pointer;
}

/* 箭头：CTA 区内居中、向下指。垂直定位用百分比（相对 CTA 盒子），字号 cq 按 .warm-hero 缩放。 */
.warm-hero .hero-cta__arrow {
  position: absolute;
  left: 50%;
  top: 38.528139%; /* 89px / 231px */
  width: auto;
  height: 26.190476%; /* 60.5px / 231px */
  transform: translateX(-50%);
}

/* CTA 文字：思源宋体 Medium、底部居中。bottom 用百分比相对 CTA，字号 cq 按 .warm-hero 缩放。 */
.warm-hero .hero-cta__text {
  position: absolute;
  left: 50%;
  bottom: 6.060606%; /* 14px / 231px */
  transform: translateX(-50%);
  font-family: var(--font-display);
  font-size: 1cqw;
  font-weight: 500;
  text-align: center;
  letter-spacing: 0.138889em;
  line-height: 1.888889;
  white-space: nowrap;
  color: #3b3028;
}

@keyframes hero-cta-breathe {
  0%, 100% { transform: translateX(-50%) scale(1); }
  50%      { transform: translateX(-50%) scale(1.06); }
}

.warm-button,
.warm-text-link {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  font: inherit;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
}

.warm-button {
  border-radius: 10px;
  padding: 11px 22px;
}

.warm-button--primary {
  color: var(--cozy-on-primary);
  background: var(--cozy-primary);
}

.warm-button--primary:hover { background: var(--cozy-primary-hover); }

.warm-button--inverse {
  color: var(--cozy-cta-alt-text);
  background: var(--cozy-cta-alt-bg);
}

.warm-button--inverse:hover { filter: brightness(0.94); }

.warm-text-link {
  color: var(--cozy-primary);
}

.warm-text-link:hover,
.warm-text-link--light:hover { text-decoration: underline; text-underline-offset: 5px; }
.warm-text-link--light { color: var(--cozy-on-surface-alt); }

.warm-membership h2 { font-size: clamp(1.75rem, 3vw, 2.4rem); line-height: 1.2; font-weight: 500; }
.membership-header__kicker { margin: 0 0 8px; font-size: 11px; font-weight: 500; letter-spacing: .18em; text-transform: uppercase; color: var(--cozy-muted-on-alt); }
.membership-header p { max-width: 34em; margin: 12px 0 0; color: var(--cozy-muted-on-alt); font-size: 15px; line-height: 1.7; }

.membership-hairline { width: 100%; height: 1px; margin-block: clamp(28px, 3vw, 40px); background: rgba(255,255,255,.1); }

/* ── 等级 + 成长进度 - 居中单列 ── */
.membership-progress-section { text-align: center; padding-block: clamp(48px, 6vw, 72px); }
.membership-progress__kicker { margin: 0 0 32px; font-size: 11px; font-weight: 500; letter-spacing: .14em; text-transform: uppercase; color: var(--cozy-muted-on-alt); }
.membership-progress__level-en { margin: 0; font-family: var(--font-display); font-size: clamp(48px, 6.5vw, 72px); font-weight: 500; line-height: 1; color: var(--cozy-on-surface-alt); }
.membership-progress__level-cn { margin: 8px 0 0; font-size: 14px; color: var(--cozy-muted-on-alt); }
.membership-progress__exp-val { margin: 24px 0 0; font-family: var(--font-display); font-size: 32px; font-weight: 500; color: var(--cozy-on-surface-alt); }
.membership-progress__exp-unit { font-size: 13px; letter-spacing: .06em; color: var(--cozy-muted-on-alt); }

/* 线与圆点公用 top:50% 容器，保证圆心必定在线上 */
.membership-progress__line { position: relative; height: 12px; margin: clamp(32px, 5vw, 48px) auto 0; max-width: 360px; background: linear-gradient(rgba(255,255,255,.20), rgba(255,255,255,.20)) no-repeat center / 100% 1px; }
.membership-progress__line-dot { position: absolute; top: 50%; left: 0; transform: translate(-50%, -50%); width: 6px; height: 6px; border-radius: 50%; background: var(--cozy-on-surface-alt); transition: left 1s ease-out; }

.membership-progress__next { margin-top: 20px; text-align: center; }
.membership-progress__next-en { display: block; font-family: var(--font-display); font-size: clamp(1.3rem, 1.8vw, 1.6rem); color: var(--cozy-on-surface-alt); opacity: .45; }
.membership-progress__next-cn { display: block; margin-top: 6px; font-size: 12px; color: var(--cozy-muted-on-alt); }
.membership-progress__max { margin-top: 32px; font-family: var(--font-display); font-size: 1rem; letter-spacing: .04em; color: var(--cozy-on-surface-alt); }

/* ── 消费 & 兑换示例（Editorial 双栏） ── */
.membership-activity { padding-block: clamp(28px, 3vw, 40px); }

/* Group 1: Header — inline with dot separator */
.membership-activity__header { margin: 0 0 24px; font-size: 11px; font-weight: 600; letter-spacing: .15em; text-transform: uppercase; color: #8C7A6B; }
.membership-activity__header-dot { font-weight: 400; color: var(--cozy-muted-on-alt); }
.membership-activity__seq { font-family: var(--font-display); font-size: 13px; font-weight: 400; letter-spacing: 0; text-transform: none; color: var(--cozy-muted-on-alt); }

/* Two-column grid */
.membership-activity__grid { display: grid; grid-template-columns: 1fr 1fr; gap: clamp(24px, 4vw, 48px); }
.membership-activity__col { min-width: 0; }

/* Group 2: Product row + border */
.membership-activity__row { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 14px; border-bottom: 1px solid rgba(255,255,255,.08); }
.membership-activity__name { font-size: clamp(1.1rem, 1.4vw, 1.3rem); font-weight: 600; color: var(--cozy-on-surface-alt); letter-spacing: .02em; }
.membership-activity__price { font-family: var(--font-display); font-size: 15px; color: var(--cozy-muted-on-alt); }
.membership-activity__price--strike { text-decoration: line-through; opacity: .45; margin-right: 6px; }

/* Group 3: Earned / Redeem block — tight grouping */
.membership-activity__earned-kicker { margin: 24px 0 0; font-size: 10px; font-weight: 600; letter-spacing: .15em; text-transform: uppercase; color: #8C7A6B; }
.membership-activity__earned-row { display: flex; align-items: baseline; gap: 8px; margin-top: 6px; }
.membership-activity__earned-num { font-family: var(--font-display); font-size: clamp(1.75rem, 2.4vw, 2.25rem); font-weight: 700; color: var(--cozy-on-surface-alt); line-height: 1; }
.membership-activity__earned-unit { font-size: 14px; font-weight: 600; letter-spacing: .08em; color: var(--cozy-muted-on-alt); }
.membership-activity__earned-plus { margin: 0 4px; font-size: 18px; font-weight: 300; color: var(--cozy-muted-on-alt); }
.membership-activity__bonus-note { margin-top: 10px; font-size: 12px; color: rgba(255,255,255,.45); }
.membership-activity__col-narrative { margin-top: 18px; font-family: var(--font-display); font-size: 12px; letter-spacing: .02em; color: var(--cozy-muted-on-alt); line-height: 2; }

/* ── 权益亮点 - 单列编号纵向 ── */
.membership-benefits { max-width: 760px; margin: 0 auto; padding-block: clamp(28px, 3vw, 40px); }
.membership-benefits__kicker { margin: 0 0 clamp(28px, 3vw, 40px); font-size: 11px; font-weight: 500; letter-spacing: .14em; text-transform: uppercase; color: var(--cozy-muted-on-alt); }
.benefit-item { display: grid; grid-template-columns: 80px 1fr; gap: 20px; padding: clamp(36px, 4vw, 48px) 0; border-bottom: 1px solid rgba(255,255,255,.08); }
.benefit-item:last-child { border-bottom: 0; }
.benefit-item__seq { font-family: var(--font-display); font-size: 18px; color: var(--cozy-muted-on-alt); padding-top: 2px; }
.benefit-item__en { margin: 0; font-size: 12px; font-weight: 500; letter-spacing: .14em; color: var(--cozy-on-surface-alt); }
.benefit-item__cn { margin: 8px 0 0; font-family: var(--font-display); font-size: clamp(1.1rem, 1.4vw, 1.25rem); color: var(--cozy-on-surface-alt); }
.benefit-item__desc { margin: 12px 0 0; font-family: var(--font-display); font-style: italic; font-size: 14px; color: var(--cozy-muted-on-alt); line-height: 1.7; }

/* ── CTA ── */
.membership-journey__cta { display: flex; justify-content: center; margin-top: clamp(20px, 2.6vw, 32px); }
.membership-journey__cta-link { padding: 14px 36px; border: 1px solid rgba(255,255,255,.18); font-size: 15px; font-weight: 500; color: var(--cozy-on-surface-alt); text-decoration: none; transition: border-color .22s ease, background .22s ease; }
.membership-journey__cta-link:hover { border-color: rgba(255,255,255,.4); background: rgba(255,255,255,.06); }

.honor-banner {
  position: relative;
  width: 100%;
  min-height: 333px;
  margin-inline: 0;
  display: grid;
  align-items: start;
  isolation: isolate;
  overflow: hidden;
  color: #f1d3a3;
  background: #1d1009;
}
.honor-banner__media,
.honor-banner__media img,
.honor-banner__scrim {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.honor-banner__media,
.honor-banner__scrim {
  width: min(100%, 1716px);
  margin-inline: auto;
}
.honor-banner__media img { display: block; object-fit: cover; object-position: center; }
.honor-banner__scrim { z-index: 1; background: linear-gradient(90deg, rgba(19, 10, 6, .26), rgba(19, 10, 6, .08) 42%, transparent 60%); }
.honor-banner__content { position: relative; z-index: 2; align-self: center; width: min(100%, 1716px); margin-inline: auto; padding: 0 clamp(32px, 6.5vw, 112px); }
.honor-banner h2 { margin: 0; color: #e8c99a; font-family: var(--font-display); font-size: clamp(2.25rem, 3.1vw, 3.125rem); line-height: 1.2; font-weight: 500; letter-spacing: 0.05em; text-align: left; text-wrap: balance; }
.honor-banner__content p { max-width: 28em; margin: 23px 0 0; color: #f4e7d3; font-size: clamp(1rem, 1.15vw, 1.22rem); line-height: 1.6; letter-spacing: 0.05em; }
.honor-banner__link { min-height: 56px; display: inline-flex; align-items: center; gap: 12px; margin-top: 28px; padding: 13px 46px; border: 1px solid rgba(232, 201, 154, .72); border-radius: 999px; color: #f3d7aa; font-size: 17px; font-weight: 400; text-decoration: none; transition: color .22s ease, background .22s ease, border-color .22s ease; }
.honor-banner__link:hover { color: #24140b; border-color: #f0cf9d; background: #f0cf9d; }

.warm-membership { min-height: 620px; padding-block: 128px; color: var(--cozy-on-surface-alt); background: oklch(0.22 0.025 42); }
.membership-shell { min-height: 360px; }

.warm-membership :where(a, button, summary):focus-visible { outline-color: var(--cozy-on-surface-alt); }

.benefits-details { margin-top: 48px; border-top: 1px solid var(--cozy-border-on-alt); }
.benefits-details summary { min-height: 52px; display: inline-flex; align-items: center; color: var(--cozy-on-surface-alt); font-weight: 600; cursor: pointer; }
.benefits-details--compact { margin: 0; border: 0; }
.benefits-details--compact p { max-width: 44em; color: var(--cozy-muted-on-alt); line-height: 1.75; }

.benefits-table-wrap { overflow-x: auto; }
.benefits-details table { width: 100%; border-collapse: collapse; text-align: left; }
.benefits-details caption { padding: 16px 0; text-align: left; font-weight: 600; }
.benefits-details th,
.benefits-details td { padding: 16px 12px; border-bottom: 1px solid var(--cozy-border-on-alt); line-height: 1.55; }
.benefits-details th { color: var(--cozy-muted-on-alt); font-size: 14px; font-weight: 500; }
.benefits-details tr.is-current td,
.benefits-list dl.is-current { color: var(--cozy-accent-on-alt); }
.current-marker { margin-left: 6px; color: var(--cozy-accent-on-alt); font-size: 12px; font-weight: 700; }
.benefits-list { display: grid; gap: 16px; padding-top: 16px; }
.benefits-list dl { margin: 0; padding-block: 16px; border-bottom: 1px solid var(--cozy-border-on-alt); }
.benefits-list dl > div { display: grid; grid-template-columns: 96px 1fr; gap: 16px; padding-block: 6px; }
.benefits-list dt { color: var(--cozy-muted-on-alt); }
.benefits-list dd { margin: 0; line-height: 1.55; }
.benefits-list__title dd { font-weight: 700; }
.membership-error { padding-block: 64px; }
.membership-error h3 { margin: 0; font-size: 1.5rem; }
.membership-error > p { color: var(--cozy-muted-on-alt); margin-top: 12px; }
.membership-skeleton { min-height: 360px; display: grid; align-content: center; gap: 20px; }
.membership-skeleton span { display: block; height: 22px; max-width: 620px; border-radius: 6px; background: rgba(255,255,255,.11); animation: warm-pulse 1.4s ease-in-out infinite alternate; }
.membership-skeleton span:nth-child(1) { width: 52%; height: 54px; }
.membership-skeleton span:nth-child(2) { width: 76%; }
.membership-skeleton span:nth-child(3) { width: 64%; }
.membership-skeleton--content { min-height: 280px; }

@keyframes warm-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes warm-pulse { from { opacity: .45; } to { opacity: 1; } }
@media (max-width: 960px) {
  .warm-membership { padding-block: 96px; }
  .membership-benefits__grid { grid-template-columns: 1fr; gap: 28px; }
}

@media (max-width: 760px) {
  .warm-shell { width: min(100% - 32px, 620px); }
  /* 窄屏画面宽远小于 3600×1600 的设计画布，aspect-ratio 会让 Hero 过矮。
     放开比例约束、给一个保底高度；图切换到 cover 满铺（裁切优于上下留白）。
     cqw/cqh 自动按容器宽等比缩放所有字号与位置，无需在此重写字号。 */
  .warm-hero { aspect-ratio: auto; height: min(90svh, 640px); min-height: 520px; }
  .warm-hero__media img { object-fit: cover; object-position: 70% center; }
  .honor-banner { min-height: 430px; align-items: start; }
  .honor-banner__media img { object-position: 66% center; }
  .honor-banner__scrim { background: linear-gradient(90deg, rgba(20, 10, 6, .94) 0%, rgba(20, 10, 6, .72) 55%, rgba(20, 10, 6, .12) 100%); }
  .honor-banner__content { align-self: start; padding-block: 48px; }
  .membership-progress__levels { gap: 24px; }
  .membership-benefits__grid { grid-template-columns: 1fr; }
  .membership-activity__grid { grid-template-columns: 1fr; gap: 24px; }
}

@media (max-width: 560px) {
  .warm-membership { padding-block: 64px; }
}

@media (prefers-reduced-motion: reduce) {
  .warm-home *,
  .warm-home *::before,
  .warm-home *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
