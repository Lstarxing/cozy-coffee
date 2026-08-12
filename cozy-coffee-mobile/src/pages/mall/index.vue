<!--
  积分商城 - 复现 prototype/mall.html 极简 Editorial
  衬线积分头 + 文字快捷链接 + 下划线分类 Tab（全部/优惠券/实物礼品）+ 1:1 商品网格 + 商品详情覆盖层
-->
<template>
  <view class="mall-page">
    <!-- 积分头 -->
    <view class="points-header">
      <text class="points-label">当前可用积分</text>
      <text class="points-value">{{ userStore.memberInfo.currentPoints || 0 }}</text>
    </view>

    <!-- 文字快捷链接 -->
    <view class="quick-links">
      <text class="quick-link" @click="goToHistory">积分明细</text>
      <view class="quick-divider" />
      <text class="quick-link" @click="goToRedemptions">兑换记录</text>
      <view class="quick-divider" />
      <text class="quick-link" @click="goToRules">积分规则</text>
    </view>

    <!-- 分类 Tab -->
    <view class="mall-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="mall-tab"
        :class="{ active: activeType === tab.value }"
        @click="switchTab(tab.value)"
      >{{ tab.label }}</view>
    </view>

    <!-- 商品网格 -->
    <view v-if="productLoading && products.length === 0" class="page-state">正在加载积分商品…</view>
    <view v-else-if="productError && products.length === 0" class="page-state error">
      <text>{{ productError }}</text>
      <view class="retry-button" @click="loadProducts">重新加载</view>
    </view>
    <view v-else-if="shownProducts.length === 0" class="page-state">暂无可兑换商品</view>
    <view v-else class="products-grid">
      <view
        v-for="item in shownProducts"
        :key="item.id"
        class="product-card"
        @click="openDetail(item)"
      >
        <view class="product-image">
          <image v-if="item.image" :src="item.image" class="product-photo" mode="aspectFill" />
        </view>
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <view class="product-points">
            <text class="points-num">{{ getDiscountedPointsCost(item.pointsPrice, 1, userStore.userLevel) }}</text>
            <text class="points-unit">积分</text>
            <text v-if="redeemDiscount < 1" class="points-original">{{ item.pointsPrice }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 商品详情覆盖层 -->
    <view v-if="showDetail" class="detail-page">
      <view class="detail-nav">
        <text class="detail-back" @click="closeDetail">‹</text>
        <text class="detail-nav-title">商品详情</text>
      </view>

      <view class="detail-body">
        <view class="detail-image-area">
          <view class="detail-image">
            <image v-if="selectedProduct?.image" :src="selectedProduct.image" class="detail-photo" mode="aspectFill" />
          </view>
        </view>

        <view class="detail-head">
          <text class="detail-name">{{ selectedProduct?.name }}</text>
          <view class="detail-tags">
            <text class="tag">{{ memberTag }}</text>
            <text v-if="limitTag" class="tag tag--limit">{{ limitTag }}</text>
          </view>
        </view>

        <view class="detail-sections">
          <view v-for="s in detailSections" :key="s.label" class="detail-section">
            <text class="section-label">{{ s.label }}</text>
            <text class="section-value">{{ s.value }}</text>
          </view>
        </view>

        <view class="detail-cta-wrap">
          <view
            v-if="!isPhysical || !needStorePickup"
            class="quantity-row"
          >
            <text class="quantity-label">兑换数量</text>
            <view class="stepper">
              <view class="stepper-btn" :class="{ disabled: redeemQuantity <= 1 }" @click="decreaseQuantity">−</view>
              <text class="stepper-value">{{ redeemQuantity }}</text>
              <view class="stepper-btn" :class="{ disabled: !canIncreaseQuantity }" @click="increaseQuantity">＋</view>
            </view>
          </view>

          <view
            class="detail-cta"
            :class="{ disabled: !canRedeemSelected }"
            @click="confirmRedeem"
          >{{ ctaText }}</view>
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
const products = ref([])
const showDetail = ref(false)
const selectedProduct = ref(null)
const redeemQuantity = ref(1)
const productLoading = ref(false)
const redeeming = ref(false)
const productError = ref('')
const activeType = ref('all')

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'coupon', label: '优惠券' },
  { value: 'physical', label: '实物礼品' }
]

