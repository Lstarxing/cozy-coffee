<template>
  <view class="detail-page">
    <LoadingState v-if="loading" text="正在读取商品…" />
    <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadProduct" />

    <view v-else-if="product" class="detail-content">
      <image :src="product.image" class="product-hero" mode="aspectFill" />

      <view class="product-info">
        <text class="product-category">{{ categoryLabel }}</text>
        <text class="product-name cozy-display">{{ product.name }}</text>
        <text class="product-description">{{ product.description || '门店现制，按所选规格制作。' }}</text>
        <view class="price-row">
          <text class="price-label">基础价</text>
          <text class="product-price">¥{{ money(product.price) }}</text>
        </view>
      </view>

      <view class="spec-hint">
        <view>
          <text class="hint-title">选择你的口味</text>
          <text class="hint-copy">杯型、温度、甜度、奶型与咖啡浓度以当前商品配置为准。</text>
        </view>
        <text class="hint-arrow">→</text>
      </view>
      <view class="detail-spacer" />
    </view>

    <view v-if="product" class="action-bar safe-area-bottom">
      <view class="cart-summary" @click="goToMenu">
        <text class="cart-count">{{ cartStore.totalCount }}</text>
        <view>
          <text class="cart-label">购物车</text>
          <text class="cart-total">¥{{ cartStore.totalPrice }}</text>
        </view>
      </view>
      <view class="select-button" @click="specVisible = true">选择规格</view>
    </view>

    <ProductSpecSheet
      :visible="specVisible"
      :product="product || {}"
      @close="specVisible = false"
      @confirm="addToCart"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getProductDetail } from '@/api/product'
import { resolveImageUrl } from '@/config/image'
import { useCartStore } from '@/stores/cart'
import { money } from '@/utils/format'
import ProductSpecSheet from '@/components/order/ProductSpecSheet.vue'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'

const cartStore = useCartStore()
const productId = ref('')
const product = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const specVisible = ref(false)

const categoryLabel = computed(() => ({
  espresso: '意式咖啡', coffee: '经典咖啡', latte: '拿铁系列', signature: '季节特调',
  soe: '手冲精品', bakery: '烘焙甜点', dessert: '甜品', addon: '加料', other: '门店现制'
})[String(product.value?.category || 'other').toLowerCase()] || '门店现制')

onLoad(options => {
  productId.value = String(options?.id || options?.productId || '')
  loadProduct()
})

async function loadProduct() {
  if (!productId.value) {
    loading.value = false
    errorMessage.value = '缺少商品编号，请返回菜单重新选择'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getProductDetail(productId.value)
    const data = response?.data ?? response
    if (!data || (data.status && String(data.status).toLowerCase() !== 'active')) {
      throw new Error('该商品暂时无法购买，请返回菜单选择其他商品')
    }
    product.value = {
      ...data,
      productId: String(data.id ?? productId.value),
      image: resolveImageUrl(data.imageUrl) || resolveImageUrl(data.image) || '/static/images/default-product.png',
      price: Number(data.price || 0)
    }
  } catch (error) {
    errorMessage.value = error?.message || '商品加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function addToCart(line) {
  cartStore.addItem(line, line.quantity)
  specVisible.value = false
  uni.setStorageSync('cozy_open_cart_on_menu', '1')
  uni.switchTab({
    url: '/pages/menu/menu',
    success: () => uni.showToast({ title: `已加入 ${line.quantity} 件`, icon: 'none' })
  })
}

function goToMenu() {
  uni.setStorageSync('cozy_open_cart_on_menu', '1')
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>

<style lang="scss" scoped>
.detail-page { min-height: 100vh; background: $cozy-surface; }
.detail-content { padding-bottom: 28rpx; }
.product-hero { width: 100%; height: 620rpx; display: block; background: $cozy-surface-alt; }
.product-info { margin: -34rpx 24rpx 0; padding: 34rpx 30rpx 30rpx; position: relative; border-radius: $cozy-radius-lg; background: #fff; }
.product-category { display: block; color: $cozy-primary; font-size: 20rpx; font-weight: 750; letter-spacing: .08em; }
.product-name { display: block; margin-top: 12rpx; color: $cozy-ink; font-size: 44rpx; }
.product-description { display: block; margin-top: 16rpx; color: $cozy-muted; font-size: 24rpx; line-height: 1.65; }
.price-row { margin-top: 26rpx; padding-top: 22rpx; display: flex; align-items: baseline; justify-content: space-between; border-top: 1rpx solid $cozy-border; }
.price-label { color: $cozy-muted; font-size: 22rpx; }
.product-price { color: $cozy-primary; font-size: 38rpx; font-weight: 760; }
.spec-hint { margin: 20rpx 24rpx 0; padding: 26rpx 28rpx; display: flex; align-items: center; justify-content: space-between; gap: 24rpx; border-radius: $cozy-radius-lg; background: $cozy-surface-alt; color: #fff; }
.hint-title { display: block; font-size: 27rpx; font-weight: 700; }
.hint-copy { display: block; margin-top: 8rpx; color: $cozy-muted-on-dark; font-size: 20rpx; line-height: 1.5; }
.hint-arrow { flex: none; font-size: 34rpx; }
.detail-spacer { height: 150rpx; }
.action-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 30; min-height: 108rpx; padding: 16rpx 26rpx max(16rpx, env(safe-area-inset-bottom)); display: flex; align-items: center; gap: 22rpx; border-top: 1rpx solid $cozy-border; background: #fff; }
.cart-summary { min-width: 210rpx; display: flex; align-items: center; gap: 14rpx; }
.cart-count { width: 54rpx; height: 54rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-surface-alt; color: #fff; font-size: 22rpx; font-weight: 750; }
.cart-label { display: block; color: $cozy-muted; font-size: 19rpx; }
.cart-total { display: block; margin-top: 4rpx; color: $cozy-ink; font-size: 25rpx; font-weight: 700; }
.select-button { flex: 1; height: 84rpx; display: flex; align-items: center; justify-content: center; border-radius: $cozy-radius-md; background: $cozy-primary; color: #fff; font-size: 27rpx; font-weight: 700; }
</style>
