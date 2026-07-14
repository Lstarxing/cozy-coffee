<template>
  <view class="product-item" @click="$emit('select', product)">
    <image class="product-image" :src="product.image" mode="aspectFill" />
    <view class="product-body">
      <view class="product-title-row">
        <text class="product-name">{{ product.name }}</text>
        <text v-if="product.isNewProduct" class="new-tag">新品</text>
      </view>
      <text class="product-description">{{ product.description || '门店现制，建议尽快饮用' }}</text>
      <view class="product-footer">
        <view class="price-wrap">
          <text class="currency">¥</text>
          <text class="price">{{ formatPrice(product.price) }}</text>
          <text v-if="hasMultipleSpecs" class="price-suffix">起</text>
        </view>
        <view class="add-button" hover-class="add-button--pressed" @click.stop="$emit('add', product)">
          <text class="add-icon">＋</text>
          <text v-if="count" class="product-count">{{ count }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  product: { type: Object, required: true },
  count: { type: Number, default: 0 }
})

defineEmits(['select', 'add'])

const hasMultipleSpecs = computed(() => props.product.sizeType && props.product.sizeType !== 'DEFAULT')
const formatPrice = value => Number(value || 0).toFixed(0)
</script>

<style lang="scss" scoped>
.product-item {
  display: flex;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $cozy-border;
}

.product-image {
  width: 164rpx;
  height: 164rpx;
  flex: 0 0 164rpx;
  border-radius: $cozy-radius-md;
  background: $cozy-surface;
}

.product-body {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-title-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.product-name {
  min-width: 0;
  color: $cozy-ink;
  font-size: 30rpx;
  font-weight: 650;
  line-height: 1.35;
}

.new-tag {
  flex: none;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  background: $cozy-accent-soft;
  color: $cozy-accent;
  font-size: 20rpx;
  font-weight: 600;
}

.product-description {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 10rpx;
  color: $cozy-muted;
  font-size: 23rpx;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 12rpx;
}

.price-wrap {
  display: flex;
  align-items: baseline;
  color: $cozy-ink;
}

.currency { font-size: 22rpx; font-weight: 700; }
.price { font-size: 34rpx; font-weight: 750; line-height: 1; }
.price-suffix { margin-left: 4rpx; color: $cozy-muted; font-size: 20rpx; }

.add-button {
  position: relative;
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $cozy-primary;
  transition: transform 160ms ease-out, background 160ms ease-out;
}

.add-button--pressed { transform: scale(.94); background: $cozy-primary-hover; }
.add-icon { color: #fff; font-size: 42rpx; font-weight: 400; line-height: 1; }

.product-count {
  position: absolute;
  top: -10rpx;
  right: -8rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 6rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3rpx solid #fff;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 19rpx;
  box-sizing: border-box;
}
</style>
