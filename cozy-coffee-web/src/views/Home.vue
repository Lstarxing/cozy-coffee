<template>
  <main class="warm-home">
    <section id="home" class="warm-hero" aria-labelledby="hero-title">
      <picture class="warm-hero__media">
        <source
          type="image/avif"
          srcset="/images/hero/hero-720.avif 720w, /images/hero/hero-1080.avif 1080w, /images/hero/hero-1440.avif 1440w, /images/hero/hero-1920.avif 1920w"
          sizes="100vw"
        >
        <source
          type="image/webp"
          srcset="/images/hero/hero-720.webp 720w, /images/hero/hero-1080.webp 1080w, /images/hero/hero-1440.webp 1440w, /images/hero/hero-1920.webp 1920w"
          sizes="100vw"
        >
        <img
          src="/images/hero/hero-1440.jpg"
          srcset="/images/hero/hero-720.jpg 720w, /images/hero/hero-1080.jpg 1080w, /images/hero/hero-1440.jpg 1440w, /images/hero/hero-1920.jpg 1920w"
          sizes="100vw"
          alt="咖啡师将细腻奶泡注入新鲜萃取的咖啡"
          fetchpriority="high"
          width="1440"
          height="900"
          @error="handleImageError"
        >
      </picture>
      <div class="warm-hero__scrim" aria-hidden="true"></div>
      <div class="warm-shell warm-hero__content">
        <h1 id="hero-title">把产地的风味，<br>留在这一杯里</h1>
        <p>全球八处产区 ｜ 小批次新鲜烘焙</p>
        <div class="warm-hero__actions">
          <a class="warm-button warm-button--primary" href="#menu">查看菜单</a>
          <a class="warm-text-link warm-text-link--light" href="#membership">会员如何回馈每一杯 →</a>
        </div>
      </div>
    </section>

    <OriginsJourney />

    <section id="menu" class="warm-section warm-menu" aria-labelledby="menu-title">
      <div class="warm-shell">
        <header class="warm-section__header warm-section__header--split">
          <div>
            <h2 id="menu-title">今天，想喝哪一杯</h2>
            <p>六种熟悉的表达，各自保留豆子的清晰个性。</p>
          </div>
          <router-link class="warm-text-link" to="/member/order">浏览完整菜单 →</router-link>
        </header>

        <div class="menu-grid" role="list">
          <article v-for="item in menuItems" :key="item.name" class="menu-product" role="listitem">
            <div class="menu-product__image image-frame">
              <img :src="item.image" :alt="item.alt" loading="lazy" width="720" height="600" @error="handleImageError">
            </div>
            <div class="menu-product__heading">
              <h3>{{ item.name }}</h3>
              <strong>{{ item.price }}</strong>
            </div>
            <p class="menu-product__flavor">{{ item.flavor }}</p>
            <router-link class="menu-product__action" to="/member/order" :aria-label="`点单：${item.name}`">去点单 →</router-link>
          </article>
        </div>

        <aside class="award-proof" aria-label="品牌获奖信息">
          <p><strong>连续 5 年 IIAC 金奖</strong><span>从选豆、烘焙到出品，以稳定兑现每一杯的品质。</span></p>
        </aside>
        <p class="warm-transition">选好这一杯，也开始记录下一杯。</p>
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
import { useUserStore } from '@/stores/user'
import {
  calculateEarnedPoints,
  calculateProgress,
  calculateRedemptionCost,
  POINTS_RATES_BY_LEVEL
} from '@/utils/homepageMembership'

const userStore = useUserStore()
const membershipState = ref('auth-resolving')
const memberInfo = ref(null)
const monthlyTask = ref(null)
const isMobile = ref(false)
let mediaQuery = null
let disposed = false

const menuItems = [
  { name: '美式咖啡', price: '¥18', flavor: '黑巧克力、烤坚果，收口干净。', image: '/images/cafe1.png', alt: '盛在玻璃杯中的冰美式咖啡' },
  { name: '原味拿铁', price: '¥25', flavor: '焦糖甜感与柔和奶香，圆润平衡。', image: '/images/cafe2.jpg', alt: '带细腻奶泡的原味拿铁' },
  { name: '卡布奇诺', price: '¥30', flavor: '浓缩、热奶与奶泡比例清晰。', image: '/images/cafe3.jpg', alt: '撒有可可粉的卡布奇诺' },
  { name: '摩卡咖啡', price: '¥30', flavor: '可可香气托住深烘咖啡的厚度。', image: '/images/cafe4.jpg', alt: '巧克力风味的摩卡咖啡' },
  { name: '抹茶拿铁', price: '¥30', flavor: '茶感鲜明，奶香克制，微苦回甘。', image: '/images/cafe5.jpg', alt: '绿色抹茶与牛奶调制的抹茶拿铁' },
  { name: '生椰拿铁', price: '¥28', flavor: '清爽椰香与浓缩咖啡自然衔接。', image: '/images/cafe6.jpg', alt: '分层呈现的生椰拿铁' }
]

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

<style scoped>
.warm-home {
  background: var(--cozy-bg);
  color: var(--cozy-ink);
  font-family: var(--font-sans);
  overflow: clip;
}