const redeemDiscount = computed(() => POINTS_REDEEM_DISCOUNTS[userStore.userLevel] || 1)
const isPhysical = computed(() => selectedProduct.value?.productType === 'PHYSICAL')
const needStorePickup = computed(() => selectedProduct.value?.fulfillmentType === 'PICKUP' || isPhysical.value)
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
const canRedeemSelected = computed(() => (
  Boolean(selectedProduct.value) &&
  redeemQuantity.value >= 1 &&
  redeemQuantity.value <= Math.max(1, selectedQuantityLimit.value) &&
  hasEnoughPoints.value &&
  !redeeming.value
))
const ctaText = computed(() => {
  if (redeeming.value) return '兑换中…'
  if (canRedeemSelected.value) return `立即兑换 · ${selectedTotalCost.value} 积分`
  const diff = selectedTotalCost.value - (userStore.memberInfo?.currentPoints || 0)
  return `积分不足 · 还差 ${Math.max(0, diff).toLocaleString()} 积分`
})

const shownProducts = computed(() => {
  if (activeType.value === 'all') return products.value
  return products.value.filter(p => p.category === activeType.value)
})

const memberTag = computed(() => selectedProduct.value?.memberLevel
  ? `${getMemberLevelName(selectedProduct.value.memberLevel)}及以上会员`
  : '全部会员')
const limitTag = computed(() => {
  const limit = selectedProduct.value?.monthlyLimit
  if (limit) return `限兑 ${limit} 件`
  if (selectedProduct.value?.stock != null && selectedProduct.value.stock >= 0) return `库存 ${selectedProduct.value.stock} 件`
  return ''
})

const detailSections = computed(() => {
  const p = selectedProduct.value || {}
  const sections = []
  sections.push({ label: '商品类型', value: p.productType === 'PHYSICAL' ? '实物周边 · 礼品' : '优惠券 · 虚拟权益' })
  if (p.validDays) sections.push({ label: '有效期限', value: `自获取之日起 ${p.validDays} 天内有效` })
  else if (p.productType === 'PHYSICAL') sections.push({ label: '有效期限', value: '兑换后 14 天内发货' })
  if (p.description) sections.push({ label: '商品说明', value: p.description })
  if (p.originalPrice) sections.push({ label: '参考价值', value: `¥${p.originalPrice}` })
  sections.push({
    label: '领取方式',
    value: p.productType === 'PHYSICAL' ? '到店自提 · 邮寄到家' : '兑换后自动发放至券包'
  })
  return sections
})

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
      image: item.imageUrl || item.image || '',
      description: item.description || '',
      category: item.category || (item.productType === 'PHYSICAL' ? 'physical' : 'coupon')
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

function switchTab(value) {
  if (activeType.value === value) return
  activeType.value = value
}

function openDetail(item) {
  selectedProduct.value = item
  redeemQuantity.value = 1
  showDetail.value = true
}

function closeDetail() {
  if (redeeming.value) return
  showDetail.value = false
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
  if (!canRedeemSelected.value) {
    uni.showToast({ title: '积分不足', icon: 'none' })
    return
  }
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
      showDetail.value = false
      selectedProduct.value = null
      redeemQuantity.value = 1
      uni.showToast({ title: '兑换成功', icon: 'success' })
      await loadMallData()
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '兑换失败', icon: 'none' })
  } finally {
    uni.hideLoading()
    redeeming.value = false
  }
}

function goToHistory() { uni.navigateTo({ url: '/pages/points/history' }) }
function goToRedemptions() { uni.navigateTo({ url: '/pages/points/redemptions' }) }
function goToRules() { uni.showToast({ title: '积分规则', icon: 'none' }) }

onShow(loadMallData)
</script>

<style lang="scss" scoped>
.mall-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding-bottom: 60rpx;
}

/* ── 积分头 ── */
.points-header {
  padding: 56rpx 48rpx 32rpx;
  background: $cozy-surface;
}
.points-label {
  display: block;
  font-size: 26rpx;
  color: $cozy-muted;
  letter-spacing: .04em;
}
.points-value {
  display: block;
  margin-top: 16rpx;
  font-family: $font-display;
  font-size: 92rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1;
}

/* ── 文字快捷链接 ── */
.quick-links {
  display: flex;
  align-items: center;
  padding: 32rpx 48rpx 44rpx;
  background: $cozy-surface;
}
.quick-link {
  flex: 1;
  text-align: center;
  font-size: 26rpx;
  color: $cozy-ink;
  letter-spacing: .04em;

  &:active { opacity: .6; }
}
.quick-divider {
  width: 1rpx;
  height: 26rpx;
  background: $cozy-border;
}

