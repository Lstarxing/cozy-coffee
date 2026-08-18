<!--
  订单详情页 - 精确复现 prototype/order-detail.html：统一小票
  状态区 → 门店/配送 → 商品 → 金额 → 订单信息；支持咖啡+兑换、自取+外送
-->
<template>
  <view class="detail-page">
    <LoadingState v-if="loading" text="正在加载订单…" />
    <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadOrder" />

    <view v-else-if="order" class="detail-content">
      <!-- 状态卡（白卡：取餐号在上居中 + 三节点进度 已下单/制作中/待取餐） -->
      <view class="status-card">
        <text v-if="statusText && (isRedeem || isCancelled)" class="status-line">{{ statusText }}</text>
        <view class="pickup-code-block">
          <text class="pickup-label">{{ codeCaption }}</text>
          <text class="pickup-code" :class="{ long: codeText.length > 6 }">{{ codeText }}</text>
        </view>
        <text v-if="pendingCountdown" class="pickup-eta urgent">{{ pendingCountdown }}</text>
        <text v-else-if="codeEta" class="pickup-eta">{{ codeEta }}</text>

        <view v-if="!isRedeem" class="progress">
          <view class="progress-seg seg-a" :class="{ on: reachedIndex >= 1 }"></view>
          <view class="progress-seg seg-b" :class="{ on: reachedIndex >= 2 }"></view>
          <view
            v-for="(step, idx) in progressSteps"
            :key="idx"
            class="progress-step"
            :class="{ current: step.current }"
          >
            <CozyIcon :name="step.icon" :size="28" stroke-width="2" :color="step.nodeColor" />
            <text class="progress-label">{{ step.label }}</text>
          </view>
        </view>
      </view>

      <!-- 门店 + 商品（白卡，参考原型合并为一个板块） -->
      <view class="store-products">
        <view class="sp-store">
          <view class="sp-store-main">
            <text class="sp-store-name">{{ isDelivery ? '配送至' : storeName }}</text>
            <text class="sp-store-addr">{{ storeAddr }}</text>
          </view>
          <view v-if="!isDelivery && !isVirtual" class="sp-store-actions">
            <view class="sp-store-act" @click="callStore"><CozyIcon name="phone" :size="16" color="#753A22" /></view>
            <view class="sp-store-act" @click="navigateStore"><CozyIcon name="send" :size="16" color="#753A22" /></view>
          </view>
        </view>

        <view class="sp-divider" />

        <view v-for="item in orderItems" :key="itemKey(item)" class="sp-product">
          <image v-if="item.productImage" :src="item.productImage" class="sp-product-img" mode="aspectFill" />
          <view class="sp-product-main">
            <text class="sp-name">{{ item.productName || item.name }}</text>
            <text class="sp-spec">{{ itemSpec(item) }}</text>
          </view>
          <view class="sp-right">
            <template v-if="isRedeem">
              <text class="sp-points">{{ formatPoints(item.pointsCost) }} 积分</text>
            </template>
            <template v-else>
              <text class="sp-price">¥{{ money(item.itemAmount ?? Number(item.unitPrice || 0) * Number(item.quantity || 1)) }}</text>
              <text class="sp-qty">x{{ item.quantity }}</text>
            </template>
          </view>
        </view>

        <view class="sp-divider" />

        <view class="sp-total">
          <text class="sp-total-count">共 {{ totalCount }} 份</text>
          <text v-if="isRedeem" class="sp-total-price">实付 {{ formatPoints(consumePoints) }} 积分</text>
          <text v-else class="sp-total-price">实付 ¥{{ money(payAmount) }}</text>
        </view>

        <view v-if="rewards" class="sp-reward">
          <text class="sp-reward-label">本次获得</text>
          <view class="sp-reward-values">
            <view v-if="rewards.points" class="sp-reward-item">
              <CozyIcon name="star" :size="20" color="#753A22" />
              <text class="sp-reward-text">积分 +{{ rewards.points }}</text>
            </view>
            <text v-if="rewards.points && rewards.exp" class="sp-reward-sep">|</text>
            <view v-if="rewards.exp" class="sp-reward-item">
              <CozyIcon name="sparkle" :size="20" color="#753A22" />
              <text class="sp-reward-text">成长值 +{{ rewards.exp }}</text>
            </view>
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
          <view v-if="Number(order.discountAmount || 0)" class="amount-row discount">
            <text>优惠 <text class="coupon-tag">{{ couponLabel }}</text></text>
            <text>-¥{{ money(order.discountAmount) }}</text>
          </view>
          <view v-if="deliveryFee" class="amount-row"><text>配送费</text><text>{{ money(deliveryFee) }}</text></view>
        </template>
      </view>

      <!-- 订单信息（低权重） -->
      <view class="meta-block">
        <view class="meta-row"><text>下单时间</text><text>{{ formatFullTime(order.createdAt) }}</text></view>
        <view class="meta-row">
          <text>订单编号</text>
          <view class="meta-value"><text selectable>{{ order.orderNo }}</text><text class="copy-btn" @click="copyOrderNo">复制</text></view>
        </view>
      </view>

      <view class="detail-spacer" />
    </view>

    <!-- 底部操作 -->
    <view v-if="order && canAction" class="detail-footer safe-area-bottom">
      <view class="reorder-btn" :class="{ disabled: paying || reordering }" @click="footerAction">
        <template v-if="isPendingPay">{{ paying ? '支付中…' : '去支付' }}</template>
        <template v-else-if="isRedeem">{{ reordering ? '跳转中…' : '再次兑换' }}</template>
        <template v-else>{{ reordering ? '恢复中…' : '再来一单' }}</template>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderDetail, acceptOrder } from '@/api/order'
