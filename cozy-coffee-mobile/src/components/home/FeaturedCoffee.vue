<!-- FeaturedCoffee: 编辑精选 3 款产品横滑，杂志卡片风格，无加购按钮。 -->
<template>
  <view class="featured-coffee">
    <view class="featured-head">
      <view>
        <text class="featured-title cozy-display">Today's Selection</text>
        <text class="featured-sub">编辑为你挑选的三杯</text>
      </view>
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
            <text class="card-desc">{{ item.description }}</text>
            <view class="card-footer">
              <text class="card-price">¥{{ item.price }}</text>
              <text class="card-action">选规格 →</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
const emit = defineEmits(['select'])

// Phase 1 mock data — Phase 2 接入 getRecommendProducts().slice(0,3)
const products = [
  {
    id: 1,
    name: '云南保山 SOE',
    description: '花香 · 柑橘 · 蜂蜜，单一产地手冲',
    price: '28',
    image: '/static/images/default-product.png'
  },
  {
    id: 2,
    name: 'Dirty',
    description: '浓缩碰撞冰牛奶，渐变层次',
    price: '32',
    image: '/static/images/default-product.png'
  },
  {
    id: 3,
    name: '冷萃咖啡',
    description: '12 小时低温萃取，柔和甘甜',
    price: '30',
    image: '/static/images/default-product.png'
  }
]

function onSelect(item) {
  emit('select', item)
}
</script>

<style lang="scss" scoped>
.featured-coffee {
  padding: 68rpx 0 60rpx;
  background: $cozy-surface;
}

.featured-head {
  padding: 0 28rpx 24rpx;
}

.featured-title {
  display: block;
  color: $cozy-ink;
  font-size: 36rpx;
}

.featured-sub {
  display: block;
  margin-top: 8rpx;
  color: $cozy-muted;
  font-size: 22rpx;
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
  border: 1rpx solid $cozy-border;
}

.card-image {
  width: 100%;
  height: 292rpx;
  background: $cozy-surface;
}

.card-copy {
  padding: 20rpx;
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

.card-desc {
  display: -webkit-box;
  overflow: hidden;
  height: 67rpx;
  margin-top: 9rpx;
  color: $cozy-muted;
  font-size: 20rpx;
  line-height: 1.55;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-footer {
  margin-top: 17rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12rpx;
}

.card-price {
  color: $cozy-primary;
  font-size: 29rpx;
  font-weight: 760;
}

.card-action {
  color: $cozy-ink;
  font-size: 19rpx;
  font-weight: 650;
}
</style>
