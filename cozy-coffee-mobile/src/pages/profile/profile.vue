<!--
  个人中心：身份卡（品牌行 + 等级徽章 + 用户名 + POINTS + 成长进度）
  + 订单双入口 + 功能金刚区（5 格）+ 服务列表 —— 对齐 prototype/profile.html
-->
<template>
  <view class="profile-page">
    <!-- 会员身份卡（紧凑） -->
    <view class="member-card" :class="{ 'is-dark': isDark }" :style="themeStyle">
      <view class="layer-shine" />

      <view class="card-top">
        <view class="brand-row">
          <CozyIcon name="coffee" :size="20" :color="memberText" />
          <text class="brand-text">COZY MEMBER</text>
        </view>
      </view>

      <view class="card-identity">
        <view class="level-emblem-wrap">
          <LevelBadge :level="themeLevel" :color="memberAccent" :size="88" />
        </view>
        <view class="identity-copy">
          <text class="user-name">{{ displayName }}</text>
          <text class="level-en">{{ levelEn }}</text>
        </view>
      </view>

      <view class="points-row" @click="handlePoints">
        <text class="points-value">{{ formatPoints(currentPoints) }}</text>
        <text class="points-label">POINTS</text>
      </view>

      <view class="level-progress">
        <view class="growth-row">
          <text class="growth-label">{{ growthLabel }}</text>
          <text class="growth-exp">{{ growthExp }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: progressPercent + '%', background: memberAccent }"></view>
        </view>
      </view>
    </view>

    <!-- 订单双入口 -->
    <view class="entry-card first">
      <view class="order-row" @click="goToOrders">
        <view class="order-ic"><CozyIcon name="bag" :size="19" color="#756A63" /></view>
        <view class="order-copy">
          <text class="order-name">我的订单</text>
          <text class="order-note">制作进度与取餐</text>
        </view>
        <text v-if="orderCount.coffee" class="order-count">{{ orderCount.coffee }}</text>
        <text class="order-arrow">›</text>
      </view>
      <view class="order-row" @click="goToRedemptions">
        <view class="order-ic"><CozyIcon name="swap" :size="19" color="#756A63" /></view>
        <view class="order-copy">
          <text class="order-name">兑换订单</text>
          <text class="order-note">积分兑换商品记录</text>
        </view>
        <text v-if="orderCount.redeem" class="order-count">{{ orderCount.redeem }}</text>
        <text class="order-arrow">›</text>
      </view>
    </view>

    <!-- 会员功能金刚区 -->
    <view class="entry-card">
      <view class="func-grid">
        <view class="f-item" @click="navigateTo('/pages/mall/index')">
          <view class="f-icon"><CozyIcon name="gift" :size="24" color="#756A63" /></view>
          <text class="f-label">积分商城</text>
        </view>
        <view class="f-item" @click="navigateTo('/pages/coupon/list')">
          <view class="f-icon"><CozyIcon name="coupon" :size="24" color="#756A63" /></view>
          <text class="f-label">优惠券</text>
        </view>
        <view class="f-item" @click="navigateTo('/pages/signin/index')">
          <view class="f-icon"><CozyIcon name="calendar" :size="24" color="#756A63" /></view>
          <text class="f-label">每日签到</text>
        </view>
        <view class="f-item" @click="navigateTo('/pages/benefits/index')">
          <view class="f-icon"><CozyIcon name="shield" :size="24" color="#756A63" /></view>
          <text class="f-label">会员权益</text>
        </view>
        <view class="f-item" @click="navigateTo('/pages/challenge/index')">
          <view class="f-icon"><CozyIcon name="target" :size="24" color="#756A63" /></view>
          <text class="f-label">月度挑战</text>
        </view>
      </view>
    </view>

    <!-- 服务 -->
    <view class="entry-card">
      <view class="svc-row" @click="navigateTo('/pages/address/list')">
        <text class="svc-name">收货地址</text>
        <text class="svc-note">配送地址管理</text>
        <text class="svc-arrow">›</text>
      </view>
      <view class="svc-row" @click="contactService">
        <text class="svc-name">联系客服</text>
        <text class="svc-note">在线咨询与反馈</text>
        <text class="svc-arrow">›</text>
      </view>
      <view class="svc-row" @click="navigateTo('/pages/settings/index')">
        <text class="svc-name">账户设置</text>
        <text class="svc-note">安全、隐私与退出登录</text>
        <text class="svc-arrow">›</text>
      </view>
      <view class="svc-row" @click="navigateTo('/pages/about/index')">
        <text class="svc-name">关于我们</text>
        <text class="svc-note">品牌故事与咖啡旅程</text>
        <text class="svc-arrow">›</text>
      </view>
    </view>

    <!-- 品牌收尾 -->
    <view class="profile-footer">
      <text class="footer-kicker">CRAFTED WITH CARE · SINCE 2026</text>
      <text class="footer-brand">COZY COFFEE</text>
      <text class="footer-tag">Roasted in Hangzhou · Origin · Craft · Warmth</text>
      <text class="footer-about" @click="navigateTo('/pages/about/index')">关于我们 · 帮助与反馈</text>
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
import { getOrderList } from '@/api/order'
import { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS, getMemberLevelName } from '@/constants/member'
import { useMemberTheme } from '@/composables/useMemberTheme'
import LevelBadge from '@/components/member/LevelBadge.vue'
import CozyIcon from '@/components/CozyIcon.vue'
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'

