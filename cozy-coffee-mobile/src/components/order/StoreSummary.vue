<template>
  <view class="store-summary">
    <view class="store-icon">店</view>
    <view class="store-content">
      <view class="store-title-row">
        <text class="store-name">{{ name }}</text>
        <text class="open-tag">营业中</text>
      </view>
      <text class="store-address">{{ address }}</text>
      <text class="pickup-time">取餐时间：{{ pickupLabel }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  name: { type: String, default: 'CozyCoffee 中心店' },
  address: { type: String, default: '固定自提门店 · 具体地址以门店公告为准' },
  pickupLabel: { type: String, default: '尽快取餐（约 15 分钟）' }
})
</script>

<style lang="scss" scoped>
.store-summary { display: flex; gap: 20rpx; padding: 28rpx; border-radius: $cozy-radius-lg; background: $cozy-surface-alt; color: #fff; }
.store-icon { width: 72rpx; height: 72rpx; flex: none; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-cta-alt-bg; color: $cozy-primary; font-size: 25rpx; font-weight: 750; }
.store-content { min-width: 0; flex: 1; }
.store-title-row { display: flex; align-items: center; gap: 12rpx; }
.store-name { overflow: hidden; font-size: 30rpx; font-weight: 700; white-space: nowrap; text-overflow: ellipsis; }
.open-tag { padding: 4rpx 10rpx; border-radius: 999rpx; background: $cozy-accent; color: #fff; font-size: 19rpx; }
.store-address, .pickup-time { display: block; margin-top: 10rpx; color: $cozy-muted-on-dark; font-size: 22rpx; line-height: 1.4; }
.pickup-time { color: #fff; }
</style>
