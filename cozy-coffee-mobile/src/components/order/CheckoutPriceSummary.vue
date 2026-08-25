<template>
  <view class="price-summary">
    <!-- 金额明细（商品小计/优惠合计可折叠/配送费/实付）公用组件；优惠明细默认展开显示所选券 -->
    <PriceBreakdown
      :subtotal="preview?.subtotal || 0"
      :discount="preview?.discount || 0"
      :delivery-fee="preview?.deliveryFee || 0"
      :payable="payable"
      :payable-label="payableLabel"
      :coupon-items="couponItems"
      initially-expanded
    />

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
import PriceBreakdown from '@/components/order/PriceBreakdown.vue'
import { money } from '@/utils/format'
import { estimateEarned } from '@/domain/reward/rewardEstimate'

const props = defineProps({
  preview: { type: Object, default: null },
  pointsRate: { type: Number, default: 1 },
  // 优惠合计展开的券明细：每张券 { title, discount }；缺省由 preview.couponDetails 提供
  couponItems: { type: Array, default: null },
  payableLabel: { type: String, default: '待支付' }
})

const payable = computed(() => money(Number(props.preview?.payable ?? props.preview?.subtotal ?? 0)))
const resolvedCouponItems = computed(() => {
  if (Array.isArray(props.couponItems) && props.couponItems.length) return props.couponItems
  const details = props.preview?.couponDetails
  return Array.isArray(details) ? details.map(d => ({ title: d.title, discount: d.discount })) : []
})

// 可得积分/成长值：优先走后端预估（积分含倍率 pointsEarned / 成长值 1:1 expEarned）；
// 本地试算 fallback 用 rewardEstimate（基数 = 实付 − 配送费，口径与后端 rewardBase 一致）
const earnedPoints = computed(() => {
  if (props.preview?.pointsEarned != null) return Number(props.preview.pointsEarned)
  return estimateEarned(props.preview, props.pointsRate).points
})
const earnedExp = computed(() => {
  if (props.preview?.expEarned != null) return Number(props.preview.expEarned)
  return estimateEarned(props.preview, props.pointsRate).exp
})
</script>

<style lang="scss" scoped>
.price-summary { margin-top: 12rpx; padding: 24rpx 0 0; border-top: 1rpx solid $cozy-border; }

.earned-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 12rpx;
}
.earned-label { color: $cozy-muted; font-size: 25rpx; }
.earned-values { display: flex; align-items: center; gap: 16rpx; }
.earned-item { display: flex; align-items: center; gap: 6rpx; }
.earned-text { color: $cozy-primary; font-size: 25rpx; font-weight: 650; }
.earned-sep { color: $cozy-border; font-size: 25rpx; }
.preview-note { display: block; margin-top: 10rpx; color: $cozy-muted; font-size: 20rpx; text-align: right; }
</style>
