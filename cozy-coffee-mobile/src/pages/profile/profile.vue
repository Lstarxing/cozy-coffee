<!-- 个人中心：会员账户主卡（对齐首页 hero 多层质感）+ 订单双入口 + 功能金刚区。 -->
<template>
  <view class="profile-page">
    <!-- 会员主卡：与首页 member-panel 同款多层装饰质感 -->
    <view class="member-hero" :class="[themeLevel, { 'is-dark': isDark }]" :style="themeStyle">
      <view class="layer-texture" />
      <view class="layer-shine" />
      <view v-if="themeLevel === 'diamond'" class="layer-hologram" />
      <view v-if="themeLevel === 'black'" class="layer-gold-foil" />

      <view class="hero-topline">
        <view>
          <view class="hero-brand-row">
            <LevelBadge :level="themeLevel" color="var(--member-accent)" :size="44" />
            <text class="hero-brand">COZY MEMBER</text>
          </view>
          <text class="hero-caption">Coffee, points and daily rituals.</text>
        </view>
        <view class="settings-entry" @click="navigateTo('/pages/settings/index')">设置</view>
      </view>

      <view class="identity-row" @click="handleAccount">
        <image :src="avatarUrl" class="user-avatar" mode="aspectFill" />
        <view class="identity-copy">
          <text class="user-name cozy-display">{{ displayName }}</text>
          <text class="user-meta">{{ isLoggedIn ? getMemberLevelName(userLevel) : '登录后同步积分、券包与订单' }}</text>
        </view>
        <text class="identity-action">{{ isLoggedIn ? '账户' : '登录' }} →</text>
      </view>

      <view class="member-ledger">
        <view class="ledger-primary" @click="navigateTo('/pages/points/history')">
          <text class="ledger-value cozy-display">{{ memberInfo.currentPoints || 0 }}</text>
          <text class="ledger-label">当前积分</text>
        </view>
        <view class="ledger-item" @click="navigateTo('/pages/coupon/list')">
          <text class="ledger-value small">{{ couponStats.exchange }}</text>
          <text class="ledger-label">兑换券</text>
        </view>
        <view class="ledger-item" @click="navigateTo('/pages/coupon/list')">
          <text class="ledger-value small">{{ regularCouponCount }}</text>
          <text class="ledger-label">优惠券</text>
        </view>
      </view>

      <view v-if="isLoggedIn" class="level-progress">
        <view class="progress-track"><view class="progress-fill" :style="{ transform: `scaleX(${progressPercent / 100})` }" /></view>
        <text class="progress-copy">{{ progressText }}</text>
      </view>
    </view>

    <!-- 订单双入口：咖啡订单 / 兑换订单 -->
    <view class="content-section order-section">
      <view class="section-heading">
        <view>
          <text class="section-title cozy-display">我的订单</text>
          <text class="section-note">咖啡订单进度与积分兑换记录</text>
        </view>
      </view>

      <view class="order-duo">
        <view class="order-card coffee" @click="goToOrders('coffee')">
          <view class="order-card-head">
            <text class="order-card-mark">杯</text>
            <text v-if="orderCount.coffee" class="order-card-count">{{ orderCount.coffee }}</text>
          </view>
          <text class="order-card-name">咖啡订单</text>
          <text class="order-card-note">查看制作进度与取餐</text>
        </view>
        <view class="order-card redeem" @click="goToOrders('redeem')">
          <view class="order-card-head">
            <text class="order-card-mark">礼</text>
            <text v-if="orderCount.redeem" class="order-card-count">{{ orderCount.redeem }}</text>
          </view>
          <text class="order-card-name">兑换订单</text>
          <text class="order-card-note">积分兑换商品记录</text>
        </view>
      </view>
    </view>

    <!-- 会员与功能金刚区 -->
    <view class="content-section">
      <view class="section-heading compact">
        <view>
          <text class="section-title cozy-display">会员与功能</text>
          <text class="section-note">把每次消费变成可见回馈</text>
        </view>
      </view>

      <view class="feature-grid">
        <view class="feature-item" @click="navigateTo('/pages/mall/index')">
          <view class="feature-icon primary"><text class="feature-glyph">积</text></view>
          <text class="feature-name">积分商城</text>
        </view>
        <view class="feature-item" @click="navigateTo('/pages/benefits/index')">
          <view class="feature-icon accent"><text class="feature-glyph">享</text></view>
          <text class="feature-name">会员权益</text>
        </view>
        <view class="feature-item" @click="navigateTo('/pages/signin/index')">
          <view class="feature-icon"><text class="feature-glyph">签</text></view>
          <text class="feature-name">每日签到</text>
        </view>
        <view class="feature-item" @click="navigateTo('/pages/profile/edit')">
          <view class="feature-icon"><text class="feature-glyph">我</text></view>
          <text class="feature-name">个人资料</text>
        </view>
      </view>
    </view>

    <!-- 账户设置小链接 -->
    <view class="account-links">
      <view class="account-row" @click="navigateTo('/pages/store/list')">
        <text class="account-name">自提门店</text>
        <text class="account-note">CozyCoffee 中心店</text>
        <text class="account-arrow">›</text>
      </view>
      <view class="account-row" @click="navigateTo('/pages/settings/index')">
        <text class="account-name">账户设置</text>
        <text class="account-note">安全、隐私与退出登录</text>
        <text class="account-arrow">›</text>
      </view>
      <view class="account-row" @click="navigateTo('/pages/about/index')">
        <text class="account-name">关于我们</text>
        <text class="account-note">品牌故事与咖啡旅程</text>
        <text class="account-arrow">›</text>
      </view>
    </view>

    <view class="profile-footer">
      <text class="footer-brand">COZY COFFEE</text>
      <text class="footer-copy">ROASTED IN HANGZHOU</text>
    </view>
    <DevLevelSwitcher />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getUserInfo } from '@/api/auth'
