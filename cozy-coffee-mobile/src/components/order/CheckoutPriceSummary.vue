<template>
  <view class="price-summary">
    <view class="summary-row"><text>商品小计</text><text>¥{{ money(preview?.subtotal) }}</text></view>
    <view v-if="Number(preview?.discount || 0) > 0" class="summary-row discount"><text>优惠</text><text>-¥{{ money(preview.discount) }}</text></view>
    <view v-if="Number(preview?.deliveryFee || 0) > 0" class="summary-row"><text>配送费</text><text>¥{{ money(preview.deliveryFee) }}</text></view>

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
import { money } from '@/utils/format'
import { estimateEarned } from '@/domain/reward/rewardEstimate'

const props = defineProps({
  preview: { type: Object, default: null },
  pointsRate: { type: Number, default: 1 }
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

.summary-row { min-height: 60rpx; display: flex; align-items: center; justify-content: space-between; gap: 16rpx; color: $cozy-muted; font-size: 25rpx; }
.summary-row.discount { color: $cozy-primary; }

.earned-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