import { getRedemptionDetail } from '@/api/member'
import { useCartStore } from '@/stores/cart'
import { useSessionStore } from '@/stores/session'
import { restoreOrderToCart } from '@/services/order/ReorderService'
import { refreshMemberProfile } from '@/services/session/MemberProfileService'
import { FIXED_STORE } from '@/config/store'
import { formatCoffeeSpec } from '@/utils/spec'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'
import CozyIcon from '@/components/CozyIcon.vue'

const orderId = ref('')
const type = ref('coffee')
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const reordering = ref(false)
const paying = ref(false)
const nowTs = ref(Date.now())
let countdownTicker = null
const cartStore = useCartStore()
const sessionStore = useSessionStore()

const isRedeem = computed(() => type.value === 'redeem')
const normalizedStatus = computed(() => String(order.value?.status || '').toLowerCase())
const isCancelled = computed(() => ['cancelled', 'canceled'].includes(normalizedStatus.value))
const isDelivery = computed(() => {
  if (isRedeem.value) return order.value?.fulfillmentType === 'DELIVERY'
  return String(order.value?.diningMethod || '').toUpperCase() === 'DELIVERY'
})
const isVirtual = computed(() => isRedeem.value && order.value?.fulfillmentType === 'VIRTUAL')

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

const isCoffeeDelivery = computed(() => isDelivery.value && !isRedeem.value)
const deliveryEtaTime = computed(() => {
  const raw = order.value?.estimatedPickupAt || order.value?.expectReadyTime || order.value?.readyAt || order.value?.estimatedReadyAt
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return ''
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
})
const codeCaption = computed(() => {
  if (isCoffeeDelivery.value) return '预计送达'
  if (isDelivery.value) return '物流单号'
  if (isVirtual.value) return '兑换码'
  if (isRedeem.value) return '取货码'
  return '取餐码'
})
const codeText = computed(() => {
  if (isCoffeeDelivery.value) return deliveryEtaTime.value || '配送中'
  if (isDelivery.value) return order.value?.trackingNumber || order.value?.deliveryNo || '待出库'
  if (isVirtual.value) return order.value?.virtualCode || '待发放'
  return order.value?.pickupCode || order.value?.virtualCode || '—'
})
const codeEta = computed(() => {
  if (isCoffeeDelivery.value) return ''
  const raw = order.value?.estimatedPickupAt || order.value?.expectReadyTime || order.value?.readyAt || order.value?.estimatedReadyAt
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return ''
  const pad = n => String(n).padStart(2, '0')
  return `预计 ${pad(d.getHours())}:${pad(d.getMinutes())} ${isDelivery.value ? '送达' : '可取'}`
})

