<template>
  <view class="level-badge" :style="{ color, width: size + 'rpx', height: size + 'rpx' }">
    <view class="emblem-3d">
      <view v-if="level === 'basic'" class="svg-wrap"><view class="bean" /></view>
      <view v-else-if="level === 'silver'" class="svg-wrap"><view class="medal" /></view>
      <view v-else-if="level === 'gold'" class="svg-wrap"><view class="star" /></view>
      <view v-else-if="level === 'diamond'" class="svg-wrap"><view class="diamond" /></view>
      <view v-else-if="level === 'black'" class="svg-wrap"><view class="crown" /></view>
      <view class="shine-overlay" />
    </view>
  </view>
</template>

<script setup>
defineProps({
  level: { type: String, default: 'basic' },
  color: { type: String, default: 'currentColor' },
  size: { type: Number, default: 48 }
})
</script>

<style lang="scss" scoped>
.level-badge { display: inline-flex; align-items: center; justify-content: center; flex: none; }
.emblem-3d {
  position: relative; width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  filter: drop-shadow(0 6rpx 10rpx rgba(0,0,0,0.22));
}
.svg-wrap { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
// 光泽高光：左上白色渐变，模拟 3D 金属反光
.shine-overlay {
  position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(135deg, rgba(255,255,255,0.42) 0%, rgba(255,255,255,0.08) 38%, transparent 60%);
  border-radius: 50%;
  mix-blend-mode: screen;
}
// 咖啡豆
.bean { width: 56%; height: 78%; border: 3rpx solid currentColor; border-radius: 50%; position: relative; transform: rotate(18deg); }
.bean::after { content: ''; position: absolute; left: 50%; top: 6%; bottom: 6%; width: 2rpx; background: currentColor; }
// 勋章
.medal { width: 64%; height: 64%; border: 3rpx solid currentColor; border-radius: 50%; position: relative; }
.medal::after { content: ''; position: absolute; left: 50%; top: 50%; width: 28%; height: 28%; transform: translate(-50%, -50%); border: 3rpx solid currentColor; border-radius: 50%; }
// 星
.star { width: 72%; height: 72%; background: currentColor; clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%); }
// 钻石
.diamond { width: 58%; height: 72%; background: currentColor; clip-path: polygon(50% 0%, 100% 42%, 50% 100%, 0% 42%); }
// 王冠
.crown { width: 74%; height: 52%; background: currentColor; clip-path: polygon(0% 100%, 0% 50%, 20% 72%, 32% 0%, 50% 52%, 68% 0%, 80% 72%, 100% 50%, 100% 100%); }
</style>