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
            <div>
              <h2 id="membership-title">这一杯，也在回馈下一杯</h2>
              <p>消费、积累、兑换。把会员回报放进一次真实的点单里。</p>
            </div>
            <p class="membership-kicker">{{ membershipKicker }}</p>
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
            <div class="membership-story">
              <div class="points-transaction">
                <p class="transaction-product"><strong>¥32</strong> 手冲咖啡</p>
                <p class="transaction-equation">基础积分 32 <span>＋</span> 等级加成 {{ earnedPoints - 32 }}</p>
                <p class="points-earned">{{ earnedPoints }} 积分到账</p>
              </div>

              <div class="points-progress">
                <div class="points-progress__labels">
                  <span>当前积分 {{ currentPoints }} / {{ rewardTarget }}</span>
                  <strong>{{ progressPercent }}%</strong>
                </div>
                <progress :value="Math.min(currentPoints, rewardTarget)" :max="rewardTarget" :aria-label="`距5元代金券进度 ${progressPercent}%`">{{ progressPercent }}%</progress>
                <p v-if="remainingPoints > 0">距 5 元代金券还差 {{ remainingPoints }} 积分</p>
                <p v-else class="is-unlocked">已达到 5 元代金券兑换积分</p>
                <p class="redemption-note">商品原价 150 积分 · {{ currentLevelLabel }}价 {{ rewardTarget }} 积分{{ redemptionDiscountLabel }}</p>
              </div>

              <div class="member-return">
                <p><strong>{{ currentLevelLabel }}</strong> · 每杯 {{ currentRate.toFixed(1) }}×</p>
                <p v-if="monthlyOrderCount !== null">本月订单数 {{ monthlyOrderCount }} · 周五会员日额外 +0.5×</p>
                <p v-else>周五会员日额外 +0.5× · {{ representativeBenefit }}</p>
                <p v-if="membershipState === 'anonymous'" class="unlock-note">下一杯预计获得 35 积分，足够解锁 5 元代金券</p>
              </div>
            </div>

            <div class="membership-actions">
              <router-link v-if="membershipState === 'anonymous'" class="warm-button warm-button--inverse" to="/register">立即入会</router-link>
              <router-link v-else class="warm-button warm-button--inverse" to="/member/mall">去积分商城</router-link>
              <router-link v-if="membershipState !== 'anonymous'" class="warm-text-link warm-text-link--light" to="/member">进入会员中心 →</router-link>
            </div>

            <details class="benefits-details">
              <summary>{{ membershipState === 'anonymous' ? '展开完整等级权益' : '查看完整等级权益' }}</summary>
              <div v-if="!isMobile" class="benefits-table-wrap">
                <table aria-label="会员等级速览">
                  <caption>等级成长体系</caption>
                  <thead>
                    <tr><th scope="col">等级</th><th scope="col">门槛</th><th scope="col">倍率</th><th scope="col">兑换折扣</th><th scope="col">代表权益</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="level in levels" :key="level.key" :class="{ 'is-current': isCurrentLevel(level.key) }">
                      <td>{{ level.name }} <span v-if="isCurrentLevel(level.key)" class="current-marker">当前</span></td>
                      <td>{{ level.threshold }}</td><td>{{ level.rate }}</td><td>{{ level.discount }}</td><td>{{ level.benefit }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="benefits-list" aria-label="会员等级速览">
                <dl v-for="level in levels" :key="level.key" :class="{ 'is-current': isCurrentLevel(level.key) }">
                  <div class="benefits-list__title"><dt>等级</dt><dd>{{ level.name }} <span v-if="isCurrentLevel(level.key)" class="current-marker">当前</span></dd></div>
                  <div><dt>门槛</dt><dd>{{ level.threshold }}</dd></div>
                  <div><dt>倍率</dt><dd>{{ level.rate }}</dd></div>
                  <div><dt>兑换折扣</dt><dd>{{ level.discount }}</dd></div>
                  <div><dt>代表权益</dt><dd>{{ level.benefit }}</dd></div>
                </dl>
              </div>
            </details>
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
  { key: 'basic', name: '基础 Classic', threshold: '0 EXP', rate: '1.0×', discount: '—', benefit: '周五额外 +0.5× 积分' },
  { key: 'silver', name: '白银 Silver', threshold: '500 EXP', rate: '1.1×', discount: '9.8 折', benefit: '生日买一赠一券' },
  { key: 'gold', name: '黄金 Gold', threshold: '1,500 EXP', rate: '1.2×', discount: '9.5 折', benefit: '生日买一赠一券 + 8.8折券×2' },
  { key: 'diamond', name: '钻石 Diamond', threshold: '4,000 EXP', rate: '1.3×', discount: '9.0 折', benefit: '免单券 + 买一赠一券×2' },
  { key: 'black', name: '黑金 Black', threshold: '9,000 EXP', rate: '1.5×', discount: '8.5 折', benefit: '免单券×2 + 无限免配送' }
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
const earnedPoints = computed(() => calculateEarnedPoints(32, normalizedLevel.value, false))
const remainingPoints = computed(() => Math.max(0, rewardTarget.value - currentPoints.value))
const progressPercent = computed(() => calculateProgress(currentPoints.value, rewardTarget.value))
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

/* Bridge 升级：上下短分隔线 + 两行标题 + ↓ 呼吸 SVG（无 CTA）。
   padding-block 比之前略小，让 Bridge 不抢戏；honor banner 自己承担 CTA。 */
.menu-bridge {
  padding-block: clamp(64px, 7vw, 96px);
  background: var(--cozy-surface);
  color: var(--cozy-ink);
}
.menu-bridge__shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: clamp(18px, 2.6vw, 28px);
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

.warm-membership h2 { font-size: clamp(2rem, 4vw, 3.25rem); line-height: 1.15; }
.membership-header p { max-width: 34em; margin: 20px 0 0; color: var(--cozy-muted); font-size: 17px; line-height: 1.75; }

.image-frame { background: var(--cozy-surface); overflow: hidden; }
.image-frame img { width: 100%; height: 100%; display: block; object-fit: cover; }
.image-frame img.image-fallback { filter: saturate(0.55); }

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

.warm-membership { min-height: 620px; padding-block: 128px; color: var(--cozy-on-surface-alt); background: var(--cozy-surface-alt); }
.membership-shell { min-height: 360px; }
.membership-header { display: flex; align-items: end; justify-content: space-between; gap: 32px; padding-bottom: 48px; border-bottom: 1px solid var(--cozy-border-on-alt); }
.membership-header p { color: var(--cozy-muted-on-alt); }
.membership-header .membership-kicker { flex: 0 0 auto; margin: 0; font-size: 14px; text-align: right; }
.membership-story { display: grid; grid-template-columns: 1fr 1.15fr 1fr; gap: 48px; padding-block: 56px; border-bottom: 1px solid var(--cozy-border-on-alt); }
.points-transaction,
.points-progress,
.member-return { min-width: 0; }
.transaction-product,
.transaction-equation,
.points-earned,
.points-progress p,
.member-return p { margin: 0; line-height: 1.75; }
.transaction-product { font-size: 1.15rem; }
.transaction-product strong { margin-right: 8px; font-size: clamp(2rem, 4vw, 3.25rem); font-weight: 600; }
.transaction-equation { margin-top: 16px; color: var(--cozy-muted-on-alt); }
.transaction-equation span { padding-inline: 6px; }
.points-earned { margin-top: 18px; color: var(--cozy-accent-on-alt); font-size: 1.4rem; font-weight: 700; }
.points-progress__labels { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 12px; color: var(--cozy-muted-on-alt); }
.points-progress__labels strong { color: var(--cozy-on-surface-alt); }
.points-progress progress { width: 100%; height: 10px; overflow: hidden; border: 0; border-radius: 999px; background: rgba(255,255,255,.14); accent-color: var(--cozy-accent-on-alt); }
.points-progress progress::-webkit-progress-bar { background: rgba(255,255,255,.14); border-radius: inherit; }
.points-progress progress::-webkit-progress-value { background: var(--cozy-accent-on-alt); border-radius: inherit; }
.points-progress progress::-moz-progress-bar { background: var(--cozy-accent-on-alt); border-radius: inherit; }
.points-progress > p { margin-top: 14px; }
.points-progress .is-unlocked { color: var(--cozy-accent-on-alt); font-weight: 700; }
.points-progress .redemption-note { color: var(--cozy-muted-on-alt); font-size: 14px; }
.member-return p + p { margin-top: 10px; color: var(--cozy-muted-on-alt); }
.member-return .unlock-note { margin-top: 24px; color: var(--cozy-on-surface-alt); }
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
  .membership-story { grid-template-columns: 1fr 1fr; }
  .member-return { grid-column: 1 / -1; }
}

