<!--
  积分商品详情页 - 独立页面（原生导航栏"商品详情"）
  底部「去兑换」跳转兑换确认订单页（领取方式/门店/地址在确认页配置）
-->
<template>
  <view class="detail-page">
    <view class="detail-body">
      <view class="detail-image-area">
        <view class="detail-image">
          <image v-if="product?.image" :src="product.image" class="detail-photo" mode="aspectFill" />
        </view>
      </view>

      <view class="detail-head">
        <text class="detail-name">{{ product?.name }}</text>
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
        <view v-if="!isPhysical || !needStorePickup" class="quantity-row">
          <text class="quantity-label">兑换数量</text>
          <Stepper v-model="redeemQuantity" :max="selectedQuantityLimit" :can-increase="canIncreaseQuantity" />
        </view>
        <view
          class="detail-cta"
          :class="{ disabled: !canRedeemSelected }"
          @click="goToConfirm"
        >{{ ctaText }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getMemberInfo } from '@/api/member'
import { getMemberLevelName } from '@/constants/member'
import Stepper from '@/components/common/Stepper.vue'
import { isPhysicalProduct } from '@/utils/product'
import {
  POINTS_REDEEM_DISCOUNTS,
  getDiscountedPointsCost,
  getRedeemQuantityLimit
} from '@/domain/member/memberRules'

const userStore = useUserStore()
const product = ref(null)
const redeemQuantity = ref(1)

onLoad(() => {
  const data = uni.getStorageSync('cozy_mall_product')
  uni.removeStorageSync('cozy_mall_product')
  product.value = data || null
  if (!product.value) {
    uni.showToast({ title: '商品不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
    return
  }
  loadMember()
})

onShow(() => {
  if (product.value) loadMember()
})

async function loadMember() {
  try {
    const response = await getMemberInfo()
    if (response.code === 200 && response.data) userStore.setMemberInfo(response.data)
  } catch (_) { /* 静默 */ }
}

const redeemDiscount = computed(() => POINTS_REDEEM_DISCOUNTS[userStore.userLevel] || 1)
const isPhysical = computed(() => isPhysicalProduct(product.value))
const needStorePickup = computed(() => product.value?.fulfillmentType === 'PICKUP' || isPhysical.value)
const selectedQuantityLimit = computed(() => getRedeemQuantityLimit(product.value || {}))
const selectedTotalCost = computed(() => getDiscountedPointsCost(
  product.value?.pointsPrice,
  redeemQuantity.value,
  userStore.userLevel
))
const hasEnoughPoints = computed(() => (userStore.memberInfo?.currentPoints || 0) >= selectedTotalCost.value)
const canIncreaseQuantity = computed(() => {
  if (!product.value || redeemQuantity.value >= selectedQuantityLimit.value) return false
  const nextCost = getDiscountedPointsCost(product.value.pointsPrice, redeemQuantity.value + 1, userStore.userLevel)
  return (userStore.memberInfo?.currentPoints || 0) >= nextCost
})
const canRedeemSelected = computed(() => (
  Boolean(product.value) &&
  redeemQuantity.value >= 1 &&
  redeemQuantity.value <= Math.max(1, selectedQuantityLimit.value) &&
  hasEnoughPoints.value
))
const ctaText = computed(() => {
  if (canRedeemSelected.value) return '去兑换'
  const diff = selectedTotalCost.value - (userStore.memberInfo?.currentPoints || 0)
  return `积分不足 · 还差 ${Math.max(0, diff).toLocaleString()} 积分`
})

const memberTag = computed(() => product.value?.memberLevel
  ? `${getMemberLevelName(product.value.memberLevel)}及以上会员`
  : '全部会员')
const limitTag = computed(() => {
  const limit = product.value?.monthlyLimit
  if (limit) return `限兑 ${limit} 件`
  if (product.value?.stock != null && product.value.stock >= 0) return `库存 ${product.value.stock} 件`
  return ''
})

const detailSections = computed(() => {
  const p = product.value || {}
  const sections = []
  sections.push({ label: '商品类型', value: isPhysicalProduct(p) ? '实物周边 · 礼品' : '优惠券 · 虚拟权益' })
  if (p.validDays) sections.push({ label: '有效期限', value: `自获取之日起 ${p.validDays} 天内有效` })
  else if (isPhysicalProduct(p)) sections.push({ label: '有效期限', value: '兑换后 14 天内发货' })
  if (p.description) sections.push({ label: '商品说明', value: p.description })
  if (p.originalPrice) sections.push({ label: '参考价值', value: `¥${p.originalPrice}` })
  sections.push({
    label: '领取方式',
    value: isPhysicalProduct(p) ? '到店自提 · 邮寄到家' : '兑换后自动发放至券包'
  })
  return sections
})

function goToConfirm() {
  if (!canRedeemSelected.value) {
    uni.showToast({ title: '积分不足', icon: 'none' })
    return
  }
  uni.setStorageSync('cozy_mall_confirm', { product: product.value, quantity: redeemQuantity.value })
  uni.navigateTo({ url: '/pages/mall/confirm' })
}
</script>

<style lang="scss" scoped>
.detail-page { min-height: 100vh; background: #fff; }

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
.detail-cta {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $cozy-ink;
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