const isPendingPay = computed(() => ['pending', 'pending_payment'].includes(normalizedStatus.value))
const pendingCountdown = computed(() => {
  if (!isPendingPay.value) return ''
  const raw = order.value?.expireAt
  const expireMs = raw ? new Date(raw).getTime() : NaN
  if (Number.isNaN(expireMs)) return '等待支付'
  const remain = Math.max(0, Math.floor((expireMs - nowTs.value) / 1000))
  if (remain <= 0) return '即将自动取消'
  const mm = String(Math.floor(remain / 60)).padStart(2, '0')
  const ss = String(remain % 60).padStart(2, '0')
  return `剩余 ${mm}:${ss} 自动取消`
})
const statusText = computed(() => {
  const status = normalizedStatus.value
  const redeem = {
    pending: isVirtual.value ? '待发放' : '待处理 · 即将发货',
    processing: '处理中 · 等待揽收',
    shipped: isVirtual.value ? '已发放 · 兑换码已到账' : ('已发货 · ' + (order.value?.shippingCompany || '快递配送')),
    completed: '已完成' + (isDelivery.value ? ' · 已送达' : isVirtual.value ? ' · 已发放' : ' · 到店自提'),
    cancelled: '已取消 · 积分已退还'
  }
  const coffee = {
    pending: '待支付',
    pending_payment: '待支付',
    preparing: '咖啡制作中',
    processing: '咖啡制作中',
    completed: '订单已完成',
    cancelled: '订单已取消'
  }
  return (isRedeem.value ? redeem : coffee)[status] || '订单已提交'
})

// 三节点进度：已下单 → 制作中 → 待取餐/待送达（reached 为已到达的节点下标）
const reachedIndex = computed(() => {
  const s = normalizedStatus.value
  if (['cancelled', 'canceled'].includes(s)) return -1
  if (['preparing', 'processing'].includes(s)) return 1
  if (['completed'].includes(s)) return 2
  return 0
})
const progressSteps = computed(() => {
  const reached = reachedIndex.value
  const labels = isDelivery.value ? ['已下单', '制作中', '待送达'] : ['已下单', '制作中', '待取餐']
  const icons = isDelivery.value ? ['receipt', 'coffee', 'truck'] : ['receipt', 'coffee', 'bell']
  return labels.map((label, idx) => ({
    label,
    icon: icons[idx],
    current: reached === idx,
    nodeColor: reached === idx ? '#2B1E16' : '#756A63'
  }))
})

const storeName = computed(() => {
  if (isVirtual.value) return '虚拟商品'
  if (isRedeem.value) return order.value?.storeName || '杭州中心店'
  return FIXED_STORE.name
})
const storeAddr = computed(() => {
  if (isVirtual.value) return '兑换码已发放至账户，可在「我的 · 卡券」查看'
  if (isDelivery.value) return order.value?.receiverAddress || '配送至已确认收货地址'
  if (isRedeem.value) return order.value?.storeName ? '到店自提 · 文三路 128 号' : FIXED_STORE.address
  return FIXED_STORE.address
})

const consumePoints = computed(() => order.value?.pointsCost || 0)
const rewards = computed(() => {
  if (isRedeem.value || normalizedStatus.value !== 'completed') return null
  const points = Number(order.value?.pointsEarned || 0)
  const exp = Number(order.value?.expEarned || 0)
  return (points || exp) ? { points, exp } : null
})
const totalCount = computed(() => orderItems.value.reduce((sum, item) => sum + Number(item.quantity || 1), 0))
const payAmount = computed(() => Number(order.value?.payAmount ?? order.value?.totalAmount ?? 0))
const remainingPoints = computed(() => Number(sessionStore.memberInfo?.currentPoints) || 0)
const couponLabel = computed(() => order.value?.couponName || order.value?.couponLabel || '优惠')
const deliveryFee = computed(() => Number(order.value?.deliveryFee || 0))