@media (max-width: 760px) {
  .warm-shell { width: min(100% - 32px, 620px); }
  /* 窄屏画面宽远小于 3600×1600 的设计画布，aspect-ratio 会让 Hero 过矮。
     放开比例约束、给一个保底高度；图切换到 cover 满铺（裁切优于上下留白）。
     cqw/cqh 自动按容器宽等比缩放所有字号与位置，无需在此重写字号。 */
  .warm-hero { aspect-ratio: auto; height: min(90svh, 640px); min-height: 520px; }
  .warm-hero__media img { object-fit: cover; object-position: 70% center; }
  .membership-header { align-items: flex-start; flex-direction: column; }
  .membership-header .membership-kicker { text-align: left; }
  .honor-banner { min-height: 430px; align-items: start; }
  .honor-banner__media img { object-position: 66% center; }
  .honor-banner__scrim { background: linear-gradient(90deg, rgba(20, 10, 6, .94) 0%, rgba(20, 10, 6, .72) 55%, rgba(20, 10, 6, .12) 100%); }
  .honor-banner__content { align-self: start; padding-block: 48px; }
  .membership-story { grid-template-columns: 1fr; gap: 40px; }
  .member-return { grid-column: auto; }
}

@media (max-width: 560px) {
  .warm-membership { padding-block: 64px; }
  .membership-actions { align-items: stretch; flex-direction: column; }
  .membership-actions .warm-button { width: 100%; }
  .warm-text-link { justify-content: flex-start; }
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
