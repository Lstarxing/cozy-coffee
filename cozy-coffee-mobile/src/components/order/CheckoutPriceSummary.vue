<template>
  <view class="price-summary">
    <view class="summary-row"><text>商品小计</text><text>¥{{ money(preview?.subtotal) }}</text></view>
    <view v-if="Number(preview?.discount || 0) > 0" class="summary-row discount"><text>优惠</text><text>-¥{{ money(preview.discount) }}</text></view>
    <view class="summary-row"><text>到店自提</text><text>免费</text></view>
    <view class="summary-divider" />
    <view class="summary-row total"><text>应付</text><text>¥{{ money(preview?.payable) }}</text></view>
    <text v-if="preview?.source === 'local-fallback'" class="preview-note">当前为本地试算，提交时以后端金额为准</text>
  </view>
</template>

<script setup>
defineProps({ preview: { type: Object, default: null } })
const money = value => Number(value || 0).toFixed(2)
</script>

<style lang="scss" scoped>
.price-summary { padding: 28rpx; border-radius: $cozy-radius-lg; background: #fff; }
.summary-row { min-height: 58rpx; display: flex; align-items: center; justify-content: space-between; color: $cozy-muted; font-size: 25rpx; }
.summary-row.discount { color: $cozy-primary; }
.summary-divider { height: 1rpx; margin: 10rpx 0; background: $cozy-border; }
.summary-row.total { color: $cozy-ink; font-size: 30rpx; font-weight: 750; }
.summary-row.total text:last-child { color: $cozy-primary; font-size: 38rpx; }
.preview-note { display: block; margin-top: 10rpx; color: $cozy-muted; font-size: 20rpx; text-align: right; }
</style>
