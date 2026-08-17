<template>
  <view class="store-summary">
    <view class="store-main" @click="goStore">
      <view class="store-copy">
        <view class="store-title-row">
          <text class="store-name">{{ name }}</text>
          <text class="pickup-tag">自提</text>
        </view>
        <text class="store-address">{{ address }}</text>
      </view>
      <text class="store-arrow">›</text>
    </view>
    <view class="pickup-row">
      <text class="pickup-label">取餐时间</text>
      <text class="pickup-value">{{ pickupLabel }}</text>
    </view>
  </view>
</template>

<script setup>
import { FIXED_STORE } from '@/config/store'

defineProps({
  name: { type: String, default: FIXED_STORE.name },
  address: { type: String, default: FIXED_STORE.address },
  pickupLabel: { type: String, default: FIXED_STORE.pickupLabel }
})

function goStore() {
  uni.navigateTo({ url: '/pages/store/list' })
}
</script>

<style lang="scss" scoped>
.store-summary {
  padding: 32rpx;
  border-radius: 28rpx;
  background: #fff;
}
.store-main { display: flex; align-items: flex-start; gap: 20rpx; }
.store-copy { min-width: 0; flex: 1; }
.store-title-row { display: flex; align-items: center; gap: 16rpx; }
.store-name {
  overflow: hidden;
  font-family: $font-display;
  font-size: 34rpx;
  font-weight: 600;
  color: $cozy-ink;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.pickup-tag {
  flex: none;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid $cozy-ink;
  color: $cozy-ink;
  font-size: 20rpx;
  font-weight: 650;
}
.store-address {
  display: block;
  margin-top: 12rpx;
  color: $cozy-ink;
  font-size: 24rpx;
  line-height: 1.5;
}
.store-arrow {
  flex: none;
  color: $cozy-muted;
  font-size: 44rpx;
  font-weight: 300;
  line-height: 1;
  margin-top: 4rpx;
}
.pickup-row {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $cozy-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.pickup-label { color: $cozy-ink; font-size: 24rpx; }
.pickup-value { color: $cozy-ink; font-size: 24rpx; font-weight: 650; }
</style>
