<!-- HomeHero: 季节性单品 Hero，单图 + 文字叠加，无轮播。v2.2: 60vh + 双 CTA。 -->
<template>
  <view class="home-hero">
    <image :src="hero.image" mode="aspectFill" class="hero-image" />
    <view class="hero-shade" />
    <view class="hero-copy">
      <text class="hero-kicker">{{ hero.kicker }}</text>
      <text class="hero-title cozy-display">{{ hero.name }}</text>
      <text class="hero-notes">{{ hero.notes }}</text>
      <view class="hero-actions">
        <view class="hero-primary" @click="onOrder">
          <text>开始点单</text>
          <text class="cta-arrow">→</text>
        </view>
        <text class="hero-secondary" @click="onExplore">探索本季</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const emit = defineEmits(['order', 'explore'])

const hero = {
  image: '/static/images/home/hero-seasonal.jpg',
  kicker: 'SEASON COLLECTION',
  name: '云南保山 SOE',
  notes: '花香 · 柑橘 · 蜂蜜'
}

// 订单模式预留：当前仅自提，未来扩展配送无需重构组件结构。
const orderMode = { pickup: true, delivery: false }

function onOrder() {
  emit('order')
}

function onExplore() {
  emit('explore', hero)
}
</script>

<style lang="scss" scoped>
.home-hero {
  position: relative;
  height: 60vh;
  background: $cozy-surface-alt;
  overflow: hidden;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(18, 12, 9, 0.48) 0%,
    rgba(18, 12, 9, 0.08) 38%,
    rgba(18, 12, 9, 0.72) 100%
  );
}

.hero-copy {
  position: absolute;
  right: 32rpx;
  bottom: 80rpx;
  left: 32rpx;
  color: #fff;
}

.hero-kicker {
  display: block;
  margin-bottom: 12rpx;
  font-size: 19rpx;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.hero-title {
  display: block;
  max-width: 610rpx;
  font-size: 48rpx;
  line-height: 1.22;
}

.hero-notes {
  display: block;
  max-width: 560rpx;
  margin-top: 14rpx;
  color: rgba(255, 255, 255, 0.84);
  font-size: 24rpx;
  line-height: 1.55;
}

.hero-actions {
  margin-top: 22rpx;
  display: flex;
  align-items: center;
  gap: 28rpx;
}

.hero-primary {
  min-height: 76rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.85);
  border-radius: $cozy-radius-md;
  font-size: 24rpx;
  font-weight: 650;
}

.cta-arrow {
  font-size: 30rpx;
}

.hero-secondary {
  font-size: 22rpx;
  opacity: 0.78;
}
</style>
