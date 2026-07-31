<!-- CoffeeOrigin: 产区国家轮廓 — 简化 silhouette + 散点布局。 -->
<template>
  <view class="coffee-origin">
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
      <text class="origin-cta" @click="onExplore">探索 8 个产区 →</text>
    </view>
  </view>
</template>

<script setup>
const emit = defineEmits(['explore'])

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

function onExplore() {
  uni.navigateTo({ url: '/pages/origins/index' })
}
</script>

<style lang="scss" scoped>
.coffee-origin {
  padding: 56rpx 12rpx 36rpx;
  background: $cozy-bg;
}

.origin-head {
  margin-bottom: 48rpx;
  padding: 0 16rpx;
}

.origin-label {
  display: block;
  color: $cozy-muted;
  font-size: 17rpx;
  font-weight: 700;
  letter-spacing: 0.14em;
  margin-bottom: 12rpx;
}

.origin-title {
  display: block;
  color: $cozy-ink;
  font-size: 36rpx;
}

.origin-sub {
  display: block;
  margin-top: 6rpx;
  color: $cozy-muted;
  font-size: 22rpx;
}

.origin-scatter {
  position: relative;
  width: 100%;
  height: 580rpx;
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
  letter-spacing: 0.12em;
}

/* 国家轮廓色块 */
.scatter-silhouette {
  opacity: 0.78;
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
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.scatter-flavor {
  color: $cozy-muted;
  font-size: 17rpx;
  white-space: nowrap;
}

.origin-foot {
  margin-top: 28rpx;
  text-align: center;
}

.origin-cta {
  color: $cozy-primary;
  font-size: 22rpx;
  font-weight: 650;
}
</style>
