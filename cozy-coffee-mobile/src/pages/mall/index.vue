<template>
  <view class="mall-page">
    <view class="points-header">
      <view class="points-info">
        <text class="points-label">当前积分</text>
        <text class="points-value">{{ userStore.memberInfo.currentPoints || 0 }}</text>
      </view>
      <view class="points-action" @click="goToHistory">积分明细 ></view>
    </view>

    <view class="mall-tabs">
      <view class="mall-tab" :class="{ active: activeTab === 'products' }" @click="activeTab = 'products'">兑换商城</view>
      <view class="mall-tab" @click="goToRedemptions">兑换记录</view>
    </view>

    <template v-if="activeTab === 'products'">
      <view v-if="redeemDiscount < 1" class="discount-tip">
        {{ getMemberLevelName(userStore.userLevel) }}积分兑换享 {{ discountLabel }}，结算积分按后端规则向上取整
      </view>
      <view v-if="productLoading && products.length === 0" class="page-state">正在加载积分商品…</view>
      <view v-else-if="productError" class="page-state error">
        <text>{{ productError }}</text>
        <button class="retry-button" @click="loadProducts">重新加载</button>
      </view>
      <view v-else-if="products.length === 0" class="page-state">暂无可兑换商品</view>
      <view v-else class="products-grid">
        <view v-for="item in products" :key="item.id" class="product-card" @click="openRedeemModal(item)">
          <image :src="item.image" class="product-image" mode="aspectFill" />
          <view class="product-info">
            <text class="product-name">{{ item.name }}</text>
            <text class="product-desc">{{ item.desc }}</text>
            <text v-if="item.monthlyLimit" class="product-limit">本月剩余 {{ getRedeemQuantityLimit(item) }} 件</text>
            <view class="product-footer">
              <view class="product-price">
                <text class="price-value">{{ getDiscountedPointsCost(item.pointsPrice, 1, userStore.userLevel) }}</text>
                <text class="price-unit">积分</text>
                <text v-if="redeemDiscount < 1" class="original-points">{{ item.pointsPrice }}</text>
              </view>
              <view v-if="canRedeem(item)" class="redeem-btn">兑换</view>
              <view v-else class="redeem-btn disabled">{{ getUnavailableText(item) }}</view>
            </view>
          </view>
        </view>
      </view>
    </template>

    <view v-if="showRedeemModal" class="modal-mask" @click="closeRedeemModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">确认兑换</text>
          <text class="modal-close" @click="closeRedeemModal">×</text>
        </view>
        <view v-if="selectedProduct" class="modal-body">
          <image :src="selectedProduct.image" class="modal-image" mode="aspectFill" />
          <text class="modal-name">{{ selectedProduct.name }}</text>
          <view class="quantity-row">
            <text class="quantity-label">兑换数量</text>
            <view class="stepper">
              <view class="stepper-btn" :class="{ disabled: redeemQuantity <= 1 }" @click="decreaseQuantity">−</view>
              <text class="stepper-value">{{ redeemQuantity }}</text>
              <view class="stepper-btn" :class="{ disabled: !canIncreaseQuantity }" @click="increaseQuantity">＋</view>
            </view>
          </view>
          <text class="modal-price">合计 {{ selectedTotalCost }} 积分</text>
          <text v-if="redeemDiscount < 1" class="modal-original-price">
            原价 {{ selectedProduct.pointsPrice * redeemQuantity }} 积分 · {{ discountLabel }}
          </text>
          <text class="modal-stock">库存 {{ selectedProduct.stock }} 件，当前最多可兑 {{ selectedQuantityLimit }} 件</text>
          <text v-if="!hasEnoughPoints" class="points-warning">当前积分不足</text>
        </view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="closeRedeemModal">取消</view>
          <view class="modal-btn confirm" :class="{ disabled: !canConfirmRedeem }" @click="confirmRedeem">
            {{ redeeming ? '兑换中…' : '确认兑换' }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getPointsProducts } from '@/api/product'
import { getMemberInfo, redeemPoints } from '@/api/member'
import { getMemberLevelName } from '@/constants/member'
import {
  POINTS_REDEEM_DISCOUNTS,
  getDiscountedPointsCost,
  getRedeemQuantityLimit
} from '@/domain/member/memberRules'

const userStore = useUserStore()
const activeTab = ref('products')
const products = ref([])
const showRedeemModal = ref(false)
const selectedProduct = ref(null)
const redeemQuantity = ref(1)
const productLoading = ref(false)
const redeeming = ref(false)
const productError = ref('')