const canAction = computed(() => {
  if (isRedeem.value) return true
  return Boolean(order.value?.items?.length)
})

onLoad(options => {
  orderId.value = options.id || options.orderId || ''
  if (options.type === 'redeem') type.value = 'redeem'
})
onShow(() => {
  if (orderId.value) loadOrder()
  startCountdownTicker()
})
onUnload(stopCountdownTicker)

function startCountdownTicker() {
  stopCountdownTicker()
  countdownTicker = setInterval(() => { nowTs.value = Date.now() }, 1000)
}
function stopCountdownTicker() {
  if (countdownTicker) { clearInterval(countdownTicker); countdownTicker = null }
}

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
function itemSpec(item) {
  if (isRedeem.value) return item.spec || (item.quantity ? `数量 ×${item.quantity}` : '')
  return formatCoffeeSpec(item) || '默认规格'
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
  if (reordering.value || paying.value) return
  if (isPendingPay.value) {
    await payPendingOrder()
    return
  }
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
        title: '部分商品有变动',
        content: notices.join('\n'),
        showCancel: false,
        confirmText: '查看购物车',
        success: openRestoredCart
      })
    } else {
      openRestoredCart()
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

async function payPendingOrder() {
  paying.value = true
  try {
    const confirmed = await new Promise(resolve => {
      uni.showModal({
        title: '模拟支付',
        content: `订单金额 ¥${money(order.value?.payAmount ?? order.value?.totalAmount)}，确认模拟支付？`,
        confirmText: '确认支付',
        cancelText: '取消',
        success: res => resolve(res.confirm)
      })
    })
    if (!confirmed) return
    uni.showLoading({ title: '正在支付', mask: true })
    await acceptOrder(orderId.value)
    uni.hideLoading()
    uni.showToast({ title: '支付成功，商家已接单', icon: 'none', duration: 2200 })
    loadOrder()
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '支付失败，请稍后重试', icon: 'none', duration: 2200 })
  } finally {
    paying.value = false
  }
}
</script>

<style lang="scss" scoped>
.detail-page { min-height: 100vh; background: $cozy-surface; }
.detail-content { padding: 12rpx 40rpx 0; }

/* ── 状态卡（白卡：取餐号在上居中 + 三节点进度） ── */
.status-card {
  margin-top: 24rpx;
  padding: 44rpx 32rpx 40rpx;
  border-radius: 28rpx;
  background: $bg-white;
  text-align: center;
}
.status-line {
  display: block;
  font-size: 26rpx;
  color: $cozy-ink;
}
.pickup-code-block {
  margin-top: 30rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}
.pickup-label {
  font-size: 26rpx;
  color: $cozy-muted;
}
.pickup-code {
  font-family: $font-display;
  font-size: 64rpx;
  font-weight: 700;
  line-height: 1;
  letter-spacing: .06em;
  color: $cozy-ink;
  word-break: break-all;
}
.pickup-code.long { font-size: 42rpx; letter-spacing: .02em; }
.pickup-eta {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}
.pickup-eta.urgent { color: $error-color; font-weight: 650; }

/* ── 三节点进度（裸 icon，仅当前状态墨黑高亮，短线在 icon 下方间隙） ── */
.progress {
  position: relative;
  display: flex;
  margin-top: 40rpx;
}
.progress-seg {
  position: absolute;
  top: 56rpx;
  height: 2rpx;
  background: $cozy-border;
  transition: background .3s $cozy-ease-out;
}
.progress-seg.on { background: $cozy-ink; }
.seg-a { left: 25.83%; width: 15%; }
.seg-b { left: 59.17%; width: 15%; }
.progress-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}
.progress-label {
  font-size: 22rpx;
  color: $cozy-muted;
}
.progress-step.current .progress-label {
  color: $cozy-ink;
  font-weight: 650;
}

