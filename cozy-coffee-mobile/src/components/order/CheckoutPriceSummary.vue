<template>
  <view class="price-summary">
    <!-- 优惠券（与价格汇总同一模块） -->
    <view class="coupon-row" @click="$emit('coupon-click')">
      <text class="coupon-label">优惠券</text>
      <view class="coupon-value-wrap">
        <text class="coupon-value" :class="{ accent: couponSelected }">{{ couponText }}</text>
        <text class="coupon-chevron">›</text>
      </view>
    </view>
    <view class="coupon-divider" />

    <view class="summary-row"><text>商品小计</text><text>¥{{ money(preview?.subtotal) }}</text></view>
    <view v-if="Number(preview?.discount || 0) > 0" class="summary-row discount"><text>优惠</text><text>-¥{{ money(preview.discount) }}</text></view>

    <!-- 可得积分 / 成长值 -->
    <view class="earned-row">
      <text class="earned-label">可得</text>
      <view class="earned-values">
        <view class="earned-item">
          <CozyIcon name="star" :size="20" color="#753A22" />
          <text class="earned-text">积分 {{ earnedPoints }}</text>
        </view>
        <text class="earned-sep">|</text>
        <view class="earned-item">
          <CozyIcon name="sparkle" :size="20" color="#753A22" />
          <text class="earned-text">成长值 {{ earnedExp }}</text>
        </view>
      </view>
    </view>

    <text v-if="preview?.source === 'local-fallback'" class="preview-note">当前为本地试算，提交时以后端金额为准</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import CozyIcon from '@/components/CozyIcon.vue'

const props = defineProps({
  preview: { type: Object, default: null },
  couponText: { type: String, default: '暂无可用' },
  couponSelected: { type: Boolean, default: false }
})
const emit = defineEmits(['coupon-click'])

const money = value => Number(value || 0).toFixed(2)
const earnedPoints = computed(() => Math.floor(Number(props.preview?.payable || 0)))
const earnedExp = computed(() => earnedPoints.value)
</script>

<style lang="scss" scoped>
.price-summary { margin-top: 24rpx; padding: 32rpx; border-radius: 28rpx; background: #fff; }

/* 优惠券行 */
.coupon-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 24rpx;
}
.coupon-label { flex: none; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.coupon-value-wrap { min-width: 0; flex: 1; display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.coupon-value { overflow: hidden; color: $cozy-muted; font-size: 24rpx; white-space: nowrap; text-overflow: ellipsis; }
.coupon-value.accent { color: $cozy-primary; }
.coupon-chevron { color: $cozy-placeholder; font-size: 42rpx; font-weight: 300; }
.coupon-divider { height: 1rpx; margin-bottom: 12rpx; background: $cozy-border; }

.summary-row { min-height: 60rpx; display: flex; align-items: center; justify-content: flex-end; gap: 16rpx; color: $cozy-muted; font-size: 25rpx; }
.summary-row.discount { color: $cozy-primary; }

.earned-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 12rpx;
}
.earned-label { color: $cozy-muted; font-size: 22rpx; }
.earned-values { display: flex; align-items: center; gap: 16rpx; }
.earned-item { display: flex; align-items: center; gap: 6rpx; }
.earned-text { color: $cozy-primary; font-size: 22rpx; font-weight: 650; }
.earned-sep { color: $cozy-border; font-size: 22rpx; }
.preview-note { display: block; margin-top: 10rpx; color: $cozy-muted; font-size: 20rpx; text-align: right; }
</style>
