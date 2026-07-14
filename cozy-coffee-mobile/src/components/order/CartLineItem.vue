<template>
  <view class="cart-line">
    <image class="line-image" :src="line.image" mode="aspectFill" />
    <view class="line-body" @click="$emit('edit', line)">
      <text class="line-name">{{ line.name }}</text>
      <text class="line-spec">{{ specText }}</text>
      <text class="edit-hint">修改规格</text>
    </view>
    <view class="line-right">
      <text class="line-price">¥{{ lineTotal }}</text>
      <view class="quantity-control">
        <view class="quantity-button" @click="$emit('decrease', line.lineKey)">−</view>
        <text class="quantity-value">{{ line.quantity }}</text>
        <view class="quantity-button quantity-button--primary" @click="$emit('increase', line.lineKey)">＋</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ line: { type: Object, required: true } })
defineEmits(['edit', 'decrease', 'increase'])

const labels = {
  STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', SMALL: '小杯',
  HOT: '热', COLD: '冰', WARM: '温',
  LESS: '少糖', HALF: '半糖', NONE: '无糖',
  WHOLE: '全脂奶', OAT: '燕麦奶', COCONUT: '椰奶', SOY: '豆奶',
  NORMAL: '标准浓度', STRONG: '加浓'
}

const specText = computed(() => {
  const values = [props.line.cupSize, props.line.temperature, props.line.sugarLevel, props.line.milkType, props.line.coffeeStrength]
    .filter(value => value && !['WHOLE', 'NORMAL'].includes(value))
    .map(value => labels[value] || value)
  return values.join(' · ') || '默认规格'
})
const lineTotal = computed(() => (Number(props.line.price || 0) * Number(props.line.quantity || 1)).toFixed(2))
</script>

<style lang="scss" scoped>
.cart-line { display: flex; gap: 18rpx; padding: 24rpx 0; border-bottom: 1rpx solid $cozy-border; }
.line-image { width: 112rpx; height: 112rpx; flex: none; border-radius: $cozy-radius-md; background: $cozy-surface; }
.line-body { min-width: 0; flex: 1; }
.line-name { display: block; overflow: hidden; color: $cozy-ink; font-size: 27rpx; font-weight: 650; white-space: nowrap; text-overflow: ellipsis; }
.line-spec { display: block; overflow: hidden; margin-top: 8rpx; color: $cozy-muted; font-size: 21rpx; white-space: nowrap; text-overflow: ellipsis; }
.edit-hint { display: block; margin-top: 10rpx; color: $cozy-primary; font-size: 21rpx; }
.line-right { flex: none; display: flex; flex-direction: column; align-items: flex-end; justify-content: space-between; }
.line-price { color: $cozy-ink; font-size: 26rpx; font-weight: 700; }
.quantity-control { display: flex; align-items: center; }
.quantity-button { width: 64rpx; height: 64rpx; display: flex; align-items: center; justify-content: center; border: 1rpx solid $cozy-border; border-radius: 50%; color: $cozy-ink; font-size: 32rpx; }
.quantity-button--primary { border-color: $cozy-primary; background: $cozy-primary; color: #fff; }
.quantity-value { min-width: 54rpx; text-align: center; color: $cozy-ink; font-size: 25rpx; font-weight: 600; }
</style>
