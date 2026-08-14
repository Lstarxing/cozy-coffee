<template>
  <view class="store-page">
    <view class="store-hero">
      <text class="eyebrow">FIXED PICKUP STORE</text>
      <text class="store-name cozy-display">{{ store.name }}</text>
      <text class="store-description">当前阶段仅开放这一家固定门店，所有移动端订单均在此自提。</text>
    </view>

    <view class="store-panel">
      <view class="status-row">
        <view class="open-status"><view class="status-dot" />营业中</view>
        <text class="hours">{{ store.businessHours }}</text>
      </view>

      <view class="info-row">
        <text class="info-label">门店地址</text>
        <text class="info-value">{{ store.address }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">取餐方式</text>
        <text class="info-value">{{ store.fulfillmentLabel }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">预计时间</text>
        <text class="info-value">下单后约 {{ store.pickupMinutes }} 分钟</text>
      </view>
    </view>

    <view class="scope-note">
      <text class="note-title">当前服务范围</text>
      <text class="note-copy">暂不提供多门店切换、外卖配送和门店定位；无需选择门店即可直接点单。</text>
    </view>

    <view class="order-button" @click="goToMenu">前往点单</view>
  </view>
</template>

<script setup>
import { FIXED_STORE } from '@/config/store'

const store = FIXED_STORE

function goToMenu() {
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>

<style lang="scss" scoped>
.store-page { min-height: 100vh; padding: 30rpx 28rpx 170rpx; background: $cozy-surface; }
.store-hero { padding: 52rpx 34rpx; border-radius: 28rpx; background: $bg-white; color: $cozy-ink; }
.eyebrow { display: block; color: $cozy-accent; font-size: 18rpx; font-weight: 800; letter-spacing: .14em; }
.store-name { display: block; margin-top: 18rpx; font-size: 46rpx; }
.store-description { display: block; max-width: 580rpx; margin-top: 16rpx; color: $cozy-muted; font-size: 23rpx; line-height: 1.65; }
.store-panel { margin-top: 24rpx; padding: 8rpx 30rpx; border-radius: 28rpx; background: $bg-white; }
.status-row, .info-row { min-height: 96rpx; display: flex; align-items: center; justify-content: space-between; gap: 30rpx; border-bottom: 1rpx solid $cozy-border; }
.info-row:last-child { border-bottom: 0; }
.open-status { display: flex; align-items: center; gap: 10rpx; color: $cozy-accent; font-size: 24rpx; font-weight: 700; }
.status-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: $cozy-accent; }
.hours { color: $cozy-ink; font-size: 24rpx; font-weight: 650; }
.info-label { flex: none; color: $cozy-muted; font-size: 22rpx; }
.info-value { color: $cozy-ink; font-size: 23rpx; line-height: 1.5; text-align: right; }
.scope-note { margin-top: 24rpx; padding: 28rpx 30rpx; border: 1rpx solid $cozy-border; border-radius: 28rpx; background: $bg-white; }
.note-title { display: block; color: $cozy-ink; font-size: 25rpx; font-weight: 700; }
.note-copy { display: block; margin-top: 11rpx; color: $cozy-muted; font-size: 21rpx; line-height: 1.65; }
.order-button { position: fixed; right: 28rpx; bottom: max(32rpx, env(safe-area-inset-bottom)); left: 28rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-ink; color: #fff; font-size: 27rpx; font-weight: 750; }
</style>
