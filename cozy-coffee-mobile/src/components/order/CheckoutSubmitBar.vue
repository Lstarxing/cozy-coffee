<template>
  <view class="submit-shell safe-area-bottom">
    <view class="submit-price">
      <text class="submit-label">合计</text>
      <text class="submit-amount">¥{{ Number(amount || 0).toFixed(2) }}</text>
    </view>
    <view class="submit-button" :class="{ disabled }" @click="onSubmit">{{ buttonText }}</view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  amount: { type: [Number, String], default: 0 },
  status: { type: String, default: 'ready' },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['submit'])

const buttonText = computed(() => ({
  previewing: '正在核价…', submitting: '正在创建订单…', paying: '模拟支付中…', offline: '网络不可用'
}[props.status] || '模拟支付'))

function onSubmit() { if (!props.disabled) emit('submit') }
</script>

<style lang="scss" scoped>
.submit-shell { position: fixed; left: 0; right: 0; bottom: 0; z-index: 80; min-height: 112rpx; padding: 14rpx 28rpx max(14rpx, env(safe-area-inset-bottom)); display: flex; align-items: center; gap: 24rpx; border-top: 1rpx solid $cozy-border; background: #fff; box-sizing: border-box; }
.submit-price { min-width: 0; flex: 1; }
.submit-label { margin-right: 10rpx; color: $cozy-muted; font-size: 23rpx; }
.submit-amount { color: $cozy-primary; font-size: 38rpx; font-weight: 750; }
.submit-button { min-width: 270rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 28rpx; font-weight: 700; }
.submit-button.disabled { background: #bdb4ae; color: #f7f5f3; }
</style>
