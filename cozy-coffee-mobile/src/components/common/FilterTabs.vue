<!--
  顶部筛选条（下划线式，对齐订单页）：选项列表 + 当前选中（v-model）
  sticky 时吸顶（优惠券页使用）
-->
<template>
  <view class="filter-tabs" :class="{ sticky }">
    <view
      v-for="f in options"
      :key="f.value"
      class="filter-tab"
      :class="{ active: modelValue === f.value }"
      @click="emit('update:modelValue', f.value)"
    >{{ f.label }}</view>
  </view>
</template>

<script setup>
defineProps({
  options: { type: Array, required: true }, // [{ value, label }]
  modelValue: { type: String, default: '' },
  sticky: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
</script>

<style lang="scss" scoped>
.filter-tabs {
  display: flex;
  height: 96rpx;
  background: $bg-white;
  border-bottom: 1rpx solid $cozy-border;

  &.sticky { position: sticky; top: 0; z-index: 10; }
}
.filter-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: $cozy-muted;
  position: relative;
  transition: color $cozy-duration $cozy-ease-out;

  &.active { color: $cozy-ink; font-weight: 600; }
  &.active::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 0;
    transform: translateX(-50%);
    width: 64rpx;
    height: 4rpx;
    border-radius: 2rpx;
    background: $cozy-primary;
  }
}
</style>
