<!--
  兑换确认订单页 - 参考咖啡订单确认页：领取方式+门店/地址 → 商品明细 → 积分汇总 → 底部确认
  数据: 详情页 storage(cozy_mall_confirm) 传入商品与数量；实物可选到店自提/快递配送
-->
<template>
  <view class="redeem-confirm-page">
    <!-- 顶部筛选（原生导航之下，对齐订单页自提/外送筛选） -->
    <FilterTabs
      v-if="product && isPhysical"
      :options="fulfillOptions"
      v-model="fulfillType"
    />

    <view v-if="product" class="confirm-content">
      <!-- 门店 / 配送地址（对齐咖啡确认页 StoreSummary） -->
      <template v-if="isPhysical">
        <StoreSummary v-if="fulfillType === 'pickup'" mode="pickup" />
        <StoreSummary
          v-else
          mode="delivery"
          :delivery-address="selectedAddress"
          :delivery-eta="'兑换后 14 天内发货'"
          @tap="addressSheetVisible = true"
        />
      </template>

      <!-- 商品明细 -->
      <view class="section-block">
        <view class="section-heading">
          <text class="section-title">商品明细</text>
        </view>
        <view class="redeem-line">
          <view class="line-thumb"><image v-if="product.image" :src="product.image" class="line-photo" mode="aspectFill" /></view>
          <view class="line-content">
            <text class="line-name">{{ product.name }}</text>
            <text class="line-spec">{{ productTypeText }}</text>
          </view>
          <text class="line-points">{{ totalCost.toLocaleString() }} 积分</text>
        </view>

        <view class="quantity-row">
          <text class="quantity-label">兑换数量</text>
          <Stepper v-model="quantity" :max="selectedQuantityLimit" :can-increase="canIncrease" />
        </view>

        <view v-if="!isPhysical" class="redeem-tip">兑换后自动发放至券包，可在「我的 · 卡券」查看</view>
      </view>

      <!-- 积分汇总 -->
      <view class="section-block">
        <view class="summary-row"><text>消耗积分</text><text class="summary-strong">{{ totalCost.toLocaleString() }} 积分</text></view>
        <view class="summary-row"><text>会员折扣</text><text>{{ redeemDiscount < 1 ? `已享 ${Math.round(redeemDiscount * 100)} 折` : '—' }}</text></view>
        <view class="summary-row"><text>兑换后剩余</text><text>{{ remaining.toLocaleString() }} 积分</text></view>
      </view>

      <view class="bottom-spacer" />
    </view>

    <view v-else class="empty-hint">
      <text>未获取到兑换商品，请返回重试</text>
      <view class="empty-btn" @click="goBack">返回商城</view>
    </view>

    <!-- 底部提交栏 -->
    <view v-if="product" class="submit-bar">
      <view class="submit-info">
        <text class="submit-cost">{{ totalCost.toLocaleString() }} 积分</text>
        <text class="submit-hint">兑换后剩余 {{ remaining.toLocaleString() }} 积分</text>
      </view>
      <view class="submit-btn" :class="{ disabled: !canSubmit }" @click="submitRedeem">{{ submitting ? '兑换中…' : '确认兑换' }}</view>
    </view>

    <AddressPickerSheet
      :visible="addressSheetVisible"
      :selected-id="selectedAddress?.id"
      @close="addressSheetVisible = false"
      @select="onAddressPicked"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getMemberInfo, redeemPoints } from '@/api/member'
import {
  getDiscountedPointsCost,
  getRedeemQuantityLimit
} from '@/domain/member/memberRules'
import { FIXED_STORE } from '@/config/store'
import AddressPickerSheet from '@/components/address/AddressPickerSheet.vue'
import StoreSummary from '@/components/order/StoreSummary.vue'
import FilterTabs from '@/components/common/FilterTabs.vue'
import Stepper from '@/components/common/Stepper.vue'
import { isPhysicalProduct, isVirtualProduct } from '@/utils/product'

const userStore = useUserStore()
const product = ref(null)
const quantity = ref(1)
const fulfillType = ref('pickup')
const fulfillOptions = [
  { value: 'pickup', label: '到店自提' },
  { value: 'delivery', label: '快递配送' }
]
const submitting = ref(false)
const addressSheetVisible = ref(false)
const selectedAddress = ref(null)

