<template>
  <view class="confirm-page">
    <view v-if="cartStore.items.length" class="confirm-content">
      <!-- 自提/外送切换 -->
      <view class="fulfillment-switch">
        <view class="fulfillment-opt" :class="{ active: diningMethod === 'TAKEOUT' }" @click="setDining('TAKEOUT')">自提</view>
        <view class="fulfillment-opt" :class="{ active: diningMethod === 'DELIVERY' }" @click="setDining('DELIVERY')">外送</view>
      </view>

      <StoreSummary
        :mode="diningMethod === 'DELIVERY' ? 'delivery' : 'pickup'"
        :delivery-address="deliveryAddressText"
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
            <text class="line-spec">{{ formatSpec(line) }}</text>
            <text class="line-quantity">× {{ line.quantity }}</text>
          </view>
          <text class="line-price">¥{{ lineAmount(line) }}</text>
        </view>
      </view>

      <OfflineState v-if="checkoutStore.status === 'offline'" @retry="recoverPreview" />
      <RetryState v-else-if="previewError" title="金额核对失败" :description="previewError" @retry="loadPreview" />
      <view v-else>
        <LoadingState v-if="checkoutStore.status === 'previewing' && !checkoutStore.latestPreview" text="正在核对商品与优惠…" />
        <CheckoutPriceSummary
          v-else
          :preview="checkoutStore.latestPreview"
          :coupon-text="couponDisplayText"
          :coupon-selected="Boolean(selectedCoupon)"
          @coupon-click="couponVisible = true"
        />
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
      v-if="cartStore.items.length"
      :amount="checkoutStore.latestPreview?.payable || cartStore.subtotal"
      :status="checkoutStore.status"
      :disabled="submitDisabled"
      @submit="submitOrder"
    />

    <view v-if="couponVisible" class="coupon-layer">
      <view class="coupon-mask" @click="couponVisible = false" />
      <view class="coupon-sheet safe-area-bottom">
        <view class="coupon-header">
          <text class="coupon-title">选择优惠券</text>
          <view class="coupon-close" @click="couponVisible = false">×</view>
        </view>
        <scroll-view scroll-y class="coupon-scroll">
          <view class="coupon-option" :class="{ selected: !selectedCoupon }" @click="selectCoupon(null)">
            <view><text class="coupon-name">不使用优惠券</text><text class="coupon-description">按商品原价结算</text></view>
            <text v-if="!selectedCoupon" class="coupon-check">✓</text>
          </view>
          <view v-for="coupon in coupons" :key="couponKey(coupon)" class="coupon-option" :class="{ selected: couponKey(selectedCoupon) === couponKey(coupon) }" @click="selectCoupon(coupon)">
            <view><text class="coupon-name">{{ couponTitle(coupon) }}</text><text class="coupon-description">{{ couponDescription(coupon) }}</text></view>
            <text v-if="couponKey(selectedCoupon) === couponKey(coupon)" class="coupon-check">✓</text>
          </view>
          <EmptyState v-if="!coupons.length" icon="券" title="暂无可用优惠券" description="本单将按商品原价结算" />
        </scroll-view>
      </view>
    </view>

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
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getCouponList } from '@/api/coupon'
import { useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { useUserStore } from '@/stores/user'
import { createDefaultCheckoutWorkflow } from '@/services/checkout/CheckoutWorkflow'
import { AuthError, NetworkError } from '@/services/errors/AppError'
import StoreSummary from '@/components/order/StoreSummary.vue'
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
const coupons = ref([])
const selectedCoupon = ref(null)
const couponVisible = ref(false)
const phoneVisible = ref(false)
const phoneDraft = ref('')
const diningMethod = ref(checkoutStore.diningMethod || 'TAKEOUT')
const deliveryAddress = ref(null)
const previewError = ref('')
let loaded = false

const deliveryAddressText = computed(() => {
  const a = deliveryAddress.value
  if (!a) return ''
  return [a.region, a.detail].filter(Boolean).join(' ')
})

const couponHint = computed(() => coupons.value.length ? `${coupons.value.length} 张可用` : '暂无可用')
const couponDisplayText = computed(() => selectedCoupon.value ? couponTitle(selectedCoupon.value) : couponHint.value)
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
  diningMethod.value = checkoutStore.diningMethod || 'TAKEOUT'
  uni.$on('addressSelected', handleAddressSelected)
})

