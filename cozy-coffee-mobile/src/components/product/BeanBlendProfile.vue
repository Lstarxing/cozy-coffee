<template>
  <view v-if="profile" class="bean-profile">
    <view class="profile-head">
      <text class="profile-title">{{ profile.name || '咖啡豆档案' }}</text>
      <text class="profile-tag">{{ isBlend ? '拼配' : '单品' }}</text>
    </view>
    <view class="profile-rows">
      <view v-for="row in rows" :key="row.label" class="profile-row">
        <text class="row-label">{{ row.label }}</text>
        <text class="row-value">{{ row.value }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

// V2 豆档案卡（P2 收尾）：随菜单/详情 API 的 beanProfile/blendProfile 渲染「烘焙 · 风味 · 醇厚 · 酸度」
const props = defineProps({
  product: { type: Object, default: () => ({}) }
})

const profile = computed(() => props.product?.beanProfile || props.product?.blendProfile || null)
const isBlend = computed(() => Boolean(props.product?.blendProfile))

const rows = computed(() => {
  const p = profile.value
  if (!p) return []
  return [
    { label: '烘焙', value: p.roast },
    { label: '风味', value: p.flavorNotes },
    { label: '醇厚', value: p.body },
    { label: '酸度', value: p.acidity }
  ].filter(r => r.value)
})
</script>

<style lang="scss" scoped>
.bean-profile {
  padding: 28rpx 0 28rpx;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 14rpx;
}
.profile-title {
  font-family: $font-display;
  color: $cozy-ink;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.3;
}
.profile-tag {
  flex: none;
  color: $cozy-primary;
  font-size: 20rpx;
  border: 1rpx solid $cozy-primary;
  border-radius: 999rpx;
  padding: 2rpx 12rpx;
}
.profile-row {
  display: flex;
  padding: 8rpx 0;
}
.row-label {
  flex: none;
  width: 96rpx;
  color: $cozy-muted;
  font-size: $font-size-sm;
}
.row-value {
  flex: 1;
  color: $cozy-ink;
  font-size: $font-size-sm;
  line-height: 1.5;
}
</style>
