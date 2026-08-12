<!--
  首页 - 完全复刻 prototype/index.html：极简 Editorial 滚动
  第一屏：Hero（大图 + 左上文案）+ 自提/外送 + 5 极简入口
  第二屏：Origin Archive 八产区散布
-->
<template>
  <view class="home-page">
    <scroll-view scroll-y class="content-scroll">
      <!-- ═══ 第一屏：Hero + 服务入口 ═══ -->
      <view class="screen-1">
        <view class="hero">
          <image class="hero-img" src="/static/images/home/hero-yunnan-natural.jpg" mode="aspectFill" />
          <view class="hero-copy">
            <text class="hero-kicker">COZY COFFEE</text>
            <text class="hero-cn">一杯咖啡</text>
            <text class="hero-cn sub">一段安静时光</text>
            <text class="hero-en">A quiet moment with coffee</text>
          </view>
        </view>

        <view class="service">
          <view class="pickup-row">
            <view class="pickup-half" @click="goMenu">
              <text class="pickup-cn">自提</text>
              <text class="pickup-en">PICK UP</text>
            </view>
            <view class="pickup-divider" />
            <view class="pickup-half" @click="goMenu">
              <text class="pickup-cn">外送</text>
              <text class="pickup-en">DELIVERY</text>
            </view>
          </view>

          <view class="entry-row">
            <view v-for="entry in entries" :key="entry.label" class="entry-item" @click="goPage(entry.url)">
              <view class="entry-icon"><CozyIcon :name="entry.icon" :size="18" color="#8B8178" /></view>
              <text class="entry-label">{{ entry.label }}</text>
            </view>
          </view>
        </view>

        <text class="scroll-hint">⌄</text>
      </view>

      <!-- ═══ 第二屏：Origin Archive — 八个产区色块 ═══ -->
      <view class="origin-archive">
        <view class="origin-head">
          <text class="origin-label">ORIGIN ARCHIVE</text>
          <text class="origin-title cozy-display">风味从土地开始</text>
          <text class="origin-sub">来自世界的八种风味，在杭州相遇</text>
        </view>

        <view class="origin-scatter">
          <view
            v-for="o in origins"
            :key="o.id"
            class="scatter-item"
            :class="{ 'scatter-em': o.id === 'yunnan' }"
            :style="{ left: o.pos[0], top: o.pos[1] }"
          >
            <view
              class="scatter-silhouette"
              :style="{ background: o.color, clipPath: o.silhouette, width: o.size[0], height: o.size[1] }"
            />
            <text class="scatter-name">{{ o.name }}</text>
            <text class="scatter-flavor">{{ o.flavor }}</text>
          </view>
        </view>

        <view class="origin-foot">
          <text class="origin-cta" @click="goPage('/pages/origins/index')">探索 8 个产区 →</text>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup>
import CozyIcon from '@/components/CozyIcon.vue'

const entries = [
  { label: '积分商城', icon: 'gift', url: '/pages/mall/index' },
  { label: '每日签到', icon: 'calendar', url: '/pages/signin/index' },
  { label: '会员权益', icon: 'shield', url: '/pages/benefits/index' },
  { label: '门店信息', icon: 'pin', url: '/pages/store/list' },
  { label: '关于我们', icon: 'sparkle', url: '/pages/about/index' }
]

