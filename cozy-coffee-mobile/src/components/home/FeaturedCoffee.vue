<!-- FeaturedCoffee: 本季精选 3 款产品横滑，杂志卡片风格。 -->
<template>
  <view class="featured-coffee">
    <view class="featured-head">
      <text class="featured-kicker">THIS SEASON</text>
      <text class="featured-title cozy-display">本季精选</text>
    </view>

    <scroll-view scroll-x class="featured-rail" :show-scrollbar="false">
      <view class="featured-inner">
        <view
          v-for="item in products"
          :key="item.id"
          class="featured-card"
          @click="onSelect(item)"
        >
          <image :src="item.image" mode="aspectFill" class="card-image" />
          <view class="card-copy">
            <text class="card-name">{{ item.name }}</text>
            <text class="card-notes">{{ item.notes }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { imageUrl } from '@/config/image'

const emit = defineEmits(['select'])

const products = [
  {
    id: 1,
    name: '云南保山 SOE',
    notes: 'Floral · Citrus · Honey',
    image: imageUrl('product-yunnan-soe.png')
  },
  {
    id: 2,
    name: 'Dirty',
    notes: 'Espresso · Cold Milk · Layers',
    image: imageUrl('product-dirty.png')
  },
  {
    id: 3,
    name: '冷萃咖啡',
    notes: '12hr Brew · Smooth · Sweet',
    image: imageUrl('product-coldbrew.png')
  },
  {
    id: 4,
    name: '埃塞俄比亚 耶加雪菲',
    notes: 'Jasmine · Lemon · Tea',
    image: imageUrl('product-ethiopia-yirgacheffe.png')
  },
  {
    id: 5,
    name: '海盐焦糖拿铁',
    notes: 'Sea Salt · Caramel · Velvet',
    image: imageUrl('product-salted-caramel-latte.png')
  }
]

function onSelect(item) {
  emit('select', item)
}
</script>

<style lang="scss" scoped>
.featured-coffee {
  padding: 56rpx 0 52rpx;
  background: $cozy-surface;
}

.featured-head {
  padding: 0 28rpx 28rpx;
}

.featured-kicker {
  display: block;
  color: $cozy-muted;
  font-size: 17rpx;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.featured-title {
  display: block;
  margin-top: 8rpx;
  color: $cozy-ink;
  font-size: 36rpx;
}

.featured-rail {
  width: 100%;
  white-space: nowrap;
}

.featured-inner {
  padding: 0 28rpx;
  display: flex;
  gap: 20rpx;
}

.featured-card {
  width: 292rpx;
  flex: 0 0 292rpx;
  overflow: hidden;
  border-radius: $cozy-radius-lg;
  background: $cozy-bg;
}

.card-image {
  width: 100%;
  height: 340rpx;
  background: $cozy-surface;
}

.card-copy {
  padding: 22rpx 20rpx 24rpx;
}

.card-name {
  display: block;
  overflow: hidden;
  color: $cozy-ink;
  font-size: 27rpx;
  font-weight: 680;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-notes {
  display: block;
  margin-top: 8rpx;
  color: $cozy-muted;
  font-size: 19rpx;
  letter-spacing: 0.03em;
  white-space: normal;
}
</style>
