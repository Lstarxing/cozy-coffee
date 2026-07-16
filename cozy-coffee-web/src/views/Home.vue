<template>
  <main class="warm-home">
    <section id="home" class="warm-hero" aria-labelledby="hero-title">
      <picture class="warm-hero__media">
        <img
          src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/hero-geography-journal-20260716.png"
          alt="阳光下的 Cozy Coffee 拿铁与杭州烘焙名片"
          fetchpriority="high"
          width="2880"
          height="1093"
          @error="handleImageError"
        >
      </picture>
      <div class="warm-hero__scrim" aria-hidden="true"></div>
      <div class="warm-shell warm-hero__content">
        <h1 id="hero-title">循着风味<br>翻开地理日志</h1>
        <p>从世界八大产区，到杭州匠心烘焙<br>每一杯，都是一次穿越土地的风味重述。</p>
        <div class="warm-hero__actions">
          <a class="warm-button warm-button--primary" href="#origins">探索产地风味之旅 <span aria-hidden="true">⟶</span></a>
        </div>
      </div>
    </section>

    <OriginsJourney />

    <EditorialMenu :products="menuItems" :flavor-routes="flavorRoutes" @image-error="handleImageError" />

    <section class="honor-banner" aria-labelledby="honor-banner-title">
      <picture class="honor-banner__media" aria-hidden="true">
        <source
          type="image/avif"
          srcset="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-768.avif 768w, https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-1440.avif 1440w, https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-1920.avif 1920w"
          sizes="(min-width: 1716px) 1716px, 100vw"
        >
        <source
          type="image/webp"
          srcset="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-768.webp 768w, https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-1440.webp 1440w, https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-1920.webp 1920w"
          sizes="(min-width: 1716px) 1716px, 100vw"
        >
        <img
          src="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-1440.jpg"
          srcset="https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-768.jpg 768w, https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-1440.jpg 1440w, https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com/images/home/honor-banner-1920.jpg 1920w"
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
  height: clamp(560px, 37vw, 640px);
  min-height: 540px;
  max-height: calc(100svh - var(--nav-height));
  display: grid;
  align-items: center;
  isolation: isolate;
  overflow: hidden;
  background: #eee1d2;
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
  object-position: center;
}

.warm-hero__scrim {
  z-index: 1;
  background: transparent;
}

.warm-hero__content {
  position: relative;
  z-index: 2;
  align-self: center;
  width: 100%;
  margin-inline: auto;
  padding-inline: clamp(72px, 6.25vw, 120px);
  color: #402417;
  transform: translateY(20px);
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
  max-width: 480px;
  color: #3a2415;
  font-family: var(--font-display);
  font-size: clamp(3.25rem, 3.2vw, 4rem);
  font-weight: 500;
  line-height: 1.18;
  letter-spacing: 0.01em;
  text-align: left;
  text-wrap: balance;
}

.warm-hero__content > p {
  max-width: 23em;
  margin: 24px 0 0;
  color: #684331;
  font-family: var(--font-sans);
  font-size: clamp(0.98rem, 1.08vw, 1.0625rem);
  font-weight: 400;
  line-height: 1.72;
  letter-spacing: 0.025em;
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

.warm-hero__actions {
  margin-top: 36px;
}

.warm-hero__actions .warm-button--primary {
  min-height: 52px;
  gap: 18px;
  padding: 13px 26px;
  border-radius: 5px;
  background: #8B5334;
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.035em;
  transition: background .25s ease, transform .25s ease, box-shadow .25s ease;
}

.warm-hero__actions .warm-button--primary:hover {
  background: #74452B;
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(58, 36, 21, .12);
}

.warm-hero__actions .warm-button--primary span {
  font-size: 19px;
  font-weight: 300;
  line-height: 1;
  transform: translateY(-1px);
}

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
  .warm-hero { height: min(620px, calc(100svh - var(--nav-height))); min-height: 520px; align-items: start; }
  .warm-hero__media img { object-position: 66% center; }
  .warm-hero__scrim { background: linear-gradient(90deg, rgba(249, 241, 231, .92) 0%, rgba(249, 241, 231, .66) 48%, rgba(249, 241, 231, .08) 82%); }
  .warm-shell.warm-hero__content,
  .warm-shell.honor-banner__content { width: 100%; margin-inline: 0; padding-inline: 16px; }
  .warm-hero__content { align-self: start; padding-block: 48px; transform: none; }
  .warm-hero h1 { max-width: 10em; font-size: clamp(2.5rem, 11vw, 3rem); line-height: 1.2; }
  .warm-hero__content > p { max-width: 21em; color: #684331; }
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
  .warm-hero__actions,
  .membership-actions { align-items: stretch; flex-direction: column; }
  .warm-hero__actions { align-items: flex-start; }
  .warm-hero__actions .warm-button { width: auto; }
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
