<!--
  订单详情页 - 复现 prototype/order-detail.html：统一小票
  状态区 → 门店/配送 → 商品 → 金额 → 订单信息；支持咖啡+兑换、自取+外送
-->
<template>
  <view class="detail-page">
    <LoadingState v-if="loading" text="正在加载订单…" />
    <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadOrder" />

    <view v-else-if="order" class="detail-content">
      <!-- 状态区 -->
      <view class="pickup-panel" :class="statusClass">
        <text class="pickup-eyebrow">{{ eyebrow }}</text>
        <text class="pickup-code">{{ codeText }}</text>
        <text class="code-caption">{{ codeCaption }}</text>
        <text class="pickup-status">{{ statusText }}</text>
      </view>

      <!-- 门店/配送（第一主体） -->
      <view class="store-block">
        <view class="store-copy">
          <text class="store-name">{{ isDelivery ? '配送至' : storeName }}</text>
          <text class="store-addr">{{ storeAddr }}</text>
        </view>
        <view v-if="!isDelivery" class="store-actions">
          <view class="store-act" @click="callStore"><CozyIcon name="phone" :size="17" color="#753A22" /></view>
          <view class="store-act" @click="navigateStore"><CozyIcon name="pin" :size="17" color="#753A22" /></view>
        </view>
      </view>

      <!-- 商品 -->
      <view class="products">
        <view v-for="item in orderItems" :key="itemKey(item)" class="product-row">
          <image :src="item.productImage || '/static/images/default-product.png'" class="product-img" mode="aspectFill" />
          <view class="product-main">
            <text class="product-name">{{ item.productName || item.name }}</text>
            <text class="product-spec">{{ itemSpec(item) }}</text>
          </view>
          <view class="product-right">
            <template v-if="isRedeem">
              <text class="product-points">{{ formatPoints(item.pointsCost) }} 积分</text>
            </template>
            <template v-else>
              <text class="product-price">¥{{ money(item.itemAmount ?? Number(item.unitPrice || 0) * Number(item.quantity || 1)) }}</text>
              <text class="product-qty">× {{ item.quantity }}</text>
            </template>
          </view>
        </view>
      </view>

      <!-- 金额（优惠总结） -->
      <view class="amount-block">
        <template v-if="isRedeem">
          <view class="amount-row"><text>消耗积分</text><text>{{ formatPoints(consumePoints) }} 积分</text></view>
          <view class="amount-row"><text>兑换后剩余</text><text>{{ formatPoints(remainingPoints) }} 积分</text></view>
        </template>
        <template v-else>
          <view class="amount-row"><text>商品金额</text><text>¥{{ money(order.totalAmount) }}</text></view>
          <view v-if="Number(order.discountAmount || 0)" class="amount-row discount">
            <text>优惠 <text class="coupon-tag">{{ couponLabel }}</text></text>
            <text>-¥{{ money(order.discountAmount) }}</text>
          </view>
          <view v-if="deliveryFee" class="amount-row"><text>配送费</text><text>{{ money(deliveryFee) }}</text></view>
        </template>
        <view class="amount-divider" />
        <view class="amount-row total">
          <text>{{ isRedeem ? '实付' : '实付金额' }}</text>
          <text>{{ totalText }}</text>
        </view>
      </view>

      <!-- 订单信息（低权重） -->
      <view class="meta-block">
        <view class="meta-row"><text>下单时间</text><text>{{ formatFullTime(order.createdAt) }}</text></view>
        <view class="meta-row">
          <text>订单编号</text>
          <view class="meta-value"><text selectable>{{ order.orderNo }}</text><text class="copy-btn" @click="copyOrderNo">复制</text></view>
        </view>
        <view class="meta-row"><text>{{ codeCaption }}</text><text>{{ codeText }}</text></view>
      </view>

      <view class="detail-spacer" />
    </view>

    <!-- 底部操作 -->
    <view v-if="order && canAction" class="detail-footer safe-area-bottom">
      <view class="reorder-btn" :class="{ disabled: reordering }" @click="footerAction">
        {{ isRedeem ? (reordering ? '跳转中…' : '再次兑换') : (reordering ? '恢复中…' : '再来一单') }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { cancelOrder, getOrderDetail } from '@/api/order'
import { getRedemptionDetail } from '@/api/member'
import { useCartStore } from '@/stores/cart'
import { useSessionStore } from '@/stores/session'
import { restoreOrderToCart } from '@/services/order/ReorderService'
import { refreshMemberProfile } from '@/services/session/MemberProfileService'
import { FIXED_STORE } from '@/config/store'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'
import CozyIcon from '@/components/CozyIcon.vue'

const orderId = ref('')
const type = ref('coffee')
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const reordering = ref(false)
const cartStore = useCartStore()
const sessionStore = useSessionStore()

const isRedeem = computed(() => type.value === 'redeem')
const normalizedStatus = computed(() => String(order.value?.status || '').toLowerCase())
const isDelivery = computed(() => {
  if (isRedeem.value) return order.value?.fulfillmentType === 'DELIVERY'
  return String(order.value?.diningMethod || '').toUpperCase() === 'DELIVERY'
})

const orderItems = computed(() => {
  if (isRedeem.value) {
    const o = order.value || {}
    if (o.items?.length) return o.items
    return [{
      productName: o.productName,
      productImage: o.productImage,
      spec: `数量 ×${o.quantity || 1}`,
      pointsCost: o.pointsCost,
      quantity: o.quantity || 1
    }]
  }
  return order.value?.items || []
})

const statusClass = computed(() => ({
  completed: 'completed',
  cancelled: 'cancelled',
  canceled: 'cancelled'
})[normalizedStatus.value] || 'active')

const eyebrow = computed(() => {
  if (isRedeem.value) return isDelivery.value ? 'COZY DELIVERY' : 'COZY PICKUP'
  return isDelivery.value ? 'COZY DELIVERY' : 'COZY PICKUP'
})
const codeCaption = computed(() => {
  if (isDelivery.value) return '物流单号'
  if (isRedeem.value) return '取货码'
  return '取餐码'
})
const codeText = computed(() => {
  if (isDelivery.value) return order.value?.trackingNumber || order.value?.deliveryNo || '待出库'
  return order.value?.pickupCode || order.value?.virtualCode || '—'
})
const statusText = computed(() => {
  const status = normalizedStatus.value
  const redeem = {
    pending: '待处理 · 即将发货',
    processing: '处理中 · 等待揽收',
    shipped: '已发货 · ' + (order.value?.shippingCompany || '快递配送'),
    completed: '已完成' + (isDelivery.value ? ' · 已送达' : ' · 到店自提'),
    cancelled: '已取消 · 积分已退还'
  }
  const coffee = {
    pending: '等待门店接单',
    pending_payment: '订单待处理',
    preparing: '咖啡制作中',
    processing: '咖啡制作中',
    completed: '订单已完成',
    cancelled: '订单已取消'
  }
  return (isRedeem.value ? redeem : coffee)[status] || '订单已提交'
})

const storeName = computed(() => isRedeem.value ? (order.value?.storeName || '杭州中心店') : FIXED_STORE.name)
const storeAddr = computed(() => {
  if (isDelivery.value) return order.value?.receiverAddress || '配送至已确认收货地址'
  if (isRedeem.value) return order.value?.storeName ? '到店自提 · 文三路 128 号' : FIXED_STORE.address
  return FIXED_STORE.address
})

const consumePoints = computed(() => order.value?.pointsCost || 0)
const remainingPoints = computed(() => {
  const current = Number(sessionStore.memberInfo?.currentPoints) || 0
  return current
})
const couponLabel = computed(() => order.value?.couponName || order.value?.couponLabel || '优惠')
const deliveryFee = computed(() => Number(order.value?.deliveryFee || 0))
const totalText = computed(() => isRedeem.value
  ? `${formatPoints(consumePoints)} 积分`
  : `¥${money(order.value?.payAmount ?? order.value?.totalAmount)}`)

const canAction = computed(() => {
  if (isRedeem.value) return true
  return Boolean(order.value?.items?.length)
})

onLoad(options => {
  orderId.value = options.id || options.orderId || ''
  if (options.type === 'redeem') type.value = 'redeem'
})
onShow(() => { if (orderId.value) loadOrder() })

async function loadOrder(silent = false) {
  if (!silent) loading.value = true
  errorMessage.value = ''
  try {
    const response = isRedeem.value ? await getRedemptionDetail(orderId.value) : await getOrderDetail(orderId.value)
    order.value = response?.data ?? response
    if (!isRedeem.value) refreshMemberWhenCompleted(order.value)
  } catch (error) {
    errorMessage.value = error?.message || '订单加载失败，请重试'
  } finally {
    if (!silent) loading.value = false
  }
}

let completedMemberRefreshed = false
async function refreshMemberWhenCompleted(currentOrder) {
  if (String(currentOrder?.status || '').toLowerCase() !== 'completed' || completedMemberRefreshed) return
  completedMemberRefreshed = true
  try { await refreshMemberProfile(sessionStore) } catch (_) { completedMemberRefreshed = false }
}

function itemKey(item) {
  return item?.id ?? `${item.productId}-${item.productName}-${item.spec}`
}
function parseOptions(value) { try { return typeof value === 'string' ? JSON.parse(value) : (value || {}) } catch (_) { return {} } }
function itemSpec(item) {
  if (isRedeem.value) return item.spec || (item.quantity ? `数量 ×${item.quantity}` : '')
  const options = parseOptions(item.optionsJson)
  return [item.cupSize, item.temperature, item.sugarLevel, options.milkType || item.milkType, item.coffeeStrength]
    .filter(Boolean)
    .join(' · ') || '默认规格'
}
function money(value) { return Number(value || 0).toFixed(2) }
function formatPoints(value) { return Number(value || 0).toLocaleString() }
function formatFullTime(value) {
  if (!value) return '--'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ').slice(0, 19)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function copyOrderNo() {
  uni.setClipboardData({
    data: String(order.value?.orderNo || ''),
    success: () => uni.showToast({ title: '订单号已复制', icon: 'none' })
  })
}
function callStore() {
  uni.showToast({ title: '拨打门店电话', icon: 'none' })
}
function navigateStore() {
  uni.showToast({ title: '打开导航', icon: 'none' })
}

async function footerAction() {
  if (reordering.value) return
  reordering.value = true
  if (isRedeem.value) {
    uni.navigateTo({ url: '/pages/mall/index' })
    reordering.value = false
    return
  }
  uni.showLoading({ title: '正在恢复商品', mask: true })
  try {
    const result = await restoreOrderToCart({ order: order.value, cartStore })
    uni.hideLoading()
    if (!result.restoredQuantity) {
      uni.showToast({ title: '原订单商品均已下架，请重新选择', icon: 'none', duration: 2200 })
      return
    }
    const notices = []
    if (result.invalidItems.length) notices.push(`${result.invalidItems.length} 款商品已下架，未加入购物车`)
    if (result.adjustedItems.length) notices.push(`${result.adjustedItems.length} 款商品规格已按当前菜单调整`)
    if (notices.length) {
      uni.showModal({
        title: `已恢复 ${result.restoredQuantity} 件商品`,
        content: notices.join('\n'),
        showCancel: false,
        confirmText: '查看购物车',
        success: openRestoredCart
      })
    } else {
      openRestoredCart()
      uni.showToast({ title: `已恢复 ${result.restoredQuantity} 件商品`, icon: 'none' })
    }
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '恢复商品失败，请稍后重试', icon: 'none', duration: 2200 })
  } finally {
    reordering.value = false
  }
}

