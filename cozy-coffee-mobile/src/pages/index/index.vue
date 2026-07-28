<!--
  首页：精品咖啡品牌首屏。
  任务排序：点单效率 → 品牌感知 → 复购关系。
  不做电商式首页（优惠券/爆款/活动）；会员信息全部下沉到「我的」tab。
-->
<template>
  <view class="home-page">
    <view class="nav-bg" :style="{ opacity: navOpacity }" />

    <view class="custom-nav" :class="{ solid: navOpacity > 0.72 }" :style="{ paddingTop: statusBarHeight + 'px', paddingRight: navRight + 'px' }">
      <view class="nav-content">
        <view class="brand-lockup">
          <text class="brand-mark">COZY</text>
          <text class="brand-sub">ROASTED IN HANGZHOU</text>
        </view>
        <view class="nav-actions">
          <view class="pickup-chip" @click="goToPage('/pages/store/list')">{{ fixedStore.shortName }} · 自提</view>
          <view class="avatar-entry" @click="handleAvatarClick">
            <image :src="avatarUrl" class="avatar-img" mode="aspectFill" />
          </view>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="content-scroll" @scroll="onScroll">
      <HomeHero @order="switchToTab('/pages/menu/menu')" @explore="onHeroExplore" />

      <FeaturedCoffee @select="onFeaturedSelect" />

      <CoffeeOrigin @explore="goToPage('/pages/menu/menu')" />

      <view class="store-brief" @click="goToPage('/pages/store/list')">
        <view class="store-copy">
          <text class="store-status"><text class="status-dot" /> 今日营业</text>
          <text class="store-name">{{ fixedStore.name }}</text>
        </view>
        <view class="store-time">
          <text class="time-value">约 {{ fixedStore.pickupMinutes }}</text>
          <text class="time-unit">分钟可取</text>
        </view>
      </view>

      <view class="quick-order">
        <view class="quick-order-head">
          <view>
            <text class="quick-kicker">ORDER AT COZY</text>
            <text class="quick-title cozy-display">为此刻，选一杯合适的咖啡</text>
          </view>
          <text class="quick-note">门店现制 · 到店自提</text>
        </view>
        <view class="quick-chips">
          <view
            v-for="cat in orderCategories"
            :key="cat.id"
            class="quick-chip"
            @click="goToMenuCategory(cat.id)"
          >
            <text class="chip-glyph">{{ cat.glyph }}</text>
            <text class="chip-name">{{ cat.name }}</text>
          </view>
        </view>
      </view>

      <view class="service-links">
        <view class="service-link" @click="goToPage('/pages/mall/index')">
          <text class="service-code">PTS</text>
          <text class="service-name">积分商城</text>
        </view>
        <view class="service-link" @click="goToPage('/pages/coupon/list')">
          <text class="service-code">CPN</text>
          <text class="service-name">我的券包</text>
        </view>
        <view class="service-link" @click="goToPage('/pages/store/list')">
          <text class="service-code">SHOP</text>
          <text class="service-name">门店信息</text>
        </view>
      </view>

      <FeaturedCoffee @select="onFeaturedSelect" />

      <view class="honor-section" @click="switchToTab('/pages/menu/menu')">
        <image src="/static/images/promo.png" mode="aspectFill" class="honor-image" />
        <view class="honor-copy">
          <text class="honor-title cozy-display">让每一次出杯，都有来处</text>
          <text class="honor-description">从选豆、烘焙到制作，把风味认真交到你手中。</text>
          <text class="honor-link">去选择一杯 →</text>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>
    <DevLevelSwitcher />
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { FIXED_STORE } from '@/config/store'
import { getImageUrl } from '@/utils/image'
import HomeHero from '@/components/home/HomeHero.vue'
import CoffeeOrigin from '@/components/home/CoffeeOrigin.vue'
import FeaturedCoffee from '@/components/home/FeaturedCoffee.vue'
import DevLevelSwitcher from '@/components/dev/DevLevelSwitcher.vue'

const userStore = useUserStore()
const fixedStore = FIXED_STORE
const statusBarHeight = ref(20)
const navRight = ref(16) // px — capsule-safe right padding
const navOpacity = ref(0)

const orderCategories = [
  { id: 'signature', name: '季节特调', glyph: '调' },
  { id: 'latte', name: '拿铁系列', glyph: '奶' },
  { id: 'coffee', name: '经典咖啡', glyph: '咖' },
  { id: 'bakery', name: '烘焙甜点', glyph: '甜' }
]

const isLoggedIn = computed(() => userStore.isLoggedIn)

const avatarUrl = computed(() => {
  const avatar = userStore.userInfo?.avatar
  if (!isLoggedIn.value || !avatar || avatar === '/images/default-avatar.png') return '/static/images/default-avatar.png'
  return getImageUrl(avatar)
})

onMounted(() => {
  const info = uni.getSystemInfoSync()
  statusBarHeight.value = info.statusBarHeight || 20
  try {
    const capsule = uni.getMenuButtonBoundingClientRect()
    if (capsule) {
      navRight.value = info.windowWidth - capsule.left + 12
    }
  } catch (_) { /* fallback to default */ }
})

function onScroll(event) { navOpacity.value = Math.min(event.detail.scrollTop / 120, 1) }

