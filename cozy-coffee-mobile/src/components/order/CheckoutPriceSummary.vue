<template>
  <view class="price-summary">
    <!-- 金额明细（商品小计/优惠合计可折叠/配送费/实付）公用组件 -->
    <PriceBreakdown
      :subtotal="preview?.subtotal || 0"
      :discount="preview?.discount || 0"
      :delivery-fee="preview?.deliveryFee || 0"
      :payable="payable"
      :payable-label="payableLabel"
      :coupon-items="couponItems"
    />

    <!-- 可得积分 / 成长值（公用组件，字号与订单详情页一致） -->
    <RewardSummary label="可得" :points="earnedPoints" :exp="earnedExp" points-prefix="+" exp-prefix="+" />

    <text v-if="preview?.source === 'local-fallback'" class="preview-note">当前为本地试算，提交时以后端金额为准</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import PriceBreakdown from '@/components/order/PriceBreakdown.vue'
import RewardSummary from '@/components/order/RewardSummary.vue'
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

.preview-note { display: block; margin-top: 10rpx; color: $cozy-muted; font-size: 20rpx; text-align: right; }
</style>