import { getMemberInfo, getMyRedemptions } from '@/api/member'
import { getCouponList } from '@/api/coupon'
import { getOrderList } from '@/api/order'
import { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS, getMemberLevelName } from '@/constants/member'
import { getImageUrl } from '@/utils/image'
import { useMemberTheme } from '@/composables/useMemberTheme'
import LevelBadge from '@/components/member/LevelBadge.vue'
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'

const userStore = useUserStore()
const { themeStyle, isDark, level: themeLevel } = useMemberTheme()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo || {})
const memberInfo = computed(() => userStore.memberInfo || {})
const userLevel = computed(() => userStore.userLevel || 'basic')
const couponStats = ref({ total: 0, exchange: 0 })
const orderCount = ref({ coffee: 0, redeem: 0 })

const avatarUrl = computed(() => {
  const avatar = userInfo.value.avatar
  if (!isLoggedIn.value || !avatar || avatar === '/images/default-avatar.png') return '/static/images/default-avatar.png'
  return getImageUrl(avatar)
})
const displayName = computed(() => isLoggedIn.value ? (userInfo.value.nickname || 'Cozy 用户') : '欢迎来到 CozyCoffee')
const regularCouponCount = computed(() => Math.max(0, couponStats.value.total - couponStats.value.exchange))

const currentExp = computed(() => Number(memberInfo.value.expTotal || 0))
const nextLevel = computed(() => {
  const index = MEMBER_LEVELS.indexOf(userLevel.value)
  return index >= 0 && index < MEMBER_LEVELS.length - 1 ? MEMBER_LEVELS[index + 1] : null
})
const progressPercent = computed(() => {
  if (!nextLevel.value) return 100
  const current = MEMBER_LEVEL_THRESHOLDS[userLevel.value] || 0
  const next = MEMBER_LEVEL_THRESHOLDS[nextLevel.value]
  return Math.max(0, Math.min(100, ((currentExp.value - current) / (next - current)) * 100))
})
const progressText = computed(() => nextLevel.value
  ? `距离${getMemberLevelName(nextLevel.value)}还需 ${Math.max(0, MEMBER_LEVEL_THRESHOLDS[nextLevel.value] - currentExp.value)} EXP`
  : '已达到当前最高等级')

onShow(async () => {
  if (!isLoggedIn.value) {
    couponStats.value = { total: 0, exchange: 0 }
    orderCount.value = { coffee: 0, redeem: 0 }
    return
  }

  try {
    const response = await getUserInfo()
    if (response.code === 200 && response.data) userStore.setLoginInfo(userStore.token, response.data)
  } catch (error) {
    console.error('获取用户资料失败', error)
  }

  try {
    const response = await getMemberInfo()
    if (response.code === 200) userStore.setMemberInfo(response.data)
  } catch (error) {
    console.error('获取会员信息失败', error)
  }

  try {
    const couponResponse = await getCouponList()
    if (couponResponse.code === 200 && couponResponse.data) {
      const availableCoupons = couponResponse.data.filter(coupon => coupon.status === 'ISSUED' && coupon.available === true)
      couponStats.value.total = availableCoupons.length
      couponStats.value.exchange = availableCoupons.filter(coupon => coupon.couponType === 'EXCHANGE').length
    }
  } catch (error) {
    console.error('获取优惠券数量失败', error)
  }

  // 轻量拉取订单数角标
  try {
    const res = await getOrderList()
    if (res.code === 200 && res.data) orderCount.value.coffee = res.data.length
  } catch (_) { /* 角标失败静默 */ }
  try {
    const res = await getMyRedemptions()
    if (res.code === 200 && res.data) orderCount.value.redeem = res.data.length
  } catch (_) { /* 角标失败静默 */ }
})