onUnload(() => {
  uni.$off('addressSelected', handleAddressSelected)
})

function setDining(value) {
  if (diningMethod.value === value) return
  diningMethod.value = value
  checkoutStore.diningMethod = value
  checkoutStore.invalidatePreview()
  loadPreview()
}

function onStoreTap() {
  if (diningMethod.value === 'DELIVERY') {
    uni.navigateTo({ url: '/pages/address/list' })
    return
  }
  uni.navigateTo({ url: '/pages/store/list' })
}

function handleAddressSelected(address) {
  if (!address) return
  deliveryAddress.value = address
  checkoutStore.deliveryAddressId = address.id || null
  if (address.phone && !checkoutStore.phone) checkoutStore.phone = address.phone
  checkoutStore.invalidatePreview()
  loadPreview()
}

onShow(async () => {
  if (!cartStore.items.length) return
  if (!loaded) {
    loaded = true
    await loadCoupons()
  }
  if (!checkoutStore.phone && userStore.userInfo?.phone) {
    checkoutStore.phone = userStore.userInfo.phone
  }
  await loadPreview()
})

async function loadCoupons() {
  try {
    const response = await getCouponList('available')
    const source = response?.data ?? response
    coupons.value = (Array.isArray(source) ? source : []).filter(coupon => {
      const status = String(coupon.status || '').toUpperCase()
      return !status || ['AVAILABLE', 'ISSUED'].includes(status)
    })
  } catch (error) {
    if (!(error instanceof AuthError)) console.warn('加载优惠券失败', error)
    coupons.value = []
  }
}

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