function goToPage(url) { uni.navigateTo({ url }) }
function switchToTab(url) { uni.switchTab({ url }) }
function onHeroExplore() { switchToTab('/pages/menu/menu') }
function onFeaturedSelect(item) { uni.navigateTo({ url: `/pages/menu/detail?id=${item.id}` }) }
function handleAvatarClick() { goToPage(isLoggedIn.value ? '/pages/profile/edit' : '/pages/login/index') }
function goToMenuCategory(categoryId) {
  uni.setStorageSync('cozy_menu_category', categoryId)
  switchToTab('/pages/menu/menu')
}
</script>

<style lang="scss" scoped>
.home-page { position: relative; height: 100vh; overflow: hidden; background: $cozy-bg; }
.content-scroll { height: 100%; }
.nav-bg { position: fixed; inset: 0 0 auto; z-index: 98; height: calc(var(--status-bar-height) + 88rpx); background: rgba(255,255,255,.97); border-bottom: 1rpx solid $cozy-border; pointer-events: none; }
.custom-nav { position: fixed; inset: 0 0 auto; z-index: 100; color: #fff; transition: color $cozy-duration $cozy-ease-out; }
.custom-nav.solid { color: $cozy-ink; }
.nav-content { height: 44px; padding: 0 28rpx; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; }
.brand-lockup { min-width: 0; display: flex; flex-direction: column; }
.brand-mark { font-size: 31rpx; font-weight: 850; letter-spacing: .2em; line-height: 1; }
.brand-sub { margin-top: 7rpx; font-size: 14rpx; font-weight: 650; letter-spacing: .12em; opacity: .78; }
.nav-actions { display: flex; align-items: center; gap: 12rpx; }
.pickup-chip { min-height: 58rpx; padding: 0 18rpx; display: flex; align-items: center; border: 1rpx solid currentColor; border-radius: 999rpx; font-size: 20rpx; font-weight: 600; }
.avatar-entry { width: 64rpx; height: 64rpx; flex: none; border-radius: 50%; overflow: hidden; background: #fff; border: 1rpx solid rgba(255,255,255,.45); }
.solid .avatar-entry { border-color: $cozy-border; }
.avatar-img { width: 100%; height: 100%; }

.store-brief { min-height: 112rpx; padding: 20rpx 30rpx; display: flex; align-items: center; justify-content: space-between; gap: 24rpx; border-bottom: 1rpx solid $cozy-border; background: #fff; }
.store-copy { min-width: 0; }
.store-status { display: flex; align-items: center; gap: 9rpx; color: $cozy-accent; font-size: 20rpx; font-weight: 700; }
.status-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: $cozy-accent; }
.store-name { display: block; margin-top: 6rpx; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.store-time { flex: none; display: flex; align-items: baseline; gap: 7rpx; color: $cozy-ink; }
.time-value { font-size: 32rpx; font-weight: 750; }
.time-unit { color: $cozy-muted; font-size: 20rpx; }

/* 快速点单：品牌叙事 + 分类直达 */
.quick-order { margin: 28rpx 28rpx 0; padding: 30rpx 28rpx 26rpx; border-radius: $cozy-radius-lg; background: $cozy-surface; }
.quick-order-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24rpx; }
.quick-kicker { display: block; color: $cozy-primary; font-size: 18rpx; font-weight: 800; letter-spacing: .13em; }
.quick-title { display: block; max-width: 460rpx; margin-top: 10rpx; color: $cozy-ink; font-size: 34rpx; line-height: 1.32; }
.quick-note { flex: none; padding-bottom: 5rpx; color: $cozy-muted; font-size: 19rpx; }
.quick-chips { margin-top: 24rpx; display: flex; gap: 16rpx; }
.quick-chip { flex: 1; padding: 22rpx 0 18rpx; display: flex; flex-direction: column; align-items: center; gap: 12rpx; border-radius: $cozy-radius-md; background: #fff; border: 1rpx solid $cozy-border; }
.quick-chip:active { background: $cozy-surface; }
.chip-glyph { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-primary; color: #fff; font-size: 28rpx; font-weight: 750; }
.chip-name { color: $cozy-ink; font-size: 21rpx; font-weight: 650; }

.service-links { margin: 28rpx 28rpx 0; display: flex; border-top: 1rpx solid $cozy-border; border-bottom: 1rpx solid $cozy-border; }
.service-link { min-width: 0; flex: 1; padding: 24rpx 10rpx; text-align: center; }
.service-link + .service-link { border-left: 1rpx solid $cozy-border; }
.service-code { display: block; color: $cozy-primary; font-size: 17rpx; font-weight: 800; letter-spacing: .1em; }
.service-name { display: block; margin-top: 7rpx; color: $cozy-ink; font-size: 23rpx; font-weight: 600; }

.honor-section { margin: 54rpx 28rpx 0; overflow: hidden; border-radius: $cozy-radius-lg; background: $cozy-surface-alt; color: #fff; }
.honor-image { width: 100%; height: 210rpx; }
.honor-copy { padding: 30rpx; }
.honor-title { display: block; font-size: 35rpx; }
.honor-description { display: block; margin-top: 12rpx; color: $cozy-muted-on-dark; font-size: 21rpx; line-height: 1.55; }
.honor-link { display: block; margin-top: 20rpx; color: #fff; font-size: 22rpx; font-weight: 650; }
.bottom-spacer { height: 150rpx; }

@media (prefers-reduced-motion: reduce) {
  .custom-nav { transition: none; }
}
</style>
