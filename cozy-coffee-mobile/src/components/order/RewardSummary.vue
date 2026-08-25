<!--
  可得积分/成长值（确认订单页「可得」/ 订单详情页「本次获得」公用）：
  左标签 + 右 积分 | 成长值（可按 + 前缀 / 条件隐藏）
-->
<template>
  <view class="reward-row">
    <text class="reward-label">{{ label }}</text>
    <view class="reward-values">
      <view v-if="points != null" class="reward-item">
        <CozyIcon name="star" :size="20" color="#753A22" />
        <text class="reward-text">积分 {{ pointsPrefix }}{{ points }}</text>
      </view>
      <text v-if="points != null && exp != null" class="reward-sep">|</text>
      <view v-if="exp != null" class="reward-item">
        <CozyIcon name="sparkle" :size="20" color="#753A22" />
        <text class="reward-text">成长值 {{ expPrefix }}{{ exp }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import CozyIcon from '@/components/CozyIcon.vue'

defineProps({
  label: { type: String, default: '可得' },
  points: { type: [Number, String], default: null },
  exp: { type: [Number, String], default: null },
  pointsPrefix: { type: String, default: '' },
  expPrefix: { type: String, default: '' }
})
</script>

<style lang="scss" scoped>
.reward-row { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-top: 12rpx; }
.reward-label { flex: none; color: $cozy-muted; font-size: 22rpx; }
.reward-values { display: flex; align-items: center; gap: 16rpx; }
.reward-item { display: flex; align-items: center; gap: 6rpx; }
.reward-text { color: $cozy-primary; font-size: 22rpx; font-weight: 650; }
.reward-sep { color: $cozy-border; font-size: 22rpx; }
</style>
