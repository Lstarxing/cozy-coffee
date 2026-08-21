<template>
  <view class="confirm-page">
    <view v-if="cartStore.items.length || checkoutSubmitting" class="confirm-content">
      <StoreSummary
        :mode="diningMethod === 'DELIVERY' ? 'delivery' : 'pickup'"
        :delivery-address="checkoutStore.deliveryAddress"
        :delivery-eta="deliveryEtaText"
        @tap="onStoreTap"
      />

      <view class="section-block">
        <view class="section-heading">
          <text class="section-title">商品明细</text>
          <text class="section-note">{{ cartStore.totalCount }} 件</text>
        </view>
        <view v-for="line in cartStore.items" :key="line.lineKey" class="checkout-line">
          <image class="line-image" :src="line.image" mode="aspectFill" />
          <view class="line-content">
            <text class="line-name">{{ line.name }}</text>
            <text class="line-spec">{{ formatCoffeeSpec(line) }}</text>
            <text class="line-quantity">× {{ line.quantity }}</text>
          </view>
          <text class="line-price">¥{{ lineAmount(line) }}</text>
        </view>

        <OfflineState v-if="checkoutStore.status === 'offline'" @retry="recoverPreview" />
        <RetryState v-else-if="previewError" title="金额核对失败" :description="previewError" @retry="loadPreview" />
        <view v-else>
          <LoadingState v-if="checkoutStore.status === 'previewing' && !checkoutStore.latestPreview" text="正在核对商品与优惠…" />
          <CheckoutPriceSummary
            v-else
            :preview="checkoutStore.latestPreview"
          />
        </view>
      </view>

      <!-- 优惠券（独立一块） -->
      <view class="coupon-block" @click="goToCoupon">
        <text class="coupon-label">优惠券</text>
        <view class="coupon-value-wrap">
          <text class="coupon-value" :class="{ accent: Boolean(selectedCoupon) }">{{ couponDisplayText }}</text>
          <text class="coupon-chevron">›</text>
        </view>
      </view>

      <!-- 订单备注 + 预留电话（底部） -->
      <view class="section-block form-block bottom-fields">
        <view class="form-row" @click="goToRemark">
          <text class="form-label">订单备注</text>
          <view class="form-value-wrap">
            <text class="form-value" :class="{ filled: checkoutStore.remark }">{{ checkoutStore.remark || '请在这里写您的备注' }}</text>
            <text class="chevron">›</text>
          </view>
        </view>
        <view class="form-divider" />
        <view class="form-row" @click="openPhone">
          <text class="form-label">预留电话</text>
          <view class="form-value-wrap">
            <text class="form-value" :class="{ filled: checkoutStore.phone }">{{ checkoutStore.phone || '选填' }}</text>
            <CozyIcon name="pencil" :size="20" color="#756A63" />
          </view>
        </view>
      </view>

      <!-- 模拟支付提示（最底部） -->
      <view class="mock-tip">
        <text class="mock-tip-title">开发阶段使用模拟支付</text>
        <text class="mock-tip-copy">不会产生真实扣款；确认成功后将创建一张真实测试订单。</text>
      </view>
      <view class="bottom-spacer" />
    </view>

    <EmptyState v-else title="购物车还是空的" description="先选择一杯喜欢的咖啡，再来结算" action-text="去点单" @action="goToMenu" />

    <CheckoutSubmitBar
      v-if="cartStore.items.length || checkoutSubmitting"
      :amount="checkoutStore.latestPreview?.payable || cartStore.subtotal"
      :status="checkoutStore.status"
      :disabled="submitDisabled"
      @submit="submitOrder"
    />

    <!-- 预留电话编辑 -->
    <view v-if="phoneVisible" class="phone-layer">
      <view class="phone-mask" @click="phoneVisible = false" />
      <view class="phone-sheet safe-area-bottom">
        <view class="phone-header">
          <text class="phone-title">预留电话</text>
          <view class="phone-close" @click="phoneVisible = false">×</view>
        </view>
        <input
          v-model="phoneDraft"
          type="number"
          maxlength="11"
          class="phone-input"
          placeholder="请输入联系电话"
          placeholder-class="phone-placeholder"
          focus
        />
        <view class="phone-save" :class="{ disabled: !phoneDraft.trim() }" @click="savePhone">保存</view>
      </view>
    </view>

    <AddressPickerSheet
      :visible="addressPickerVisible"
      :selected-id="checkoutStore.deliveryAddressId"
      @close="addressPickerVisible = false"
      @select="onAddressPicked"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { useUserStore } from '@/stores/user'
