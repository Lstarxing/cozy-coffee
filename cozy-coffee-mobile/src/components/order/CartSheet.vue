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
          <text class="cart-chip">{{ fulfillment === 'delivery' ? '外送' : '自提' }}</text>
        </view>
        <view class="cart-clear" @click="emit('clear')">
          <CozyIcon name="trash" size="16" color="#756A63" />
        </view>
      </view>

      <scroll-view scroll-y class="cart-scroll">
        <view v-if="!items.length" class="cart-empty">
          <text>购物车空空如也，去选一杯喜欢的吧</text>
        </view>
        <CartLineItem
          v-for="line in items"
          :key="line.lineKey"
          :line="line"
          @edit="emit('edit', $event)"
          @decrease="emit('decrease', $event)"
          @increase="emit('increase', $event)"
        />
      </scroll-view>
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
  fulfillment: { type: String, default: 'pickup' }
})
const emit = defineEmits(['close', 'clear', 'edit', 'decrease', 'increase'])

const totalCount = computed(() => props.items.reduce((sum, line) => sum + Number(line.quantity || 0), 0))
</script>

<style lang="scss" scoped>
.cart-layer { position: fixed; inset: 0; z-index: 40; }
.cart-mask { position: absolute; inset: 0; background: rgba(28, 20, 14, .45); animation: cart-fade .25s ease; }
.cart-sheet {
  position: absolute; left: 0; right: 0; bottom: 112rpx;
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

.cart-scroll { max-height: 48vh; padding: 0 40rpx; box-sizing: border-box; }
.cart-empty { width: 100%; display: flex; align-items: center; justify-content: center; padding: 72rpx 40rpx; color: $cozy-muted; font-size: 26rpx; line-height: 1.6; text-align: center; box-sizing: border-box; }

@media (prefers-reduced-motion: reduce) { .cart-sheet, .cart-mask { animation: none; } }
</style>
