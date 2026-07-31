<!-- HomeHero: 3 张品牌叙事轮播。产品 → 故事 → 生活方式。 -->
<template>
  <view class="home-hero">
    <swiper
      class="hero-swiper"
      :autoplay="true"
      :interval="5000"
      :duration="800"
      :circular="true"
      :current="currentSlide"
      @change="onSlideChange"
    >
      <swiper-item v-for="(slide, idx) in slides" :key="idx">
        <image :src="slide.image" mode="aspectFill" class="hero-image" />
        <view class="hero-shade" :class="'shade-' + slide.shade" />
        <view class="hero-copy" :class="['copy-' + slide.copyPos, 'v-' + slide.copyVPos]">
          <text class="hero-kicker">{{ slide.kicker }}</text>
          <text class="hero-title cozy-display">{{ slide.title }}</text>
          <text v-if="slide.subtitle" class="hero-subtitle">{{ slide.subtitle }}</text>
          <text class="hero-notes">{{ slide.notes }}</text>
          <view class="hero-actions">
            <view class="hero-primary" @click.stop="onAction(slide.primaryAction)">
              <text>{{ slide.primaryLabel }}</text>
              <text class="cta-arrow">→</text>
            </view>
            <text v-if="slide.secondaryLabel" class="hero-secondary" @click.stop="onAction(slide.secondaryAction)">
              {{ slide.secondaryLabel }}
            </text>
          </view>
        </view>
      </swiper-item>
    </swiper>

    <view class="hero-indicator">
      <view
        v-for="(_, idx) in slides"
        :key="idx"
        class="indicator-line"
        :class="{ active: currentSlide === idx }"
      />
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['order', 'explore'])

const currentSlide = ref(0)
const slides = [
  {
    image: '/static/images/home/hero-season-soe.jpg',
    shade: 'bottom',
    copyPos: 'left',
    copyVPos: 'bottom',
    kicker: 'SEASON COLLECTION',
    title: '云南保山 SOE',
    subtitle: '',
    notes: '花香 · 柑橘 · 蜂蜜',
    primaryLabel: '开始点单',
    primaryAction: { type: 'order' },
    secondaryLabel: '探索本季',
    secondaryAction: { type: 'explore' }
  },
  {
    image: '/static/images/home/hero-origin-notes.jpg',
    shade: 'top',
    copyPos: 'left',
    copyVPos: 'top',
    kicker: 'ORIGIN NOTES',
    title: '从土地，',
    subtitle: '到杯中的风味',
    notes: '云南保山咖啡产地记录',
    primaryLabel: '阅读故事',
    primaryAction: { type: 'navigate', url: '/pages/origins/index' },
    secondaryLabel: '',
    secondaryAction: null
  },
  {
    image: '/static/images/home/hero-coffee-ritual.jpg',
    shade: 'subtle',
    copyPos: 'right',
    copyVPos: 'top',
    kicker: 'COFFEE RITUAL',
    title: '一杯咖啡，',
    subtitle: '一段慢下来的时间',
    notes: '在日常中，找到属于自己的节奏',
    primaryLabel: '探索咖啡时刻',
    primaryAction: { type: 'explore' },
    secondaryLabel: '',
    secondaryAction: null
  }
]

function onSlideChange(e) {
  currentSlide.value = e.detail.current
}

function onAction(action) {
  if (!action) return
  if (action.type === 'order') emit('order')
  if (action.type === 'explore') emit('explore')
  if (action.type === 'navigate') uni.navigateTo({ url: action.url })
}
</script>

<style lang="scss" scoped>
.home-hero {
  position: relative;
  height: 60vh;
  background: $cozy-surface-alt;
}

.hero-swiper {
  width: 100%;
  height: 100%;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero-shade {
  position: absolute;
  inset: 0;
}
.shade-bottom {
  background: linear-gradient(180deg, rgba(18,12,9,0.32) 0%, rgba(18,12,9,0.04) 48%, rgba(18,12,9,0.84) 100%);
}
.shade-top {
  background: linear-gradient(180deg, rgba(18,12,9,0.84) 0%, rgba(18,12,9,0.12) 55%, rgba(18,12,9,0.08) 100%);
}
.shade-subtle {
  background: linear-gradient(160deg, rgba(18,12,9,0.44) 0%, rgba(18,12,9,0.04) 42%, rgba(18,12,9,0.58) 100%);
}

.hero-copy {
  position: absolute;
  right: 32rpx;
  bottom: 80rpx;
  left: 32rpx;
  color: #fff;
}
.hero-copy.v-top {
  top: 80rpx;
  bottom: auto;
}
.copy-right { text-align: right; }
.copy-left { text-align: left; }

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
.copy-right .hero-title { margin-left: auto; }

.hero-subtitle {
  display: block;
  margin-top: 4rpx;
  font-size: 40rpx;
  line-height: 1.22;
  font-family: inherit;
}

.hero-notes {
  display: block;
  max-width: 560rpx;
  margin-top: 14rpx;
  color: rgba(255,255,255,0.84);
  font-size: 24rpx;
  line-height: 1.55;
}
.copy-right .hero-notes { margin-left: auto; }

.hero-actions {
  margin-top: 22rpx;
  display: flex;
  align-items: center;
  gap: 28rpx;
}
.copy-right .hero-actions { justify-content: flex-end; }

.hero-primary {
  min-height: 76rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid rgba(255,255,255,0.85);
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

/* 指示器：极细线 */
.hero-indicator {
  position: absolute;
  bottom: 32rpx;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 16rpx;
  z-index: 2;
}
.indicator-line {
  width: 40rpx;
  height: 3rpx;
  border-radius: 1rpx;
  background: rgba(255,255,255,0.32);
  transition: all 0.4s ease;
}
.indicator-line.active {
  width: 56rpx;
  background: rgba(255,255,255,0.85);
}
</style>
