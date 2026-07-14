<template>
  <view v-if="visible" class="cart-layer">
    <view class="cart-mask" @click="$emit('close')" />
    <view class="cart-sheet">
      <view class="cart-header">
        <view>
          <text class="cart-title">已选商品</text>
          <text class="cart-subtitle">共 {{ totalCount }} 件</text>
        </view>
        <view class="clear-button" @click="$emit('clear')">清空</view>
      </view>
      <scroll-view scroll-y class="cart-scroll">
        <CartLineItem
          v-for="line in items"
          :key="line.lineKey"
          :line="line"
          @edit="$emit('edit', $event)"
          @decrease="$emit('decrease', $event)"
          @increase="$emit('increase', $event)"
        />
      </scroll-view>
      <view class="cart-summary">
        <text class="summary-label">合计</text>
        <text class="summary-price">¥{{ Number(total || 0).toFixed(2) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import CartLineItem from './CartLineItem.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  total: { type: [Number, String], default: 0 }
})
defineEmits(['close', 'clear', 'edit', 'decrease', 'increase'])
const totalCount = computed(() => props.items.reduce((sum, line) => sum + Number(line.quantity || 0), 0))
</script>

<style lang="scss" scoped>
.cart-layer { position: fixed; inset: 0; z-index: 105; }
.cart-mask { position: absolute; inset: 0; background: rgba(25, 18, 14, .46); }
.cart-sheet { position: absolute; left: 0; right: 0; bottom: 150rpx; max-height: 64vh; padding: 30rpx 32rpx 20rpx; border-radius: 24rpx 24rpx 0 0; background: #fff; animation: cart-in 200ms ease-out; }
/* #ifdef H5 */
.cart-sheet { bottom: calc(150rpx + 50px); }
/* #endif */
.cart-header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 18rpx; border-bottom: 1rpx solid $cozy-border; }
.cart-title { color: $cozy-ink; font-size: 32rpx; font-weight: 700; }
.cart-subtitle { margin-left: 12rpx; color: $cozy-muted; font-size: 22rpx; }
.clear-button { min-width: 88rpx; min-height: 72rpx; display: flex; align-items: center; justify-content: flex-end; color: $cozy-muted; font-size: 24rpx; }
.cart-scroll { max-height: 42vh; }
.cart-summary { display: flex; align-items: baseline; justify-content: flex-end; gap: 14rpx; padding-top: 20rpx; }
.summary-label { color: $cozy-muted; font-size: 24rpx; }
.summary-price { color: $cozy-primary; font-size: 34rpx; font-weight: 750; }
@keyframes cart-in { from { transform: translateY(40rpx); opacity: .4; } to { transform: translateY(0); opacity: 1; } }
@media (prefers-reduced-motion: reduce) { .cart-sheet { animation: none; } }
</style>