/* ── 分类 Tab ── */
.mall-tabs {
  display: flex;
  margin: 0 40rpx;
  padding: 0 8rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.mall-tab {
  flex: 1;
  padding: 28rpx 0;
  text-align: center;
  font-size: 30rpx;
  color: $cozy-muted;
  position: relative;
  transition: color $cozy-duration $cozy-ease-out;

  &.active {
    color: $cozy-ink;
    font-weight: 600;
  }
  &.active::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: -1rpx;
    transform: translateX(-50%);
    width: 48rpx;
    height: 4rpx;
    border-radius: 2rpx;
    background: $cozy-ink;
  }
}

/* ── 商品网格 ── */
.products-grid {
  margin: 32rpx 40rpx 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32rpx 28rpx;
  padding-bottom: 48rpx;
}
.product-card {
  &:active .product-image { transform: scale(.98); }
}
.product-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 16rpx;
  background: $cozy-bg;
  overflow: hidden;
  transition: transform $cozy-duration $cozy-ease-out;
}
.product-photo {
  width: 100%;
  height: 100%;
}
.product-info { padding: 24rpx 4rpx 0; }
.product-name {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: $font-display;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1.4;
}
.product-points {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
  margin-top: 14rpx;
}
.points-num { font-size: 34rpx; font-weight: 700; color: $cozy-primary; }
.points-unit { font-size: 22rpx; color: $cozy-muted; }
.points-original {
  margin-left: 10rpx;
  font-size: 20rpx;
  color: $cozy-placeholder;
  text-decoration: line-through;
}

.page-state { padding: 120rpx 40rpx; color: $cozy-muted; text-align: center; }
.page-state text { display: block; }
.page-state.error { color: $error-color; }
.retry-button {
  width: 220rpx;
  margin: 32rpx auto 0;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $cozy-radius-md;
  background: $cozy-primary;
  color: #fff;
  font-size: 24rpx;
}

/* ── 商品详情覆盖层 ── */
.detail-page {
  position: fixed;
  inset: 0;
  background: #fff;
  z-index: 50;
  display: flex;
  flex-direction: column;
  animation: detail-in .35s cubic-bezier(.32,.72,.32,1);
}
@keyframes detail-in {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
.detail-nav {
  flex: none;
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.detail-back {
  font-size: 48rpx;
  line-height: 1;
  color: $cozy-ink;
  padding: 4rpx 8rpx;
}
.detail-nav-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 30rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.detail-body { flex: 1; overflow-y: auto; }

.detail-image-area {
  padding: 48rpx 64rpx 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-image {
  width: 480rpx;
  height: 480rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: $cozy-surface;
}
.detail-photo { width: 100%; height: 100%; }

.detail-head {
  padding: 0 48rpx 44rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.detail-name {
  display: block;
  font-family: $font-display;
  font-size: 44rpx;
  font-weight: 600;
  color: $cozy-ink;
  line-height: 1.3;
}
.detail-tags {
  margin-top: 24rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.tag {
  display: inline-block;
  padding: 10rpx 22rpx;
  font-size: 22rpx;
  border: 1rpx solid $cozy-ink;
  color: $cozy-ink;
  border-radius: 4rpx;
  letter-spacing: .02em;
}
.tag--limit {
  border-color: #9B3932;
  color: #9B3932;
}

.detail-sections { padding: 0 48rpx; }
.detail-section {
  padding: 32rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
}
.section-label {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.section-value {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: $cozy-muted;
}

.detail-cta-wrap {
  padding: 40rpx 48rpx 48rpx;
  background: #fff;
  margin-top: 8rpx;
}
.quantity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}
.quantity-label { color: $cozy-muted; font-size: 28rpx; }
.stepper {
  display: flex;
  align-items: center;
  overflow: hidden;
  border: 1rpx solid $cozy-border;
  border-radius: 999rpx;
}
.stepper-btn {
  width: 80rpx;
  padding: 12rpx 0;
  color: $cozy-primary;
  font-size: 40rpx;
  text-align: center;

  &.disabled { color: $cozy-placeholder; }
}
.stepper-value {
  min-width: 64rpx;
  color: $cozy-ink;
  font-size: 30rpx;
  font-weight: 600;
  text-align: center;
}
.detail-cta {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $cozy-primary;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 48rpx;
  transition: opacity $cozy-duration $cozy-ease-out;

  &:active { opacity: .85; }
  &.disabled {
    background: $cozy-border;
    color: $cozy-muted;
  }
}
</style>
