<template>
  <image :src="badgeSrc" :style="{ width: size + 'rpx', height: size + 'rpx' }" mode="aspectFit" class="level-badge" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: { type: String, default: 'basic' },
  size: { type: Number, default: 48 }
})

// 等级徽章 PNG —— 由原型 profile.html EMBLEMS 的 SVG 用主题 accent 色导出
const BADGE_MAP = {
  basic: '/static/images/member/badge-basic.png',
  silver: '/static/images/member/badge-silver.png',
  gold: '/static/images/member/badge-gold.png',
  diamond: '/static/images/member/badge-diamond.png',
  black: '/static/images/member/badge-black.png'
}

const badgeSrc = computed(() => BADGE_MAP[props.level] || BADGE_MAP.basic)
</script>

<style lang="scss" scoped>
.level-badge {
  flex: none;
  filter: drop-shadow(0 6rpx 10rpx rgba(0, 0, 0, 0.22));
}
</style>