const userStore = useUserStore()
const { themeStyle, isDark, level: themeLevel } = useMemberTheme()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo || {})
const memberInfo = computed(() => userStore.memberInfo || {})
const userLevel = computed(() => userStore.userLevel || 'basic')
const orderCount = ref({ coffee: 0, redeem: 0 })

const LEVEL_EN = { basic: 'BASIC', silver: 'SILVER', gold: 'GOLD', diamond: 'DIAMOND', black: 'BLACK' }

const displayName = computed(() => isLoggedIn.value ? (userInfo.value.nickname || 'Cozy 咖啡爱好者') : 'Cozy 咖啡爱好者')
const levelEn = computed(() => LEVEL_EN[userLevel.value] || userLevel.value.toUpperCase())
const memberText = computed(() => isDark.value ? '#E6C97A' : '#2B1E16')
const memberAccent = computed(() => themeStyle.value['--member-accent'])
const currentPoints = computed(() => Number(memberInfo.value.currentPoints) || 0)

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
const growthLabel = computed(() => nextLevel.value
  ? `距离${getMemberLevelName(nextLevel.value)}`
  : '已达最高等级')
const growthExp = computed(() => nextLevel.value
  ? `${Math.max(0, MEMBER_LEVEL_THRESHOLDS[nextLevel.value] - currentExp.value).toLocaleString()} EXP`
  : '尊享全部权益')

function formatPoints(value) { return Number(value || 0).toLocaleString() }

onShow(async () => {
  if (!isLoggedIn.value) {
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
    const res = await getOrderList()
    if (res.code === 200 && res.data) orderCount.value.coffee = res.data.length
  } catch (_) { /* 角标失败静默 */ }
  try {
    const res = await getMyRedemptions()
    if (res.code === 200 && res.data) orderCount.value.redeem = res.data.length
  } catch (_) { /* 角标失败静默 */ }
})

function handlePoints() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/points/history' })
}

function navigateTo(url) {
  if (isLoggedIn.value || ['/pages/store/list', '/pages/settings/index'].includes(url)) uni.navigateTo({ url })
  else uni.navigateTo({ url: '/pages/login/index' })
}

function goToOrders() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.switchTab({ url: '/pages/order/order' })
}

function goToRedemptions() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/points/redemptions' })
}

