<!--
  数量步进器（v-model）：min/max 限制，父层可传 canIncrease（如积分足够）控制 +
-->
<template>
  <view class="stepper">
    <view class="stepper-btn" :class="{ disabled: modelValue <= min }" @click="decrease">−</view>
    <text class="stepper-value">{{ modelValue }}</text>
    <view class="stepper-btn" :class="{ disabled: !canIncrease || modelValue >= max }" @click="increase">＋</view>
  </view>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Number, default: 1 },
  min: { type: Number, default: 1 },
  max: { type: Number, default: 99 },
  canIncrease: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue'])

function decrease() {
  if (props.modelValue > props.min) emit('update:modelValue', props.modelValue - 1)
}
function increase() {
  if (props.canIncrease && props.modelValue < props.max) emit('update:modelValue', props.modelValue + 1)
}
</script>

<style lang="scss" scoped>
.stepper {
  display: flex;
  align-items: center;
  overflow: hidden;
  border: 1rpx solid $cozy-border;
  border-radius: 999rpx;
}
.stepper-btn {
  width: 64rpx;
  padding: 6rpx 0;
  color: $cozy-ink;
  font-size: 32rpx;
  line-height: 1.2;
  text-align: center;

  &.disabled { color: $cozy-placeholder; }
}
.stepper-value {
  min-width: 48rpx;
  color: $cozy-ink;
  font-size: 26rpx;
  font-weight: 600;
  text-align: center;
}
</style>
