<!--
  首页：精品咖啡品牌首屏。
  上半部分：Hero + Season + Store → 品牌感知 + 消费决策
  下半部分：Flavors + Origin + Journal + Club → 品牌编辑内容
-->
<template>
  <view class="home-page">
    <scroll-view scroll-y class="content-scroll">
      <HomeHero @order="switchToTab('/pages/menu/menu')" @explore="onHeroExplore" />

      <FeaturedCoffee @select="onFeaturedSelect" />

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

      <view class="flavor-discovery">
        <view class="flavor-head">
          <text class="flavor-kicker">EXPLORE FLAVORS</text>
          <text class="flavor-title cozy-display">探索你的风味</text>
          <text class="flavor-note">门店现制 · 到店自提</text>
        </view>
        <view class="flavor-tags">
          <view
            v-for="tag in flavorTags"
            :key="tag.category"
            class="flavor-tag"
            @click="goToMenuCategory(tag.category)"
          >
            <text class="tag-en">{{ tag.en }}</text>
            <text class="tag-cn">{{ tag.cn }}</text>
          </view>
        </view>
      </view>

      <CoffeeOrigin />

      <CozyJournal />

      <view class="cozy-club" @click="goToPage('/pages/benefits/index')">
        <text class="club-kicker">COZY CLUB</text>
        <text class="club-title cozy-display">你的咖啡旅程</text>
        <text class="club-desc">会员等级 · 专属权益 · 积分礼遇</text>
        <text class="club-link">查看会员权益 →</text>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup>
import { FIXED_STORE } from '@/config/store'
import HomeHero from '@/components/home/HomeHero.vue'
import CoffeeOrigin from '@/components/home/CoffeeOrigin.vue'
import FeaturedCoffee from '@/components/home/FeaturedCoffee.vue'
import CozyJournal from '@/components/home/CozyJournal.vue'

const fixedStore = FIXED_STORE

const flavorTags = [
  { en: 'Floral', cn: '花香', category: 'signature' },
  { en: 'Nutty', cn: '坚果', category: 'coffee' },
  { en: 'Fruity', cn: '果香', category: 'latte' },
  { en: 'Sweet', cn: '甜感', category: 'bakery' }
]

function goToPage(url) { uni.navigateTo({ url }) }
function switchToTab(url) { uni.switchTab({ url }) }
function onHeroExplore() { switchToTab('/pages/menu/menu') }
function onFeaturedSelect(item) { uni.navigateTo({ url: `/pages/menu/detail?id=${item.id}` }) }
function goToMenuCategory(categoryId) {
  uni.setStorageSync('cozy_menu_category', categoryId)
  switchToTab('/pages/menu/menu')
}
</script>

<style lang="scss" scoped>
.home-page { height: 100vh; overflow: hidden; background: $cozy-bg; }
.content-scroll { height: 100%; }

.store-brief {
  min-height: 112rpx; padding: 20rpx 30rpx;
  display: flex; align-items: center; justify-content: space-between; gap: 24rpx;
  border-bottom: 1rpx solid $cozy-border; background: #fff;
}
.store-copy { min-width: 0; }
.store-status { display: flex; align-items: center; gap: 9rpx; color: $cozy-accent; font-size: 20rpx; font-weight: 700; }
.status-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: $cozy-accent; }
.store-name { display: block; margin-top: 6rpx; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.store-time { flex: none; display: flex; align-items: baseline; gap: 7rpx; color: $cozy-ink; }
.time-value { font-size: 32rpx; font-weight: 750; }
.time-unit { color: $cozy-muted; font-size: 20rpx; }

/* 风味探索 */
.flavor-discovery { margin: 44rpx 28rpx 0; padding: 34rpx 28rpx 30rpx; border-radius: $cozy-radius-lg; background: $cozy-surface; }
.flavor-head { margin-bottom: 28rpx; }
.flavor-kicker { display: block; color: $cozy-primary; font-size: 17rpx; font-weight: 700; letter-spacing: 0.14em; }
.flavor-title { display: block; margin-top: 10rpx; color: $cozy-ink; font-size: 34rpx; }
.flavor-note { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 20rpx; }
.flavor-tags { display: flex; gap: 16rpx; }
.flavor-tag {
  flex: 1; padding: 26rpx 0 22rpx;
  display: flex; flex-direction: column; align-items: center; gap: 10rpx;
  border-radius: $cozy-radius-md; background: #fff; border: 1rpx solid $cozy-border;
}
.flavor-tag:active { background: $cozy-surface; }
.tag-en { color: $cozy-ink; font-size: 22rpx; font-weight: 650; letter-spacing: 0.04em; }
.tag-cn { color: $cozy-muted; font-size: 19rpx; }

/* Cozy Club */
.cozy-club {
  margin: 28rpx 28rpx 0; padding: 36rpx 30rpx;
  border-radius: $cozy-radius-lg; background: $cozy-surface;
}
.club-kicker { display: block; color: $cozy-primary; font-size: 17rpx; font-weight: 700; letter-spacing: 0.14em; }
.club-title { display: block; margin-top: 14rpx; color: $cozy-ink; font-size: 34rpx; }
.club-desc { display: block; margin-top: 10rpx; color: $cozy-muted; font-size: 22rpx; }
.club-link { display: block; margin-top: 24rpx; color: $cozy-primary; font-size: 22rpx; font-weight: 650; }

.bottom-spacer { height: 150rpx; }
</style>