function contactService() {
  uni.showToast({ title: '客服热线 0571-8888 8888', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.profile-page { min-height: 100vh; padding: 20rpx 28rpx 140rpx; background: $cozy-surface; }

/* ===== 会员身份卡（紧凑 · 等级主题） ===== */
.member-card {
  position: relative;
  overflow: hidden;
  padding: 30rpx 32rpx 30rpx;
  border-radius: $cozy-radius-lg;
  box-shadow: 0 12rpx 40rpx rgba(43,30,22,.08);
  background: var(--member-surface, $cozy-surface);
  color: var(--member-text, $cozy-ink);
  transition: background .35s ease, color .35s ease;
}
.member-card > view { position: relative; z-index: 1; }
.layer-shine {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(160deg, rgba(255,255,255,.30) 0%, rgba(255,255,255,.05) 40%, transparent 70%);
}
.member-card.is-dark .layer-shine {
  background: linear-gradient(160deg, rgba(255,255,255,.14) 0%, rgba(255,255,255,.02) 45%, transparent 70%);
}

.card-top { display: flex; align-items: center; }
.brand-row { display: flex; align-items: center; gap: 12rpx; }
.brand-text {
  font-size: 19rpx;
  font-weight: 800;
  letter-spacing: .24em;
  opacity: .84;
  color: inherit;
}

.card-identity {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-top: 30rpx;
}
.level-emblem-wrap {
  position: relative;
  width: 88rpx;
  height: 88rpx;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
.identity-copy { flex: 1; min-width: 0; }
.user-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: inherit;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.level-en {
  display: block;
  margin-top: 6rpx;
  font-family: $font-display;
  font-size: 18rpx;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: .06em;
  opacity: .6;
  color: inherit;
}

.points-row {
  margin-top: 26rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid currentColor;
  opacity: .9;
}
.points-value {
  display: block;
  font-family: $font-display;
  font-size: 50rpx;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -.01em;
  color: inherit;
}
.points-label {
  display: block;
  margin-top: 10rpx;
  font-size: 18rpx;
  font-weight: 650;
  letter-spacing: .18em;
  opacity: .72;
  color: inherit;
}

.level-progress { margin-top: 28rpx; }
.growth-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 14rpx;
}
.growth-label { font-size: 21rpx; opacity: .74; color: inherit; }
.growth-exp { font-size: 22rpx; font-weight: 650; color: inherit; }
.progress-track {
  height: 5rpx;
  border-radius: 3rpx;
  background: rgba(60,40,30,.12);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: inherit;
  transition: width .4s ease;
}

/* ===== 卡片容器 ===== */
.entry-card {
  margin-top: 22rpx;
  border-radius: $cozy-radius-lg;
  background: $bg-white;
  overflow: hidden;
}
.entry-card.first { margin-top: 30rpx; }

/* ── 我的订单（两行入口） ── */
.order-row {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
  &:active { opacity: .8; }
}
.order-ic {
  flex: none;
  width: 64rpx;
  height: 64rpx;
  border-radius: $cozy-radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $cozy-surface;
}
.order-copy { flex: 1; min-width: 0; }
.order-name {
  display: block;
  font-size: $font-size-md;
  font-weight: 600;
  color: $cozy-ink;
}
.order-note {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: $cozy-muted;
}
.order-count {
  flex: none;
  min-width: 40rpx;
  height: 40rpx;
  padding: 0 12rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-surface;
  color: $cozy-muted;
  font-size: 20rpx;
  font-weight: 700;
}
.order-arrow { flex: none; font-size: 34rpx; color: $cozy-placeholder; line-height: 1; }

/* ── 会员功能（一行 5 格） ── */
.func-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  row-gap: 36rpx;
  padding: 44rpx 10rpx 40rpx;
}
.f-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;

  &:active { opacity: .55; }
}
.f-icon {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-muted;
}
.f-label { font-size: 21rpx; color: $cozy-ink; font-weight: 500; letter-spacing: .02em; }

/* ── 服务（安静文字列表） ── */
.svc-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
  &:active { opacity: .8; }
}
.svc-name { font-size: $font-size-md; color: $cozy-ink; }
.svc-note { flex: 1; text-align: right; font-size: 20rpx; color: $cozy-muted; }
.svc-arrow { flex: none; font-size: 34rpx; color: $cozy-placeholder; line-height: 1; }

/* ===== 底部品牌区域 ===== */
.profile-footer { padding: 70rpx 0 20rpx; text-align: center; }
.footer-kicker {
  display: block;
  font-size: 18rpx;
  font-weight: 700;
  letter-spacing: .26em;
  color: $cozy-placeholder;
}
.footer-brand {
  display: block;
  margin-top: 24rpx;
  font-family: $font-display;
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: .3em;
  color: $cozy-ink;
}
.footer-tag {
  display: block;
  margin-top: 16rpx;
  font-size: 20rpx;
  letter-spacing: .08em;
  color: $cozy-muted;
}
.footer-about {
  display: inline-block;
  margin-top: 30rpx;
  font-size: 20rpx;
  letter-spacing: .08em;
  color: $cozy-placeholder;

  &:active { opacity: .6; }
}
</style>
