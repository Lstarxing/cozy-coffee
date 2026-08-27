<!--
  MiniButton：白底细描边小按钮（订单页"取消/查看详情"、权益页"去商城/领取"等复用）
  size: 默认 / md；variant: 默认 / strong（墨描边加粗）/ muted（浅灰）
  内部已阻止冒泡，父级直接 @click 即可
-->
<template>
  <view class="mini-btn" :class="[size, variant]" @click.stop="handleClick">
    <slot />
  </view>
</template>

<script setup>
defineProps({
  size: { type: String, default: '' },
  variant: { type: String, default: '' }
})
const emit = defineEmits(['click'])
function handleClick(e) { emit('click', e) }
</script>

<style lang="scss" scoped>
.mini-btn {
  flex: none;
  height: 64rpx;
  padding: 0 28rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 12rpx;
  background: $bg-white;
  color: $cozy-ink;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;

  &:active { background: $cozy-surface; }
}
.mini-btn.md { height: 56rpx; padding: 0 20rpx; font-size: 22rpx; }
.mini-btn.strong { border-color: $cozy-ink; background: transparent; color: $cozy-ink; font-weight: 650; }
.mini-btn.strong:active { opacity: .85; }
.mini-btn.muted { background: $cozy-surface; color: $cozy-muted; }
</style>