const redeemDiscount = computed(() => POINTS_REDEEM_DISCOUNTS[userStore.userLevel] || 1)
const discountLabel = computed(() => `${Number((redeemDiscount.value * 10).toFixed(1))}折`)
const selectedQuantityLimit = computed(() => getRedeemQuantityLimit(selectedProduct.value || {}))
const selectedTotalCost = computed(() => getDiscountedPointsCost(
  selectedProduct.value?.pointsPrice,
  redeemQuantity.value,
  userStore.userLevel
))
const hasEnoughPoints = computed(() => (userStore.memberInfo?.currentPoints || 0) >= selectedTotalCost.value)
const canIncreaseQuantity = computed(() => {
  if (!selectedProduct.value || redeemQuantity.value >= selectedQuantityLimit.value) return false
  const nextCost = getDiscountedPointsCost(selectedProduct.value.pointsPrice, redeemQuantity.value + 1, userStore.userLevel)
  return (userStore.memberInfo?.currentPoints || 0) >= nextCost
})
const canConfirmRedeem = computed(() => (
  Boolean(selectedProduct.value) &&
  redeemQuantity.value >= 1 &&
  redeemQuantity.value <= selectedQuantityLimit.value &&
  hasEnoughPoints.value &&
  !redeeming.value
))

async function loadMember() {
  const response = await getMemberInfo()
  if (response.code === 200 && response.data) userStore.setMemberInfo(response.data)
}

async function loadProducts() {
  productLoading.value = true
  productError.value = ''
  try {
    const response = await getPointsProducts()
    products.value = (response.data || []).map(item => ({
      ...item,
      image: item.imageUrl || item.image || '/static/images/default-product.png',
      desc: item.description || ''
    }))
  } catch (error) {
    products.value = []
    productError.value = error?.message || '积分商品加载失败'
  } finally {
    productLoading.value = false
  }
}

async function loadMallData() {
  try {
    await loadMember()
  } catch (error) {
    productError.value = error?.message || '会员积分加载失败'
  }
  await loadProducts()
}

function canRedeem(item) {
  return getRedeemQuantityLimit(item) > 0 &&
    (userStore.memberInfo?.currentPoints || 0) >= getDiscountedPointsCost(item.pointsPrice, 1, userStore.userLevel)
}

function getUnavailableText(item) {
  if (getRedeemQuantityLimit(item) <= 0) return item.stock > 0 ? '已达限购' : '已兑完'
  return '积分不足'
}

function openRedeemModal(item) {
  if (!canRedeem(item)) {
    uni.showToast({ title: getUnavailableText(item), icon: 'none' })
    return
  }
  selectedProduct.value = item
  redeemQuantity.value = 1
  showRedeemModal.value = true
}

function closeRedeemModal() {
  if (redeeming.value) return
  showRedeemModal.value = false
  selectedProduct.value = null
  redeemQuantity.value = 1
}

function decreaseQuantity() {
  if (redeemQuantity.value > 1) redeemQuantity.value -= 1
}

function increaseQuantity() {
  if (canIncreaseQuantity.value) redeemQuantity.value += 1
}

async function confirmRedeem() {
  if (!canConfirmRedeem.value) return
  redeeming.value = true
  uni.showLoading({ title: '兑换中…', mask: true })
  try {
    const response = await redeemPoints({
      productId: selectedProduct.value.id,
      quantity: redeemQuantity.value,
      fulfillmentType: selectedProduct.value.productType === 'VIRTUAL' ? 'VIRTUAL' : 'PICKUP',
      storeId: selectedProduct.value.productType === 'VIRTUAL' ? undefined : 1
    })
    if (response.code === 200) {
      showRedeemModal.value = false
      selectedProduct.value = null
      redeemQuantity.value = 1
      uni.showToast({ title: '兑换成功', icon: 'success' })
      await loadMallData()
      activeTab.value = 'orders'
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '兑换失败', icon: 'none' })
  } finally {
    uni.hideLoading()
    redeeming.value = false
  }
}

function goToRedemptions() {
  uni.navigateTo({ url: '/pages/points/redemptions' })
}

function goToHistory() {
  uni.navigateTo({ url: '/pages/points/history' })
}

onShow(loadMallData)
</script>