// 八产区散布数据 —— 与原型 index.html / CoffeeOrigin 一致
const origins = [
  {
    id: 'ethiopia',
    name: 'ETHIOPIA',
    flavor: '花香 · 柑橘',
    color: '#D8CBB8',
    size: ['90rpx', '80rpx'],
    silhouette: 'polygon(40% 0, 65% 3%, 90% 8%, 100% 18%, 95% 35%, 78% 55%, 55% 72%, 30% 100%, 15% 88%, 5% 62%, 0 38%, 8% 18%, 18% 5%)',
    pos: ['47%', '13%']
  },
  {
    id: 'kenya',
    name: 'KENYA',
    flavor: '莓果 · 明亮',
    color: '#D8C1B5',
    size: ['78rpx', '70rpx'],
    silhouette: 'polygon(30% 0, 55% 2%, 78% 8%, 100% 22%, 88% 45%, 70% 60%, 55% 82%, 35% 100%, 15% 85%, 5% 58%, 0 32%, 8% 12%)',
    pos: ['15%', '31%']
  },
  {
    id: 'colombia',
    name: 'COLOMBIA',
    flavor: '焦糖 · 均衡',
    color: '#B8AD98',
    size: ['64rpx', '88rpx'],
    silhouette: 'polygon(38% 0, 62% 2%, 80% 8%, 90% 16%, 72% 28%, 65% 42%, 78% 62%, 68% 80%, 52% 100%, 30% 88%, 15% 65%, 8% 40%, 12% 18%, 22% 5%)',
    pos: ['79%', '27%']
  },
  {
    id: 'yunnan',
    name: 'YUNNAN',
    flavor: '茶感 · 坚果',
    color: '#753A22',
    size: ['100rpx', '84rpx'],
    silhouette: 'polygon(30% 0, 55% 3%, 80% 6%, 100% 12%, 95% 28%, 85% 42%, 72% 58%, 55% 78%, 40% 100%, 22% 90%, 8% 68%, 2% 42%, 5% 20%, 15% 5%)',
    pos: ['47%', '53%']
  },
  {
    id: 'brazil',
    name: 'BRAZIL',
    flavor: '坚果 · 巧克力',
    color: '#C9B99A',
    size: ['100rpx', '68rpx'],
    silhouette: 'polygon(8% 5%, 30% 0, 55% 2%, 78% 8%, 100% 22%, 95% 48%, 82% 68%, 58% 100%, 32% 85%, 12% 58%, 2% 30%, 4% 12%)',
    pos: ['13%', '65%']
  },
  {
    id: 'indonesia',
    name: 'INDONESIA',
    flavor: '草本 · 木质',
    color: '#AAB4B5',
    size: ['96rpx', '50rpx'],
    silhouette: 'polygon(5% 25%, 20% 8%, 42% 0, 62% 8%, 80% 5%, 100% 18%, 92% 45%, 72% 65%, 50% 100%, 28% 85%, 10% 65%, 2% 40%)',
    pos: ['81%', '63%']
  },
  {
    id: 'guatemala',
    name: 'GUATEMALA',
    flavor: '香料 · 坚果',
    color: '#B4BEAA',
    size: ['68rpx', '60rpx'],
    silhouette: 'polygon(15% 0, 45% 5%, 75% 8%, 100% 22%, 88% 48%, 62% 68%, 35% 100%, 12% 85%, 2% 55%, 5% 25%)',
    pos: ['27%', '85%']
  },
  {
    id: 'panama',
    name: 'PANAMA',
    flavor: '花香 · 茶感',
    color: '#D6C3A8',
    size: ['78rpx', '38rpx'],
    silhouette: 'polygon(2% 25%, 18% 5%, 40% 0, 62% 10%, 82% 8%, 100% 25%, 92% 55%, 72% 80%, 48% 100%, 22% 82%, 8% 58%, 3% 38%)',
    pos: ['67%', '87%']
  }
]

function goMenu() { uni.switchTab({ url: '/pages/menu/menu' }) }
function goPage(url) { uni.navigateTo({ url }) }
</script>

<style lang="scss" scoped>
.home-page { height: 100vh; overflow: hidden; background: $cozy-bg; }
.content-scroll { height: 100%; }

/* ═══════════════════════════════════════
   HERO
   ═══════════════════════════════════════ */