function handleAccount() {
  uni.navigateTo({ url: isLoggedIn.value ? '/pages/profile/edit' : '/pages/login/index' })
}

function navigateTo(url) {
  if (isLoggedIn.value || ['/pages/store/list', '/pages/settings/index'].includes(url)) uni.navigateTo({ url })
  else uni.navigateTo({ url: '/pages/login/index' })
}

function goToOrders(category) {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.setStorageSync('cozy_order_category', category)
  uni.switchTab({ url: '/pages/order/order' })
}
</script>

<style lang="scss" scoped>
.profile-page { min-height: 100vh; padding-bottom: 140rpx; background: $cozy-bg; }

/* ===== 会员主卡（多层装饰，对齐首页）===== */
.member-hero { position: relative; overflow: hidden; padding: 38rpx 32rpx 34rpx; background: var(--member-surface, #{$cozy-surface-alt}); color: var(--member-text, #fff); box-shadow: $cozy-shadow-raised; }
.member-hero > view, .member-hero > text { position: relative; z-index: 1; }
.layer-texture { position: absolute; left: 0; right: 0; top: 0; height: 260rpx; z-index: 0; pointer-events: none; opacity: 0.5;
  background-image: radial-gradient(rgba(255,255,255,0.06) 1rpx, transparent 1rpx); background-size: 14rpx 14rpx; }
.layer-shine { position: absolute; left: 0; right: 0; top: 0; height: 60%; z-index: 0; pointer-events: none;
  background: linear-gradient(160deg, rgba(255,255,255,0.30) 0%, rgba(255,255,255,0.06) 40%, transparent 70%); }
.member-hero.is-dark .layer-shine { background: linear-gradient(160deg, rgba(255,255,255,0.14) 0%, transparent 50%); }
.layer-hologram { position: absolute; inset: 0; z-index: 0; pointer-events: none;
  background: linear-gradient(120deg, rgba(84,110,122,0.18), rgba(207,216,220,0.22), rgba(84,110,122,0.18));
  animation: member-hologram 6s linear infinite; }
@keyframes member-hologram { 0% { filter: hue-rotate(0deg); } 100% { filter: hue-rotate(360deg); } }
.layer-gold-foil { position: absolute; inset: 0; z-index: 0; pointer-events: none;
  background-image: radial-gradient(#{$member-black-pattern} 1rpx, transparent 1rpx); background-size: 18rpx 18rpx; }
.hero-topline { position: relative; z-index: 2; display: flex; align-items: flex-start; justify-content: space-between; gap: 24rpx; }
.hero-brand-row { display: flex; align-items: center; gap: 14rpx; }
.hero-brand { display: block; color: var(--member-text, #{$cozy-muted-on-dark}); font-size: 19rpx; font-weight: 800; letter-spacing: .22em; opacity: 0.82; }
.hero-caption { display: block; margin-top: 10rpx; font-size: 17rpx; opacity: 0.7; }
.settings-entry { min-height: 62rpx; padding: 0 18rpx; display: flex; align-items: center; border: 1rpx solid var(--member-text, rgba(255,255,255,0.3)); border-radius: $cozy-radius-md; opacity: 0.85; font-size: 21rpx; }
.identity-row { position: relative; z-index: 2; margin-top: 34rpx; display: flex; align-items: center; gap: 20rpx; }
.user-avatar { width: 104rpx; height: 104rpx; flex: none; border: 2rpx solid rgba(255,255,255,.28); border-radius: 50%; background: rgba(255,255,255,0.12); }
.identity-copy { min-width: 0; flex: 1; }
.user-name { display: block; overflow: hidden; font-size: 38rpx; text-overflow: ellipsis; white-space: nowrap; }
.user-meta { display: block; margin-top: 9rpx; opacity: 0.72; font-size: 20rpx; }
.identity-action { flex: none; font-size: 22rpx; font-weight: 650; opacity: 0.85; }
.member-ledger { position: relative; z-index: 2; margin-top: 34rpx; padding-top: 27rpx; display: grid; grid-template-columns: 1.35fr 1fr 1fr; border-top: 1rpx solid rgba(255,255,255,0.16); }
.ledger-primary, .ledger-item { min-width: 0; }
.ledger-item { padding-left: 24rpx; border-left: 1rpx solid rgba(255,255,255,0.16); }
.ledger-value { display: block; font-size: 66rpx; font-weight: 760; line-height: 1; letter-spacing: -0.01em; }
.ledger-value.small { font-size: 44rpx; }
.ledger-label { display: block; margin-top: 10rpx; font-size: 19rpx; opacity: 0.72; letter-spacing: .06em; }
.level-progress { position: relative; z-index: 2; margin-top: 24rpx; }
.progress-track { height: 6rpx; overflow: hidden; border-radius: 3rpx; background: rgba(255,255,255,.18); }
.progress-fill { width: 100%; height: 100%; border-radius: inherit; background: var(--member-accent, #{$cozy-accent}); transform: scaleX(0); transform-origin: left center; transition: transform 320ms $cozy-ease-out; }
.progress-copy { display: block; margin-top: 9rpx; font-size: 18rpx; opacity: 0.72; }

/* ===== 订单双入口 ===== */
.content-section { padding: 44rpx 28rpx 0; }
.order-section { padding-top: 44rpx; }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 22rpx; padding-bottom: 22rpx; border-bottom: 1rpx solid $cozy-border; }
.section-heading.compact { padding-bottom: 20rpx; }
.section-title { display: block; color: $cozy-ink; font-size: 38rpx; }
.section-note { display: block; margin-top: 7rpx; color: $cozy-muted; font-size: 20rpx; }

.order-duo { display: flex; gap: 18rpx; padding-top: 24rpx; }
.order-card { flex: 1; padding: 26rpx 22rpx 24rpx; border-radius: $cozy-radius-lg; background: #fff; box-shadow: 0 2rpx 8rpx rgba(43,30,22,0.05); }
.order-card.coffee { background: linear-gradient(135deg, #FFF, $cozy-surface); }
.order-card.redeem { background: linear-gradient(135deg, #FFF, $cozy-accent-soft); }
.order-card-head { display: flex; align-items: center; justify-content: space-between; }
.order-card-mark { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-primary; color: #fff; font-size: 26rpx; font-weight: 750; }
.order-card.redeem .order-card-mark { background: $cozy-accent; }
.order-card-count { min-width: 36rpx; height: 36rpx; padding: 0 8rpx; display: inline-flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-surface; color: $cozy-muted; font-size: 19rpx; font-weight: 700; }
.order-card-name { display: block; margin-top: 18rpx; color: $cozy-ink; font-size: 28rpx; font-weight: 700; }
.order-card-note { display: block; margin-top: 7rpx; color: $cozy-muted; font-size: 19rpx; }

/* ===== 功能金刚区 ===== */
.feature-grid { display: flex; flex-wrap: wrap; padding-top: 24rpx; }
.feature-item { width: 25%; padding: 12rpx 6rpx; display: flex; flex-direction: column; align-items: center; }
.feature-icon { width: 88rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; border-radius: $cozy-radius-md; background: $cozy-surface; color: $cozy-primary; }
.feature-icon.primary { background: $cozy-primary; color: #fff; }
.feature-icon.accent { background: $cozy-accent-soft; color: $cozy-accent; }
.feature-glyph { font-size: 34rpx; font-weight: 750; }
.feature-name { display: block; margin-top: 14rpx; color: $cozy-ink; font-size: 22rpx; font-weight: 600; }

/* ===== 账户小链接 ===== */
.account-links { margin: 40rpx 28rpx 0; border: 1rpx solid $cozy-border; border-radius: $cozy-radius-lg; overflow: hidden; background: #fff; }
.account-row { min-height: 96rpx; display: flex; align-items: center; gap: 18rpx; padding: 0 26rpx; border-bottom: 1rpx solid $cozy-border; }
.account-row:last-child { border-bottom: 0; }
.account-name { color: $cozy-ink; font-size: 25rpx; font-weight: 650; }
.account-note { flex: 1; color: $cozy-muted; font-size: 20rpx; }
.account-arrow { color: $cozy-placeholder; font-size: 38rpx; font-weight: 300; }

/* ===== Footer ===== */
.profile-footer { padding: 62rpx 28rpx 0; text-align: center; }
.footer-brand { display: block; color: $cozy-ink; font-size: 20rpx; font-weight: 850; letter-spacing: .2em; }
.footer-copy { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 15rpx; letter-spacing: .12em; }
</style>