function selectCoupon(coupon) {
  selectedCoupon.value = coupon
  checkoutStore.selectedCouponId = coupon ? couponKey(coupon) : null
  checkoutStore.invalidatePreview()
  couponVisible.value = false
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
  previewError.value = ''
  try {
    const result = ['awaiting_auth', 'offline', 'failed', 'cancelled'].includes(checkoutStore.status)
      ? await workflow.recover({ coupon: selectedCoupon.value })
      : await workflow.submit({ coupon: selectedCoupon.value })

    if (result.status === 'cancelled') {
      const orderId = result.order?.id || result.order?.orderId
      uni.redirectTo({ url: `/pages/order/detail?id=${encodeURIComponent(orderId || '')}` })
      return
    }

    const orderId = result.order?.id || result.order?.orderId
    uni.redirectTo({
      url: `/pages/order/detail?id=${encodeURIComponent(orderId || '')}`,
      success: () => uni.showToast({ title: '下单成功', icon: 'success' })
    })
  } catch (error) {
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
function couponDescription(coupon) {
  if (coupon?.description) return coupon.description
  if (coupon?.discountAmount) return `可减 ¥${Number(coupon.discountAmount).toFixed(2)}`
  if (coupon?.minAmount) return `满 ¥${Number(coupon.minAmount).toFixed(0)} 可用`
  return '具体优惠以结算试算为准'
}

const optionLabels = {
  STANDARD: '标准杯', MEDIUM: '中杯', LARGE: '大杯', SMALL: '小杯',
  HOT: '热', COLD: '冰', WARM: '温', LESS: '少糖', HALF: '半糖', NONE: '无糖',
  WHOLE: '全脂奶', OAT: '燕麦奶', COCONUT: '椰奶', SOY: '豆奶', NORMAL: '标准浓度', STRONG: '加浓'
}
function formatSpec(line) {
  return [line.cupSize, line.temperature, line.sugarLevel, line.milkType, line.coffeeStrength]
    .filter(value => value && !['WHOLE', 'NORMAL'].includes(value))
    .map(value => optionLabels[value] || value)
    .join(' · ') || '默认规格'
}
function lineAmount(line) { return (Number(line.price || 0) * Number(line.quantity || 1)).toFixed(2) }
function goToMenu() { uni.switchTab({ url: '/pages/menu/menu' }) }
</script>

<style lang="scss" scoped>
.confirm-page { min-height: 100vh; background: $cozy-surface; }
.confirm-content { padding: 32rpx 32rpx 0; }

/* ── 自提/外送切换 ── */
.fulfillment-switch {
  display: flex;
  padding: 8rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 999rpx;
  background: $bg-white;
  margin-bottom: 24rpx;
}
.fulfillment-opt {
  flex: 1;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  font-size: 26rpx;
  color: $cozy-muted;

  &.active {
    background: $cozy-ink;
    color: #fff;
    font-weight: 600;
  }
}

.section-block { margin-top: 24rpx; padding: 32rpx; border-radius: 28rpx; background: #fff; }
.section-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.section-title { color: $cozy-ink; font-family: $font-display; font-size: 30rpx; font-weight: 600; }
.section-note { color: $cozy-muted; font-size: 22rpx; }
.pickup-option { min-height: 96rpx; display: flex; align-items: center; gap: 18rpx; }
.pickup-radio { width: 38rpx; height: 38rpx; display: flex; align-items: center; justify-content: center; border: 2rpx solid $cozy-primary; border-radius: 50%; }
.pickup-radio-dot { width: 20rpx; height: 20rpx; border-radius: 50%; background: $cozy-primary; }
.pickup-title { display: block; color: $cozy-ink; font-size: 28rpx; font-weight: 650; }
.pickup-description { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 22rpx; }
.checkout-line { display: flex; align-items: center; gap: 24rpx; padding: 24rpx 0; border-bottom: 1rpx solid $cozy-border; }
.checkout-line:last-child { border-bottom: 0; }
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
.mock-tip { margin-top: 24rpx; padding: 24rpx 28rpx; border-radius: 16rpx; background: $cozy-accent-soft; }
.mock-tip-title { display: block; color: $cozy-accent; font-size: 25rpx; font-weight: 700; }
.mock-tip-copy { display: block; margin-top: 8rpx; color: #53604b; font-size: 21rpx; line-height: 1.5; }
.bottom-spacer { height: 180rpx; }
.coupon-layer { position: fixed; inset: 0; z-index: 120; }
.coupon-mask { position: absolute; inset: 0; background: rgba(25, 18, 14, .46); }
.coupon-sheet { position: absolute; left: 0; right: 0; bottom: 0; max-height: 72vh; padding: 20rpx 28rpx max(20rpx, env(safe-area-inset-bottom)); border-radius: 32rpx 32rpx 0 0; background: #fff; }
.coupon-header { display: flex; align-items: center; justify-content: space-between; padding: 8rpx 0 20rpx; border-bottom: 1rpx solid $cozy-border; }
.coupon-title { color: $cozy-ink; font-family: $font-display; font-size: 32rpx; font-weight: 600; }
.coupon-close { width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; color: $cozy-muted; font-size: 44rpx; }
.coupon-scroll { max-height: 56vh; }
.coupon-option { min-height: 112rpx; padding: 22rpx 10rpx; display: flex; align-items: center; justify-content: space-between; gap: 24rpx; border-bottom: 1rpx solid $cozy-border; }
.coupon-option.selected { color: $cozy-primary; }
.coupon-name { display: block; color: $cozy-ink; font-size: 28rpx; font-weight: 650; }
.coupon-description { display: block; margin-top: 8rpx; color: $cozy-muted; font-size: 21rpx; }
.coupon-check { color: $cozy-primary; font-size: 34rpx; font-weight: 750; }

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