import { createDefaultCheckoutWorkflow } from '@/services/checkout/CheckoutWorkflow'
import { resolveDeliveryAddress } from '@/services/address/DeliveryAddressResolver'
import { formatCoffeeSpec } from '@/utils/spec'
import { AuthError, NetworkError } from '@/services/errors/AppError'
import StoreSummary from '@/components/order/StoreSummary.vue'
import AddressPickerSheet from '@/components/address/AddressPickerSheet.vue'
import CheckoutPriceSummary from '@/components/order/CheckoutPriceSummary.vue'
import CheckoutSubmitBar from '@/components/order/CheckoutSubmitBar.vue'
import LoadingState from '@/components/states/LoadingState.vue'
import EmptyState from '@/components/states/EmptyState.vue'
import RetryState from '@/components/states/RetryState.vue'
import OfflineState from '@/components/states/OfflineState.vue'
import CozyIcon from '@/components/CozyIcon.vue'

const cartStore = useCartStore()
const checkoutStore = useCheckoutStore()
const userStore = useUserStore()
const workflow = createDefaultCheckoutWorkflow()
const selectedCoupon = ref(null)
const phoneVisible = ref(false)
const phoneDraft = ref('')
const diningMethod = computed(() => checkoutStore.diningMethod || 'TAKEOUT')
const previewError = ref('')
const checkoutSubmitting = ref(false)
const addressPickerVisible = ref(false)

// 预计送达：当前本地时间 + 50~60 分钟区间
const deliveryEtaText = computed(() => {
  const pad = n => String(n).padStart(2, '0')
  const start = new Date(Date.now() + 50 * 60 * 1000)
  const end = new Date(Date.now() + 60 * 60 * 1000)
  return `现在下单，预计 ${pad(start.getHours())}:${pad(start.getMinutes())}-${pad(end.getHours())}:${pad(end.getMinutes())} 送达`
})

const couponDisplayText = computed(() => selectedCoupon.value ? couponTitle(selectedCoupon.value) : '选择优惠券')
const submitDisabled = computed(() => (
  checkoutStore.isBusy ||
  checkoutStore.status === 'offline' ||
  !checkoutStore.latestPreview ||
  !cartStore.items.length
))

onLoad(() => {
  checkoutStore.start()
  checkoutStore.pickupTime = 'ASAP'
  checkoutStore.storeId = 1
  uni.$on('addressSelected', handleAddressSelected)
  uni.$on('couponSelected', handleCouponSelected)
})

onUnload(() => {
  uni.$off('addressSelected', handleAddressSelected)
  uni.$off('couponSelected', handleCouponSelected)
})

function onStoreTap() {
  if (diningMethod.value === 'DELIVERY') {
    addressPickerVisible.value = true
    return
  }
  uni.navigateTo({ url: '/pages/store/list' })
}

function handleAddressSelected(address) {
  if (!address) return
  checkoutStore.deliveryAddress = address
  checkoutStore.deliveryAddressId = address.id || null
  if (address.phone && !checkoutStore.phone) checkoutStore.phone = address.phone
  checkoutStore.invalidatePreview()
  loadPreview()
}

function onAddressPicked(address) {
  addressPickerVisible.value = false
  handleAddressSelected(address)
}

onShow(async () => {
  if (!cartStore.items.length) return
  if (!checkoutStore.phone && userStore.userInfo?.phone) {
    checkoutStore.phone = userStore.userInfo.phone
  }
  // 外送：无地址时解析默认地址，仍无则卡片引导添加
  if (diningMethod.value === 'DELIVERY' && !checkoutStore.deliveryAddressId) {
    await resolveDeliveryAddress(checkoutStore)
  }
  await loadPreview()
})

