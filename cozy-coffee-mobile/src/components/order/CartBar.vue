<template>
  <view class="cart-bar-shell">
    <view class="cart-bar">
      <view class="cart-total-block" @click="$emit('open')">
        <view class="cart-bag">
          <CozyIcon name="shopping-bag" size="18" color="#ffffff" />
          <text v-if="count > 0" class="cart-bag-count">{{ count }}</text>
        </view>
        <text class="cart-total-price">¥{{ Number(total || 0).toFixed(2) }}</text>
      </view>
      <view class="cart-checkout" :class="{ disabled: count === 0 }" @click="$emit('checkout')">去结算</view>
    </view>
  </view>
</template>

<script setup>
import CozyIcon from '@/components/CozyIcon.vue'

defineProps({ count: { type: Number, default: 0 }, total: { type: [Number, String], default: 0 } })
defineEmits(['open', 'checkout'])
</script>

<style lang="scss" scoped>
.cart-bar-shell { position: fixed; left: 0; right: 0; bottom: 0; z-index: 15; }
/* #ifdef H5 */
.cart-bar-shell { bottom: 50px; }
/* #endif */
.cart-bar { height: 112rpx; display: flex; align-items: center; padding: 0 32rpx; background: rgba(61, 42, 32, .96); color: #fff; }
.cart-total-block { flex: 1; min-width: 0; display: flex; align-items: center; gap: 20rpx; padding: 0 8rpx; }
.cart-bag { position: relative; width: 72rpx; height: 72rpx; flex: none; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-primary; }
.cart-bag-count { position: absolute; top: -8rpx; right: -12rpx; min-width: 36rpx; height: 36rpx; padding: 0 10rpx; display: flex; align-items: center; justify-content: center; border-radius: 18rpx; background: #fff; color: $cozy-primary; font-size: 22rpx; font-weight: 700; box-sizing: border-box; }
.cart-total-price { font-size: 34rpx; font-weight: 700; }
.cart-checkout { height: 80rpx; padding: 0 44rpx; display: flex; align-items: center; justify-content: center; border-radius: 40rpx; background: $cozy-primary; color: #fff; font-size: 28rpx; font-weight: 600; }
.cart-checkout.disabled { opacity: .45; }
</style>
