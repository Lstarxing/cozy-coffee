<template>
  <view v-if="visible" class="cart-layer">
    <view class="cart-mask" @click="emit('close')" />
    <view class="cart-sheet">
      <view class="cart-header">
        <view class="cart-header-left">
          <view class="cart-check-icon">
            <CozyIcon name="check" size="12" color="#ffffff" />
          </view>
          <text class="cart-title">已选购 {{ totalCount }} 件</text>
          <text class="cart-chip">自取</text>
        </view>
        <view class="cart-clear" @click="emit('clear')">
          <CozyIcon name="trash" size="16" color="#756A63" />
        </view>
      </view>

      <scroll-view scroll-y class="cart-scroll">
        <view v-if="!items.length" class="cart-empty">购物车空空如也，去选一杯喜欢的吧</view>
        <CartLineItem
          v-for="line in items"
          :key="line.lineKey"
          :line="line"
          @edit="emit('edit', $event)"
          @decrease="emit('decrease', $event)"
          @increase="emit('increase', $event)"
        />
      </scroll-view>

      <view class="cart-footer">
        <view class="footer-total">
          <text class="footer-total-label">合计</text>
          <text class="footer-total-price">¥{{ Number(total || 0).toFixed(2) }}</text>
        </view>
        <view class="checkout-button" :class="{ disabled: totalCount === 0 }" @click="onCheckout">结算</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import CartLineItem from './CartLineItem.vue'
import CozyIcon from '@/components/CozyIcon.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  total: { type: [Number, String], default: 0 }
})
const emit = defineEmits(['close', 'clear', 'edit', 'decrease', 'increase', 'checkout'])

const totalCount = computed(() => props.items.reduce((sum, line) => sum + Number(line.quantity || 0), 0))
function onCheckout() { if (totalCount.value) emit('checkout') }
</script>

<style lang="scss" scoped>
.cart-layer { position: fixed; inset: 0; z-index: 40; }
.cart-mask { position: absolute; inset: 0; background: rgba(28, 20, 14, .45); animation: cart-fade .25s ease; }
.cart-sheet {
  position: absolute; left: 0; right: 0; bottom: 0;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  animation: cart-up .35s cubic-bezier(.32, .72, .32, 1) both;
}
@keyframes cart-fade { from { opacity: 0 } to { opacity: 1 } }
@keyframes cart-up { from { transform: translateY(100%) } to { transform: translateY(0) } }

.cart-header { display: flex; align-items: center; justify-content: space-between; padding: 36rpx 40rpx 28rpx; border-bottom: 1rpx solid $cozy-border; }
.cart-header-left { display: flex; align-items: center; gap: 20rpx; }
.cart-check-icon { width: 40rpx; height: 40rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-ink; }
.cart-title { font-size: 30rpx; font-weight: 600; color: $cozy-ink; }
.cart-chip { padding: 6rpx 18rpx; font-size: 22rpx; border-radius: $cozy-radius; background: $cozy-surface; color: $cozy-muted; }
.cart-clear { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; }

.cart-scroll { max-height: 48vh; padding: 0 40rpx; }
.cart-empty { padding: 96rpx 0; text-align: center; color: $cozy-muted; font-size: 26rpx; }

.cart-footer { display: flex; align-items: center; gap: 24rpx; padding: 28rpx 32rpx; border-top: 1rpx solid $cozy-border; }
.footer-total { flex: 1; display: flex; align-items: baseline; gap: 8rpx; }
.footer-total-label { font-size: 36rpx; font-weight: 700; color: $cozy-ink; }
.footer-total-price { font-size: 36rpx; font-weight: 700; color: $cozy-ink; }
.checkout-button { height: 88rpx; padding: 0 64rpx; display: flex; align-items: center; justify-content: center; border-radius: 44rpx; background: $cozy-ink; color: #fff; font-size: 30rpx; font-weight: 600; }
.checkout-button.disabled { opacity: .4; }

@media (prefers-reduced-motion: reduce) { .cart-sheet, .cart-mask { animation: none; } }
</style>