.hero {
  position: relative;
  height: 60vh;
  overflow: hidden;
}
.hero-img {
  width: 100%;
  height: 100%;
}
.hero-copy {
  position: absolute;
  top: 110rpx;
  left: 80rpx;
  width: 240rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.hero-kicker {
  display: block;
  font-size: 24rpx;
  font-weight: 500;
  letter-spacing: .42em;
  color: rgba(44,39,35,.55);
  white-space: nowrap;
}
.hero-cn {
  display: block;
  margin-top: 140rpx;
  font-family: $font-display;
  font-size: 56rpx;
  font-weight: 400;
  color: #3A342E;
  line-height: 1;
  letter-spacing: .03em;
  white-space: nowrap;
}
.hero-cn.sub {
  margin-top: 40rpx;
  font-size: 44rpx;
}
.hero-en {
  display: block;
  margin-top: 80rpx;
  font-family: $font-display;
  font-size: 24rpx;
  font-weight: 400;
  color: rgba(44,39,35,.55);
  line-height: 1.6;
  letter-spacing: .08em;
  font-style: italic;
}

/* ── 第一屏容器 + 向下滚动提示 ── */
.screen-1 {
  position: relative;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.scroll-hint {
  position: absolute;
  bottom: 120rpx;
  left: 50%;
  transform: translateX(-50%);
  font-size: 40rpx;
  line-height: 1;
  color: $cozy-muted;
  opacity: .65;
  pointer-events: none;
  animation: hint-bounce 1.8s ease-in-out infinite;
}
@keyframes hint-bounce {
  0%, 100% { transform: translate(-50%, 0); }
  50% { transform: translate(-50%, 6rpx); }
}

/* ═══════════════════════════════════════
   SERVICE AREA — 30%
   ═══════════════════════════════════════ */
.service {
  padding: 64rpx 40rpx 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  background: $cozy-bg;
}

/* Pickup / Delivery split */
.pickup-row {
  display: flex;
  align-items: stretch;
  margin-bottom: 60rpx;
}
.pickup-half {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
}
.pickup-cn {
  display: block;
  font-family: $font-display;
  font-size: 44rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.pickup-en {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  font-weight: 500;
  letter-spacing: .14em;
  color: $cozy-muted;
}
.pickup-divider {
  width: 1rpx;
  align-self: stretch;
  background: $cozy-muted;
  opacity: .18;
}

/* Five minimal entries */
.entry-row {
  display: flex;
  justify-content: space-between;
}
.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  color: $cozy-muted;
}
.entry-icon {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: .5;
}
.entry-label {
  font-size: 20rpx;
  font-weight: 400;
  letter-spacing: .04em;
}

/* ═══════════════════════════════════════
   ORIGIN ARCHIVE — 八个产区色块
   ═══════════════════════════════════════ */
.origin-archive {
  min-height: 100vh;
  padding: 0 12rpx 80rpx;
  background: $cozy-bg;
  display: flex;
  flex-direction: column;
}
.origin-head {
  margin-top: 128rpx;
  padding: 0 16rpx;
  text-align: center;
}
.origin-label {
  display: block;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: .14em;
  color: $cozy-muted;
}
.origin-title {
  display: block;
  margin-top: 20rpx;
  font-family: $font-display;
  font-size: 52rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.origin-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: $cozy-muted;
}
.origin-scatter {
  position: relative;
  width: 100%;
  height: 580rpx;
  margin: auto 0;
  flex-shrink: 0;
  padding: 32rpx 24rpx 8rpx;
  box-sizing: content-box;
}
.scatter-item {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  transform: translate(-50%, -50%);
}
.scatter-em {
  z-index: 1;
}
.scatter-em .scatter-name {
  color: $cozy-primary;
  font-size: 22rpx;
  letter-spacing: .12em;
}
.scatter-silhouette {
  opacity: .78;
  background-image:
    radial-gradient(rgba(0,0,0,0.05) 0.5px, transparent 0.5px),
    radial-gradient(rgba(255,255,255,0.08) 0.8px, transparent 0.8px);
  background-size: 3px 3px, 5px 5px;
  background-position: 0 0, 2px 1px;
}
.scatter-name {
  color: $cozy-ink;
  font-size: 18rpx;
  font-weight: 700;
  letter-spacing: .08em;
  white-space: nowrap;
}
.scatter-flavor {
  color: $cozy-muted;
  font-size: 17rpx;
  white-space: nowrap;
}
.origin-foot {
  margin: 56rpx 0 20rpx;
  text-align: center;
}
.origin-cta {
  color: $cozy-primary;
  font-size: 26rpx;
  font-weight: 650;
}

.bottom-spacer { height: 140rpx; }
</style>