.warm-shell {
  width: min(1180px, calc(100% - 48px));
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
  height: 70svh;
  min-height: min(560px, calc(100svh - var(--nav-height)));
  max-height: 760px;
  display: grid;
  align-items: end;
  isolation: isolate;
  overflow: hidden;
}

.warm-hero__media,
.warm-hero__media img,
.warm-hero__scrim {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.warm-hero__media img {
  object-fit: cover;
  object-position: center 56%;
}

.warm-hero__scrim {
  z-index: 1;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.25), rgba(0, 0, 0, 0.64));
}

.warm-hero__content {
  position: relative;
  z-index: 2;
  color: white;
  padding-block: 64px;
  animation: warm-fade 700ms ease-out both;
}

.warm-hero h1,
.warm-section h2,
.warm-membership h2 {
  margin: 0;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.warm-hero h1 {
  max-width: 9.5em;
  font-size: clamp(2.75rem, 6vw, 5rem);
  line-height: 1.08;
  color: var(--cozy-on-surface-alt);
}

.warm-hero__content > p {
  margin: 24px 0 0;
  font-size: clamp(1rem, 1.5vw, 1.125rem);
  line-height: 1.7;
}

.warm-hero__actions,
.membership-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 24px;
  margin-top: 32px;
}

.warm-button,
.warm-text-link,
.menu-product__action {
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

.warm-section { padding-block: 128px; }
.warm-section__header { max-width: 720px; margin-bottom: 64px; }
.warm-section__header--split { max-width: none; display: flex; align-items: end; justify-content: space-between; gap: 32px; }
.warm-section h2,
.warm-membership h2 { font-size: clamp(2rem, 4vw, 3.25rem); line-height: 1.15; }
.warm-section__header p,
.membership-header p { max-width: 34em; margin: 20px 0 0; color: var(--cozy-muted); font-size: 17px; line-height: 1.75; }

.image-frame { background: var(--cozy-surface); overflow: hidden; }
.image-frame img { width: 100%; height: 100%; display: block; object-fit: cover; }
.image-frame img.image-fallback { filter: saturate(0.55); }

.warm-transition { margin: 96px 0 0; font-size: clamp(1.45rem, 2.7vw, 2.25rem); line-height: 1.5; text-align: center; }

.warm-menu { background: var(--cozy-surface); }
.menu-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 56px 24px; }
.menu-product { min-width: 0; }
.menu-product__image { aspect-ratio: 6 / 5; border-radius: 8px; }
.menu-product__image img { transition: transform 300ms ease; }
.menu-product:hover .menu-product__image img,
.menu-product:focus-within .menu-product__image img { transform: scale(1.025); }
.menu-product__heading { display: flex; justify-content: space-between; align-items: baseline; gap: 16px; margin-top: 20px; }
.menu-product__heading h3 { margin: 0; font-size: 1.2rem; font-weight: 600; }
.menu-product__heading strong { white-space: nowrap; font-size: 1.05rem; }
.menu-product__flavor { min-height: 3.4em; margin: 10px 0 0; color: var(--cozy-muted); line-height: 1.7; opacity: 0.74; transition: opacity 200ms ease; }
.menu-product:hover .menu-product__flavor,
.menu-product:focus-within .menu-product__flavor { opacity: 1; }
.menu-product__action { justify-content: flex-start; margin-top: 8px; color: var(--cozy-primary); }
.menu-product__action:hover { text-decoration: underline; text-underline-offset: 5px; }
.award-proof { margin-top: 96px; padding-block: 24px; border-block: 1px solid var(--cozy-border); }
.award-proof p { display: flex; align-items: baseline; justify-content: center; gap: 20px 32px; margin: 0; color: var(--cozy-muted); line-height: 1.7; }
.award-proof strong { color: var(--cozy-ink); font-size: 1.15rem; }

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
  .warm-section,
  .warm-membership { padding-block: 96px; }
  .menu-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .membership-story { grid-template-columns: 1fr 1fr; }
  .member-return { grid-column: 1 / -1; }
}

@media (max-width: 760px) {
  .warm-shell { width: min(100% - 32px, 620px); }
  .warm-hero { min-height: min(520px, calc(100svh - var(--nav-height))); }
  .warm-hero__content { padding-block: 48px; }
  .warm-section__header--split,
  .membership-header { align-items: flex-start; flex-direction: column; }
  .membership-header .membership-kicker { text-align: left; }
  .warm-transition { margin-top: 72px; text-align: left; }
  .award-proof p { align-items: flex-start; flex-direction: column; }
  .membership-story { grid-template-columns: 1fr; gap: 40px; }
  .member-return { grid-column: auto; }
}

@media (max-width: 560px) {
  .warm-section,
  .warm-membership { padding-block: 64px; }
  .warm-section__header { margin-bottom: 48px; }
  .menu-grid { grid-template-columns: 1fr; gap: 48px; }
  .menu-product__flavor { min-height: 0; opacity: 1; }
  .warm-hero__actions,
  .membership-actions { align-items: stretch; flex-direction: column; }
  .warm-hero__actions .warm-button,
  .membership-actions .warm-button { width: 100%; }
  .warm-text-link { justify-content: flex-start; }
}

@media (hover: none), (pointer: coarse) {
  .menu-product__flavor { opacity: 1; }
  .menu-product__action { min-height: 48px; }
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
