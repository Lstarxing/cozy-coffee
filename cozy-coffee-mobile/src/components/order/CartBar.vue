<template>
  <view v-if="count > 0" class="cart-bar-shell safe-area-bottom">
    <view class="cart-bar">
      <view class="cart-overview" @click="$emit('open')">
        <view class="cart-symbol">
          <text>C</text>
          <text class="cart-badge">{{ count }}</text>
        </view>
        <view>
          <text class="cart-label">COZY BAG</text>
          <text class="cart-total">¥{{ Number(total || 0).toFixed(2) }}</text>
        </view>
      </view>
      <view class="checkout-button" @click="$emit('checkout')">去结算</view>
    </view>
  </view>
</template>

<script setup>
defineProps({ count: { type: Number, default: 0 }, total: { type: [Number, String], default: 0 } })
defineEmits(['open', 'checkout'])
</script>

<style lang="scss" scoped>
.cart-bar-shell { position: fixed; left: 0; right: 0; bottom: 0; z-index: 110; padding: 12rpx 24rpx max(18rpx, env(safe-area-inset-bottom)); pointer-events: none; }
/* #ifdef H5 */
.cart-bar-shell { bottom: 50px; }
/* #endif */
.cart-bar { height: 112rpx; display: flex; align-items: center; padding: 10rpx 10rpx 10rpx 24rpx; border-radius: $cozy-radius-lg; background: $cozy-surface-alt; box-shadow: $cozy-shadow-raised; box-sizing: border-box; pointer-events: auto; }
.cart-overview { min-width: 0; flex: 1; display: flex; align-items: center; gap: 20rpx; }
.cart-symbol { position: relative; width: 70rpx; height: 70rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-cta-alt-bg; color: $cozy-primary; font-size: 27rpx; font-weight: 850; }
.cart-badge { position: absolute; top: -10rpx; right: -8rpx; min-width: 34rpx; height: 34rpx; padding: 0 6rpx; display: flex; align-items: center; justify-content: center; border: 3rpx solid $cozy-surface-alt; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 19rpx; box-sizing: border-box; }
.cart-label { display: block; color: $cozy-muted-on-dark; font-size: 16rpx; font-weight: 750; letter-spacing: .12em; }
.cart-total { display: block; margin-top: 4rpx; color: #fff; font-size: 34rpx; font-weight: 750; line-height: 1.1; }
.checkout-button { height: 92rpx; min-width: 184rpx; padding: 0 32rpx; display: flex; align-items: center; justify-content: center; border-radius: $cozy-radius-md; background: $cozy-primary; color: #fff; font-size: 27rpx; font-weight: 700; box-sizing: border-box; }
</style>
