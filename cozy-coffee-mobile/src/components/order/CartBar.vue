<template>
  <view v-if="count > 0" class="cart-bar-shell safe-area-bottom">
    <view class="cart-bar">
      <view class="cart-overview" @click="$emit('open')">
        <view class="cart-symbol">
          <text>袋</text>
          <text class="cart-badge">{{ count }}</text>
        </view>
        <view>
          <text class="cart-total">¥{{ Number(total || 0).toFixed(2) }}</text>
          <text class="cart-hint">点击查看已选商品</text>
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
.cart-bar { height: 108rpx; display: flex; align-items: center; padding: 8rpx 8rpx 8rpx 26rpx; border-radius: 999rpx; background: $cozy-surface-alt; box-sizing: border-box; pointer-events: auto; }
.cart-overview { min-width: 0; flex: 1; display: flex; align-items: center; gap: 20rpx; }
.cart-symbol { position: relative; width: 70rpx; height: 70rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #f2ede8; color: $cozy-primary; font-size: 24rpx; font-weight: 750; }
.cart-badge { position: absolute; top: -10rpx; right: -8rpx; min-width: 34rpx; height: 34rpx; padding: 0 6rpx; display: flex; align-items: center; justify-content: center; border: 3rpx solid $cozy-surface-alt; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 19rpx; box-sizing: border-box; }
.cart-total { display: block; color: #fff; font-size: 34rpx; font-weight: 750; line-height: 1.2; }
.cart-hint { display: block; margin-top: 4rpx; color: $cozy-muted-on-dark; font-size: 20rpx; }
.checkout-button { height: 92rpx; min-width: 190rpx; padding: 0 34rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-primary; color: #fff; font-size: 28rpx; font-weight: 700; box-sizing: border-box; }
</style>
