<template>
  <view class="cart-line">
    <view class="line-check">
      <CozyIcon name="check" size="12" color="#ffffff" />
    </view>
    <view class="line-image">
      <image v-if="line.image && !imgError" :src="line.image" mode="aspectFill" @error="imgError = true" />
    </view>
    <view class="line-body" @click="$emit('edit', line)">
      <text class="line-name">{{ line.name }}</text>
      <text class="line-spec">{{ specText }}</text>
      <view class="line-foot">
        <text class="line-price">¥{{ lineTotal }}</text>
        <view class="quantity-control">
          <text class="quantity-button" @click="$emit('decrease', line.lineKey)">−</text>
          <text class="quantity-value">{{ line.quantity }}</text>
          <text class="quantity-button" @click="$emit('increase', line.lineKey)">＋</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import CozyIcon from '@/components/CozyIcon.vue'

const props = defineProps({ line: { type: Object, required: true } })
defineEmits(['edit', 'decrease', 'increase'])

const imgError = ref(false)

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
.cart-line { display: flex; align-items: flex-start; gap: 24rpx; padding: 28rpx 0; border-bottom: 1rpx solid $cozy-border; }
.cart-line:last-child { border-bottom: 0; }
.line-check { flex: none; width: 40rpx; height: 40rpx; margin-top: 36rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-ink; }
.line-image { flex: none; width: 120rpx; height: 120rpx; border-radius: $cozy-radius-md; background: linear-gradient(135deg, #E8DDD2, #D8C8B4); overflow: hidden; }
.line-image image { width: 100%; height: 100%; }
.line-body { flex: 1; min-width: 0; }
.line-name { display: block; overflow: hidden; font-family: $font-display; color: $cozy-ink; font-size: 30rpx; font-weight: 600; white-space: nowrap; text-overflow: ellipsis; }
.line-spec { display: block; overflow: hidden; margin-top: 8rpx; color: $cozy-muted; font-size: 22rpx; line-height: 1.5; white-space: nowrap; text-overflow: ellipsis; }
.line-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 16rpx; }
.line-price { color: $cozy-ink; font-size: 32rpx; font-weight: 700; }
.quantity-control { display: flex; align-items: center; gap: 24rpx; border: 1rpx solid $cozy-border; border-radius: 40rpx; padding: 8rpx 16rpx; }
.quantity-button { width: 40rpx; height: 40rpx; display: flex; align-items: center; justify-content: center; font-size: 28rpx; color: $cozy-ink; }
.quantity-value { min-width: 26rpx; text-align: center; font-size: 26rpx; color: $cozy-ink; }
</style>