function openRestoredCart() {
  uni.setStorageSync('cozy_open_cart_on_menu', '1')
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>

<style lang="scss" scoped>
.detail-page { min-height: 100vh; background: $cozy-surface; }
.detail-content { padding: 24rpx; }

/* ── 状态区（品牌色，紧凑） ── */
.pickup-panel {
  padding: 34rpx 32rpx 30rpx;
  border-radius: $cozy-radius-lg;
  text-align: center;
  color: #fff;

  &.active { background: $cozy-primary; }
  &.completed { background: $cozy-accent; }
  &.cancelled { background: $cozy-muted; }
}
.pickup-eyebrow { display: block; font-size: 18rpx; font-weight: 700; letter-spacing: .24em; opacity: .8; }
.pickup-code {
  display: block;
  margin-top: 14rpx;
  font-family: $font-display;
  font-size: 60rpx;
  font-weight: 800;
  letter-spacing: .06em;
  line-height: 1;
  word-break: break-all;
}
.code-caption { display: block; margin-top: 14rpx; font-size: 18rpx; letter-spacing: .16em; opacity: .75; }
.pickup-status { display: block; margin-top: 14rpx; font-size: 22rpx; opacity: .85; }

/* ── 门店/配送 ── */
.store-block {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 36rpx 4rpx 30rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.store-copy { flex: 1; min-width: 0; }
.store-name {
  display: block;
  font-family: $font-display;
  font-size: 36rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.store-addr {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $cozy-muted;
}
.store-actions { flex: none; display: flex; gap: 14rpx; }
.store-act {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  border: 1rpx solid $cozy-border;
  background: $bg-white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;

  &:active { opacity: .6; }
}

/* ── 商品 ── */
.products { padding: 6rpx 4rpx; }
.product-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 26rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
}
.product-img {
  flex: none;
  width: 112rpx;
  height: 112rpx;
  border-radius: $cozy-radius-md;
  background: linear-gradient(135deg, #E8DDD2, #D8C8B4);
}
.product-main { flex: 1; min-width: 0; }
.product-name {
  display: block;
  font-family: $font-display;
  font-size: 30rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.product-spec {
  display: block;
  margin-top: 8rpx;
  font-size: 21rpx;
  color: $cozy-muted;
  line-height: 1.5;
}
.product-right { flex: none; text-align: right; }
.product-price { display: block; font-size: 27rpx; font-weight: 650; color: $cozy-ink; }
.product-qty { display: block; margin-top: 6rpx; font-size: 20rpx; color: $cozy-muted; }
.product-points { display: block; font-size: 27rpx; font-weight: 700; color: $cozy-primary; }

/* ── 金额 ── */
.amount-block { padding: 8rpx 4rpx 16rpx; }
.amount-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20rpx;
  padding: 15rpx 0;
  font-size: 24rpx;
  color: $cozy-ink;

  .coupon-tag { font-size: 19rpx; color: $cozy-muted; }
  &.discount { color: $cozy-primary; }
}
.amount-divider { height: 1rpx; background: $cozy-border; margin: 12rpx 0 6rpx; }
.amount-row.total {
  padding-top: 20rpx;
  font-weight: 700;

  span:last-child { color: $cozy-primary; font-size: 36rpx; font-weight: 750; }
}

/* ── 订单信息（低权重） ── */
.meta-block {
  padding: 22rpx 4rpx 8rpx;
  border-top: 1rpx solid $cozy-border;
}
.meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20rpx;
  padding: 13rpx 0;
  font-size: 21rpx;
  color: $cozy-muted;

  > text:last-child, .meta-value { color: $cozy-ink; text-align: right; }
}
.meta-value { display: flex; align-items: center; gap: 12rpx; min-width: 0; }
.copy-btn {
  flex: none;
  padding: 0 12rpx;
  border-radius: $cozy-radius-sm;
  border: 1rpx solid $cozy-border;
  color: $cozy-muted;
  font-size: 19rpx;
  font-weight: 600;

  &:active { opacity: .6; }
}

.detail-spacer { height: 150rpx; }

/* ── 底部操作 ── */
.detail-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14rpx 28rpx max(14rpx, env(safe-area-inset-bottom));
  border-top: 1rpx solid $cozy-border;
  background: $bg-white;
}
.reorder-btn {
  height: 84rpx;
  border-radius: $cozy-radius-md;
  background: $cozy-primary;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-size-md;
  font-weight: 600;

  &:active { background: $cozy-primary-hover; }
  &.disabled { opacity: .55; }
}
</style>