onLoad(() => {
  const data = uni.getStorageSync('cozy_mall_confirm')
  uni.removeStorageSync('cozy_mall_confirm')
  product.value = data?.product || null
  quantity.value = data?.quantity || 1
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

const isPhysical = computed(() => isPhysicalProduct(product.value))
const redeemDiscount = computed(() => Number(userStore.memberInfo?.redeemDiscount) || 1)
const selectedQuantityLimit = computed(() => getRedeemQuantityLimit(product.value || {}))
const totalCost = computed(() => getDiscountedPointsCost(product.value?.pointsPrice, quantity.value, redeemDiscount.value))
const remaining = computed(() => Math.max(0, (userStore.memberInfo?.currentPoints || 0) - totalCost.value))
const hasEnoughPoints = computed(() => (userStore.memberInfo?.currentPoints || 0) >= totalCost.value)
const canIncrease = computed(() => {
  if (!product.value || quantity.value >= selectedQuantityLimit.value) return false
  const next = getDiscountedPointsCost(product.value.pointsPrice, quantity.value + 1, redeemDiscount.value)
  return (userStore.memberInfo?.currentPoints || 0) >= next
})
const canSubmit = computed(() => {
  if (!product.value || !hasEnoughPoints.value || submitting.value) return false
  if (isPhysical.value && fulfillType.value === 'delivery') return Boolean(selectedAddress.value)
  return true
})
const productTypeText = computed(() => isPhysicalProduct(product.value) ? '实物周边 · 礼品' : '优惠券 · 虚拟权益')

function onAddressPicked(address) {
  if (!address) return
  selectedAddress.value = address
  addressSheetVisible.value = false
}

async function submitRedeem() {
  if (!canSubmit.value) return
  const payload = {
    productId: product.value.id,
    quantity: quantity.value,
    fulfillmentType: isVirtualProduct(product.value) ? 'VIRTUAL' : fulfillType.value === 'delivery' ? 'DELIVERY' : 'PICKUP'
  }
  if (payload.fulfillmentType === 'PICKUP') {
    payload.storeId = FIXED_STORE.id
  } else if (payload.fulfillmentType === 'DELIVERY') {
    if (selectedAddress.value) payload.addressId = selectedAddress.value.id
  }
  submitting.value = true
  uni.showLoading({ title: '兑换中…', mask: true })
  try {
    const response = await redeemPoints(payload)
    if (response.code === 200) {
      uni.showToast({ title: '兑换成功', icon: 'success' })
      const orderId = response.data?.id
      setTimeout(() => {
        uni.redirectTo({ url: `/pages/order/detail?id=${encodeURIComponent(orderId || '')}&type=redeem` })
      }, 600)
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '兑换失败', icon: 'none' })
  } finally {
    uni.hideLoading()
    submitting.value = false
  }
}

function goBack() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
.redeem-confirm-page { min-height: 100vh; background: $cozy-surface; }
.confirm-content { padding: 32rpx 32rpx 0; }

.section-block { margin-top: 24rpx; padding: 32rpx; border-radius: 28rpx; background: #fff; }
.section-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.section-title { color: $cozy-ink; font-family: $font-display; font-size: 30rpx; font-weight: 600; }

/* ── 商品明细 ── */
.redeem-line { display: flex; align-items: center; gap: 24rpx; padding: 20rpx 0; }
.line-thumb {
  flex: none;
  width: 116rpx;
  height: 116rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #E8DDD2, #D8C8B4);
  overflow: hidden;
}
.line-photo { width: 100%; height: 100%; }
.line-content { flex: 1; min-width: 0; }
.line-name { display: block; overflow: hidden; color: $cozy-ink; font-size: 28rpx; font-weight: 650; white-space: nowrap; text-overflow: ellipsis; }
.line-spec { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 22rpx; }
.line-points { flex: none; color: $cozy-primary; font-size: 28rpx; font-weight: 700; }
.quantity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 24rpx;
  border-top: 1rpx solid $cozy-border;
}
.quantity-label { color: $cozy-muted; font-size: 26rpx; }
.redeem-tip {
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
  border-radius: 12rpx;
  background: $cozy-surface;
  font-size: 24rpx;
  line-height: 1.6;
  color: $cozy-muted;
}

/* ── 积分汇总 ── */
.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  min-height: 56rpx;
  font-size: 25rpx;
  color: $cozy-muted;

  &:first-child { margin-top: 0; }
}
.summary-strong { color: $cozy-primary; font-size: 30rpx; font-weight: 700; }

/* ── 底部提交栏 ── */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 20rpx 32rpx max(20rpx, env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid $cozy-border;
}
.submit-info { flex: 1; min-width: 0; }
.submit-cost { display: block; color: $cozy-primary; font-family: $font-display; font-size: 34rpx; font-weight: 700; }
.submit-hint { display: block; margin-top: 4rpx; color: $cozy-muted; font-size: 22rpx; }
.submit-btn {
  flex: none;
  min-width: 240rpx;
  height: 92rpx;
  padding: 0 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 29rpx;
  font-weight: 650;

  &:active { opacity: .85; }
  &.disabled { background: $cozy-border; color: $cozy-muted; }
}
.bottom-spacer { height: 180rpx; }

.empty-hint {
  padding: 200rpx 60rpx;
  text-align: center;
  color: $cozy-muted;
  font-size: 26rpx;
}
.empty-btn {
  width: 260rpx;
  margin: 40rpx auto 0;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 26rpx;
}

</style>