/* ── 门店 + 商品（白卡，合并为一个板块） ── */
.store-products {
  margin-top: 24rpx;
  padding: 8rpx 32rpx;
  border-radius: 28rpx;
  background: $bg-white;
}
.sp-store {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 30rpx 4rpx 26rpx;
}
.sp-store-main { flex: 1; min-width: 0; }
.sp-store-name {
  display: block;
  font-family: $font-display;
  font-size: 30rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.sp-store-addr {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: $cozy-muted;
}
.sp-store-actions {
  flex: none;
  display: flex;
  gap: 12rpx;
}
.sp-store-act {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  border: 1rpx solid $cozy-border;
  background: $bg-white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;

  &:active { opacity: .6; }
}
.sp-divider { height: 1rpx; background: $cozy-border; }
.sp-product {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 26rpx 4rpx;
}
.sp-product-img {
  flex: none;
  width: 112rpx;
  height: 112rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #E8DDD2, #D8C8B4);
  overflow: hidden;
}
.sp-product-main { flex: 1; min-width: 0; }
.sp-name {
  display: block;
  font-size: 28rpx;
  font-weight: 650;
  color: $cozy-ink;
}
.sp-spec {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: $cozy-muted;
}
.sp-right { flex: none; text-align: right; }
.sp-price { display: block; font-size: 28rpx; font-weight: 650; color: $cozy-ink; }
.sp-qty { display: block; margin-top: 8rpx; font-size: 22rpx; color: $cozy-muted; }
.sp-points { display: block; font-size: 26rpx; font-weight: 700; color: $cozy-primary; }
.sp-total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 16rpx;
  padding: 28rpx 4rpx 20rpx;
}
.sp-total-count { font-size: 24rpx; color: $cozy-muted; }
.sp-total-price { font-size: 28rpx; font-weight: 700; color: $cozy-ink; }

/* ── 本次获得（完成订单，位于实付下方，对齐确认页可得积分样式） ── */
.sp-reward {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  padding: 0 4rpx 24rpx;
}
.sp-reward-label { flex: none; color: $cozy-muted; font-size: 22rpx; }
.sp-reward-values { display: flex; align-items: center; gap: 16rpx; }
.sp-reward-item { display: flex; align-items: center; gap: 6rpx; }
.sp-reward-text { color: $cozy-primary; font-size: 22rpx; font-weight: 650; }
.sp-reward-sep { color: $cozy-border; font-size: 22rpx; }

/* ── 金额 ── */
.amount-block { padding: 12rpx 4rpx 20rpx; }
.amount-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 24rpx;
  padding: 18rpx 0;
  font-size: 26rpx;
  color: $cozy-ink;

  .coupon-tag { font-size: 20rpx; color: $cozy-muted; }
  &.discount { color: $cozy-primary; }
}

/* ── 订单信息（低权重） ── */
.meta-block {
  padding: 28rpx 4rpx 8rpx;
  border-top: 1rpx solid $cozy-border;
}
.meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 24rpx;
  padding: 16rpx 0;
  font-size: 24rpx;
  color: $cozy-muted;

  > text:last-child, .meta-value { color: $cozy-ink; text-align: right; }
}
.meta-value { display: flex; align-items: center; gap: 12rpx; min-width: 0; }
.copy-btn {
  flex: none;
  padding: 0 14rpx;
  border-radius: 12rpx;
  border: 1rpx solid $cozy-border;
  color: $cozy-muted;
  font-size: 22rpx;
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
  padding: 24rpx 40rpx max(24rpx, env(safe-area-inset-bottom));
  background: transparent;
}
.reorder-btn {
  height: 96rpx;
  border-radius: 20rpx;
  background: $cozy-ink;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 600;

  &:active { opacity: .85; }
  &.disabled { opacity: .55; }
}
</style>