async function loadPreview() {
  if (!cartStore.items.length || checkoutStore.isBusy) return
  previewError.value = ''
  try {
    await workflow.preview({ coupon: selectedCoupon.value })
  } catch (error) {
    if (error instanceof AuthError) {
      promptLogin()
      return
    }
    previewError.value = error?.message || '暂时无法核对订单金额'
  }
}

async function recoverPreview() {
  checkoutStore.invalidatePreview()
  await loadPreview()
}

function goToCoupon() {
  uni.setStorageSync('cozy_coupon_cart', JSON.stringify({
    items: cartStore.items,
    diningMethod: checkoutStore.diningMethod || 'TAKEOUT'
  }))
  uni.navigateTo({ url: '/pages/coupon/list?select=1' })
}

function handleCouponSelected(coupon) {
  selectedCoupon.value = coupon
  checkoutStore.selectedCouponId = coupon ? couponKey(coupon) : null
  checkoutStore.invalidatePreview()
  loadPreview()
}

function goToRemark() {
  uni.navigateTo({ url: '/pages/order/remark' })
}

function openPhone() {
  phoneDraft.value = checkoutStore.phone || ''
  phoneVisible.value = true
}

function savePhone() {
  const phone = phoneDraft.value.trim()
  if (phone && !/^1\d{10}$/.test(phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  checkoutStore.phone = phone
  phoneVisible.value = false
}

async function submitOrder() {
  if (submitDisabled.value) return
  if (diningMethod.value === 'DELIVERY' && !checkoutStore.deliveryAddressId) {
    uni.showToast({ title: '请先选择配送地址', icon: 'none' })
    return
  }
  // 结算中锁定内容区：购物车清空后、跳转详情前不闪现空状态
  checkoutSubmitting.value = true
  previewError.value = ''
  try {
    const result = ['awaiting_auth', 'offline', 'failed', 'cancelled'].includes(checkoutStore.status)
      ? await workflow.recover({ coupon: selectedCoupon.value })
      : await workflow.submit({ coupon: selectedCoupon.value })

    if (result.status === 'cancelled') {
      const orderId = result.order?.id || result.order?.orderId
      if (result.order) uni.setStorageSync('cozy_order_detail', result.order)
      uni.redirectTo({ url: `/pages/order/detail?id=${encodeURIComponent(orderId || '')}`, animationType: 'none' })
      return
    }

    const orderId = result.order?.id || result.order?.orderId
    if (result.order) uni.setStorageSync('cozy_order_detail', result.order)
    uni.redirectTo({
      url: `/pages/order/detail?id=${encodeURIComponent(orderId || '')}`,
      animationType: 'none',
      success: () => uni.showToast({ title: '下单成功', icon: 'success' })
    })
  } catch (error) {
    checkoutSubmitting.value = false
    if (error instanceof AuthError) {
      promptLogin()
      return
    }
    if (error instanceof NetworkError) {
      uni.showToast({ title: '网络不可用，购物车已保留', icon: 'none' })
      return
    }
    previewError.value = error?.message || '提交失败，请重试'
    uni.showToast({ title: previewError.value, icon: 'none' })
  }
}

function promptLogin() {
  uni.showModal({
    title: '结算前需要登录',
    content: '登录后会保留购物车，并可继续当前结算。',
    confirmText: '去登录',
    success: result => {
      if (result.confirm) uni.navigateTo({ url: '/pages/login/index?redirect=%2Fpages%2Forder%2Fconfirm' })
    }
  })
}

function couponKey(coupon) { return coupon?.couponCode || coupon?.code || coupon?.id || '' }
function couponTitle(coupon) { return coupon?.name || coupon?.couponName || coupon?.title || coupon?.typeName || '优惠券' }

function lineAmount(line) { return (Number(line.price || 0) * Number(line.quantity || 1)).toFixed(2) }
function goToMenu() { uni.switchTab({ url: '/pages/menu/menu' }) }
</script>

<style lang="scss" scoped>
.confirm-page { min-height: 100vh; background: $cozy-surface; }
.confirm-content { padding: 32rpx 32rpx 0; }

.section-block { margin-top: 24rpx; padding: 32rpx; border-radius: 28rpx; background: #fff; }

/* ── 优惠券（独立一块） ── */
.coupon-block {
  margin-top: 24rpx;
  padding: 32rpx;
  border-radius: 28rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;

  &:active { opacity: .85; }
}
.coupon-label { flex: none; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.coupon-value-wrap { min-width: 0; flex: 1; display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.coupon-value { overflow: hidden; color: $cozy-muted; font-size: 24rpx; white-space: nowrap; text-overflow: ellipsis; }
.coupon-value.accent { color: $cozy-primary; }
.coupon-chevron { color: $cozy-placeholder; font-size: 42rpx; font-weight: 300; }
.section-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.section-title { color: $cozy-ink; font-family: $font-display; font-size: 30rpx; font-weight: 600; }
.section-note { color: $cozy-muted; font-size: 22rpx; }
.pickup-option { min-height: 96rpx; display: flex; align-items: center; gap: 18rpx; }
.pickup-radio { width: 38rpx; height: 38rpx; display: flex; align-items: center; justify-content: center; border: 2rpx solid $cozy-primary; border-radius: 50%; }
.pickup-radio-dot { width: 20rpx; height: 20rpx; border-radius: 50%; background: $cozy-primary; }
.pickup-title { display: block; color: $cozy-ink; font-size: 28rpx; font-weight: 650; }
.pickup-description { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 22rpx; }
.checkout-line { display: flex; align-items: center; gap: 24rpx; padding: 20rpx 0; }
.line-image { width: 116rpx; height: 116rpx; flex: none; border-radius: 16rpx; background: linear-gradient(135deg, #E8DDD2, #D8C8B4); }
.line-content { min-width: 0; flex: 1; }
.line-name { display: block; overflow: hidden; color: $cozy-ink; font-size: 28rpx; font-weight: 650; white-space: nowrap; text-overflow: ellipsis; }
.line-spec, .line-quantity { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 22rpx; }
.line-price { flex: none; color: $cozy-ink; font-size: 28rpx; font-weight: 700; }
.form-row { min-height: 88rpx; display: flex; align-items: center; justify-content: space-between; gap: 24rpx; }
.form-label { flex: none; color: $cozy-ink; font-size: 27rpx; font-weight: 650; }
.form-value-wrap { min-width: 0; flex: 1; display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.form-value { overflow: hidden; color: $cozy-placeholder; font-size: 24rpx; white-space: nowrap; text-overflow: ellipsis; }
.form-value.filled { color: $cozy-ink; }
.form-value.accent { color: $cozy-primary; }
.chevron { color: $cozy-placeholder; font-size: 42rpx; font-weight: 300; }
.form-divider { height: 1rpx; background: $cozy-border; }
.mock-tip { margin-top: 24rpx; padding: 24rpx 28rpx; border-radius: 16rpx; background: $cozy-surface; }
.mock-tip-title { display: block; color: $cozy-muted; font-size: 25rpx; font-weight: 700; }
.mock-tip-copy { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 21rpx; line-height: 1.5; }
.bottom-spacer { height: 180rpx; }

/* ── 预留电话编辑 sheet ── */
.phone-layer { position: fixed; inset: 0; z-index: 120; }
.phone-mask { position: absolute; inset: 0; background: rgba(25, 18, 14, .46); }
.phone-sheet { position: absolute; left: 0; right: 0; bottom: 0; padding: 24rpx 32rpx max(24rpx, env(safe-area-inset-bottom)); border-radius: 32rpx 32rpx 0 0; background: #fff; }
.phone-header { display: flex; align-items: center; justify-content: space-between; padding: 4rpx 4rpx 20rpx; }
.phone-title { color: $cozy-ink; font-family: $font-display; font-size: 32rpx; font-weight: 600; }
.phone-close { width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; color: $cozy-muted; font-size: 44rpx; }
.phone-input { height: 92rpx; padding: 0 28rpx; border-radius: $cozy-radius-md; background: $cozy-surface; color: $cozy-ink; font-size: 30rpx; }
.phone-placeholder { color: $cozy-placeholder; }
.phone-save { margin-top: 24rpx; height: 92rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-ink; color: #fff; font-size: 30rpx; font-weight: 600; }
.phone-save.disabled { opacity: .4; }
</style>