<style lang="scss" scoped>
.mall-page { min-height: 100vh; padding: $spacing-md; background: $bg-color; }
.points-header { display: flex; justify-content: space-between; align-items: center; padding: $spacing-lg; border-radius: $cozy-radius-lg; background: $cozy-surface-alt; color: #fff; }
.points-label { display: block; font-size: $font-size-sm; opacity: .8; }
.points-value { font-size: 56rpx; font-weight: 700; }
.points-action { font-size: $font-size-sm; opacity: .85; }
.mall-tabs { display: flex; margin: $spacing-md 0; padding: 6rpx; border-radius: 999rpx; background: $bg-white; }
.mall-tab { flex: 1; padding: 18rpx; border-radius: 999rpx; color: $text-secondary; text-align: center; font-size: $font-size-sm; }
.mall-tab.active { background: $cozy-primary; color: #fff; font-weight: 600; }
.discount-tip { margin-bottom: $spacing-md; padding: 18rpx 22rpx; border-radius: $cozy-radius-md; background: $cozy-accent-soft; color: $cozy-primary; font-size: $font-size-xs; line-height: 1.5; }
.page-state { padding: 100rpx 24rpx; color: $cozy-muted; text-align: center; }
.page-state text { display: block; }
.page-state.error { color: $error-color; }
.retry-button { width: 220rpx; margin-top: 24rpx; border-radius: $cozy-radius-md; background: $cozy-primary; color: #fff; font-size: 24rpx; }
.products-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: $spacing-md; }
.product-card { overflow: hidden; border-radius: $cozy-radius-md; background: $bg-white; }
.product-image { width: 100%; height: 200rpx; }
.product-info { padding: $spacing-sm; }
.product-name, .product-desc, .product-limit { display: block; }
.product-name { margin-bottom: 4rpx; color: $text-primary; font-size: $font-size-md; font-weight: 600; }
.product-desc { min-height: 58rpx; margin-bottom: 8rpx; color: $text-placeholder; font-size: $font-size-xs; }
.product-limit { margin-bottom: 10rpx; color: $cozy-accent; font-size: 20rpx; }
.product-footer { display: flex; align-items: center; justify-content: space-between; gap: 8rpx; }
.price-value { color: $primary-color; font-size: $font-size-lg; font-weight: 600; }
.price-unit { margin-left: 4rpx; color: $primary-color; font-size: $font-size-xs; }
.original-points { margin-left: 8rpx; color: $text-placeholder; font-size: 20rpx; text-decoration: line-through; }
.redeem-btn { flex-shrink: 0; padding: 8rpx 16rpx; border-radius: 20rpx; background: $primary-color; color: #fff; font-size: $font-size-xs; }
.redeem-btn.disabled { background: #ccc; }
.modal-mask { position: fixed; inset: 0; z-index: 999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.5); }
.modal-content { width: 82%; overflow: hidden; border-radius: $cozy-radius-lg; background: $bg-white; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: $spacing-md; border-bottom: 1rpx solid $border-color; }
.modal-title { font-size: $font-size-lg; font-weight: 600; }
.modal-close { color: $text-placeholder; font-size: 48rpx; }
.modal-body { padding: $spacing-lg; text-align: center; }
.modal-image { width: 180rpx; height: 180rpx; margin-bottom: $spacing-md; border-radius: $cozy-radius-md; }
.modal-name { display: block; margin-bottom: $spacing-md; color: $text-primary; font-size: $font-size-lg; font-weight: 600; }
.quantity-row { display: flex; align-items: center; justify-content: space-between; margin: $spacing-md 0; }
.quantity-label { color: $text-secondary; font-size: $font-size-sm; }
.stepper { display: flex; align-items: center; overflow: hidden; border: 1rpx solid $border-color; border-radius: 999rpx; }
.stepper-btn { width: 70rpx; padding: 10rpx 0; color: $cozy-primary; font-size: 34rpx; }
.stepper-btn.disabled { color: $text-placeholder; }
.stepper-value { min-width: 58rpx; color: $text-primary; font-size: $font-size-md; font-weight: 600; }
.modal-price { display: block; margin-top: $spacing-sm; color: $primary-color; font-size: $font-size-xl; font-weight: 600; }
.modal-original-price, .modal-stock, .points-warning { display: block; margin-top: 8rpx; font-size: $font-size-xs; }
.modal-original-price, .modal-stock { color: $text-placeholder; }
.points-warning { color: $error-color; }
.modal-footer { display: flex; border-top: 1rpx solid $border-color; }
.modal-btn { flex: 1; padding: $spacing-md; text-align: center; font-size: $font-size-md; }
.modal-btn.cancel { color: $text-secondary; }
.modal-btn.confirm { background: $primary-color; color: #fff; }
.modal-btn.confirm.disabled { background: #ccc; }
</style>
