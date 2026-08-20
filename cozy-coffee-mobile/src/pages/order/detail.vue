<!--
  订单详情页 - 精确复现 prototype/order-detail.html：统一小票
  状态区 → 门店/配送 → 商品 → 金额 → 订单信息；支持咖啡+兑换、自提+外送
-->
<template>
  <view class="detail-page">
    <LoadingState v-if="loading" text="正在加载订单…" />
    <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadOrder" />

    <view v-else-if="order" class="detail-content">
      <!-- 未支付：等待支付卡（居中三行：等待支付/剩余倒计时/超时提示，含「已下单」首节点进度） -->
      <view v-if="isPendingPay" class="pending-card">
        <text class="pending-title">等待支付</text>
        <text class="pending-countdown">剩余 {{ pendingRemain }}</text>
        <text class="pending-sub">超过15分钟未支付，订单将自动取消</text>

        <view class="progress pending-progress">
          <view class="progress-seg seg-a"></view>
          <view class="progress-seg seg-b"></view>
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

        <view class="pending-actions">
          <view class="pending-btn ghost" @click="cancelPendingOrder">取消订单</view>
          <view class="pending-btn solid" @click="payPendingOrder">{{ paying ? '支付中…' : '立即支付' }}</view>
        </view>
      </view>

      <!-- 状态卡（白卡：取餐号在上居中 + 三节点进度 已下单/制作中/待取餐） -->
      <view v-else class="status-card">
        <text v-if="statusText && (isRedeem || isCancelled || isDelivering)" class="status-line">{{ statusText }}</text>
        <text v-if="isCancelled" class="status-thanks">感谢您对CozyCoffee的支持，欢迎再次光临</text>
        <view v-if="!isCancelled" class="pickup-code-block">
          <text class="pickup-label">{{ codeCaption }}</text>
          <text class="pickup-code" :class="{ long: codeText.length > 6 }">{{ codeText }}</text>
        </view>
        <text v-if="codeEta" class="pickup-eta">{{ codeEta }}</text>

        <view v-if="!isRedeem && !isCancelled" class="progress">
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
            <text class="sp-store-name">{{ storeName }}</text>
            <text class="sp-store-addr">{{ storeAddr }}</text>
          </view>
          <view v-if="!isVirtual" class="sp-store-actions">
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

        <!-- 金额（商品小计 + 可折叠配送费/优惠合计 + 实付 + 订单信息） -->
        <view class="sp-amount">
          <template v-if="isRedeem">
            <view class="sp-amount-row"><text>消耗积分</text><text>{{ formatPoints(consumePoints) }} 积分</text></view>
            <view class="sp-amount-row"><text>兑换后剩余</text><text>{{ formatPoints(remainingPoints) }} 积分</text></view>
          </template>
          <template v-else>
            <view class="sp-amount-row"><text>商品小计</text><text>¥{{ money(order.totalAmount) }}</text></view>

            <!-- 可折叠：配送费 / 优惠合计 → 展开券明细 -->
            <view v-if="deliveryFee || discount" class="sp-collapse" @click="amountExpanded = !amountExpanded">
              <view v-if="deliveryFee" class="sp-amount-row"><text>配送费</text><text>¥{{ money(deliveryFee) }}</text></view>
              <view v-if="discount" class="sp-amount-row discount">
                <view class="collapse-left">
                  <text>优惠合计</text>
                  <text class="collapse-arrow" :class="{ open: amountExpanded }">›</text>
                </view>
                <text>-¥{{ money(order.discountAmount) }}</text>
              </view>
            </view>
            <view class="sp-collapse-body" :class="{ open: amountExpanded }">
              <view v-for="(name, idx) in couponNames" :key="idx" class="sp-coupon-line">
                <text>{{ name }}</text>
                <text>-¥{{ money(order.discountAmount) }}</text>
              </view>
            </view>
          </template>

          <view class="sp-total-line">
            <text class="total-muted">共 {{ totalCount }} 件商品，实付</text>
            <text v-if="isRedeem" class="total-value">{{ formatPoints(consumePoints) }} 积分</text>
            <text v-else class="total-value">¥{{ money(payAmount) }}</text>
          </view>

          <!-- 本次获得（完成订单，紧贴实付下方） -->
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

          <!-- 订单信息（实付下方，分隔线分隔；配送方式/地址仅外送） -->
          <view class="sp-divider" />
          <view class="meta-row"><text>下单时间</text><text>{{ formatFullTime(order.createdAt) }}</text></view>
          <view class="meta-row">
            <text>订单编号</text>
            <view class="meta-value"><text selectable>{{ order.orderNo }}</text><text class="copy-btn" @click="copyOrderNo">复制</text></view>
          </view>
          <template v-if="isDelivery">
            <view class="sp-divider" />
            <view class="meta-row"><text>配送方式</text><text>外送</text></view>
            <view class="meta-row">
              <text>配送地址</text>
              <view class="delivery-addr">
                <text class="addr-line">{{ deliveryAddress }}</text>
                <text class="addr-line addr-contact">{{ deliveryContact }}</text>
              </view>
            </view>
          </template>
        </view>
      </view>

    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderDetail, acceptOrder, cancelOrder } from '@/api/order'
import { getRedemptionDetail } from '@/api/member'
import { useSessionStore } from '@/stores/session'
import { refreshMemberProfile } from '@/services/session/MemberProfileService'
import { FIXED_STORE } from '@/config/store'
import { formatCoffeeSpec } from '@/utils/spec'
import { formatFullTime, formatPoints, money } from '@/utils/format'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'
import CozyIcon from '@/components/CozyIcon.vue'

const orderId = ref('')
const type = ref('coffee')
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const paying = ref(false)
const nowTs = ref(Date.now())
let countdownTicker = null
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
const isDelivering = computed(() => normalizedStatus.value === 'delivering')
const deliveryEtaTime = computed(() => {
  const raw = order.value?.expectedDeliveryAt || order.value?.estimatedPickupAt || order.value?.expectReadyTime || order.value?.readyAt || order.value?.estimatedReadyAt
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
  const raw = order.value?.expectedDeliveryAt || order.value?.estimatedPickupAt || order.value?.expectReadyTime || order.value?.readyAt || order.value?.estimatedReadyAt
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return ''
  const pad = n => String(n).padStart(2, '0')
  return `预计 ${pad(d.getHours())}:${pad(d.getMinutes())} ${isDelivery.value ? '送达' : '可取'}`
})

const isPendingPay = computed(() => !isRedeem.value && ['pending', 'pending_payment'].includes(normalizedStatus.value))
const pendingRemain = computed(() => {
  if (!isPendingPay.value) return '--:--'
  const raw = order.value?.expireAt
  const expireMs = raw ? new Date(raw).getTime() : NaN
  if (Number.isNaN(expireMs)) return '--:--'
  const remain = Math.max(0, Math.floor((expireMs - nowTs.value) / 1000))
  const mm = String(Math.floor(remain / 60)).padStart(2, '0')
  const ss = String(remain % 60).padStart(2, '0')
  return `${mm}:${ss}`
})
const amountExpanded = ref(false)
const deliveryAddress = computed(() => order.value?.receiverAddress || '—')
const deliveryContact = computed(() => {
  const parts = [order.value?.receiverName, order.value?.receiverPhone].filter(Boolean)
  return parts.join(' · ') || '—'
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
    delivering: '配送中',
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
  if (['delivering', 'completed'].includes(s)) return 2
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
    nodeColor: reached === idx ? '#2B1E16' : '#C0B8B1'
  }))
})

const storeName = computed(() => {
  if (isVirtual.value) return '虚拟商品'
  if (isRedeem.value) return order.value?.storeName || '杭州中心店'
  return FIXED_STORE.name
})
const storeAddr = computed(() => {
  if (isVirtual.value) return '兑换码已发放至账户，可在「我的 · 卡券」查看'
  if (isRedeem.value && isDelivery.value) return order.value?.receiverAddress || '配送至已确认收货地址'
  if (isRedeem.value) return '到店自提 · 文三路 128 号'
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
const couponName = computed(() => order.value?.couponName || order.value?.couponLabel || '')
const couponNames = computed(() => {
  const names = order.value?.appliedCouponNames || []
  if (names.length) return names
  if (couponName.value) return [couponName.value]
  return ['优惠券']
})
const deliveryFee = computed(() => Number(order.value?.deliveryFee || 0))
const discount = computed(() => Number(order.value?.discountAmount || 0))

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

async function cancelPendingOrder() {
  const confirmed = await new Promise(resolve => {
    uni.showModal({
      title: '取消订单',
      content: '订单取消后，如使用优惠券会退回到卡包中',
      confirmText: '确定取消',
      cancelText: '再想想',
      success: res => resolve(res.confirm)
    })
  })
  if (!confirmed) return
  try {
    await cancelOrder(orderId.value)
    uni.showToast({ title: '订单已取消', icon: 'none' })
    loadOrder()
  } catch (error) {
    uni.showToast({ title: error?.message || '取消失败，请稍后重试', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.detail-page { min-height: 100vh; background: $cozy-surface; }
.detail-content { padding: 12rpx 40rpx 120rpx; }

/* ── 未支付：等待支付卡（居中三行 + 首节点进度） ── */
.pending-card {
  margin-top: 24rpx;
  padding: 40rpx 32rpx 32rpx;
  border-radius: 28rpx;
  background: $bg-white;
  text-align: center;
}
.pending-title {
  display: block;
  font-family: $font-display;
  font-size: 36rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.pending-countdown {
  display: block;
  margin-top: 12rpx;
  font-family: $font-display;
  font-size: 52rpx;
  font-weight: 700;
  letter-spacing: .02em;
  color: $cozy-primary;
}
.pending-sub {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}
.pending-progress { margin-top: 36rpx; }
.pending-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 36rpx;
}
.pending-btn {
  flex: 1;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  font-size: 27rpx;
  font-weight: 650;

  &.ghost { border: 1rpx solid $cozy-border; background: $bg-white; color: $cozy-ink; }
  &.solid { background: $cozy-ink; color: #fff; }
  &:active { opacity: .85; }
}

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
.status-thanks {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: $cozy-muted;
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
  padding: 8rpx 32rpx 40rpx;
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
.sp-divider { height: 1rpx; margin: 20rpx 0; background: $cozy-border; }
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
/* ── 金额（商品小计/配送费/优惠合计/实付 + 订单信息，商品详情模块内） ── */
.sp-amount { padding: 8rpx 4rpx 0; }
.sp-amount-row {
  min-height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  font-size: 25rpx;
  color: $cozy-ink;

  &.discount { color: $cozy-primary; }
}
.collapse-left {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.collapse-arrow {
  font-size: 30rpx;
  line-height: 1;
  color: $cozy-placeholder;
  transform: rotate(90deg);
  transition: transform .3s $cozy-ease-out;
}
.collapse-arrow.open { transform: rotate(270deg); }
.sp-collapse-body {
  max-height: 0;
  overflow: hidden;
  opacity: 0;
  transition: max-height .3s $cozy-ease-out, opacity .25s ease;
}
.sp-collapse-body.open {
  max-height: 200rpx;
  opacity: 1;
}
.sp-coupon-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  margin: 4rpx 4rpx 4rpx 24rpx;
  padding: 14rpx 4rpx 14rpx 20rpx;
  border-left: 2rpx solid $cozy-border;
  font-size: 24rpx;
  color: $cozy-ink;
}
.sp-coupon-line text:last-child { color: $cozy-primary; font-weight: 650; }

/* 实付（右对齐 + 共X件商品） */
.sp-total-line {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8rpx;
  margin-top: 8rpx;
  padding-top: 20rpx;
  font-size: 25rpx;
}
.total-muted { color: $cozy-muted; }
.total-value { color: $cozy-primary; font-size: 32rpx; font-weight: 750; }

.delivery-addr { flex: 1; min-width: 0; text-align: right; }
.addr-line { display: block; font-size: 24rpx; line-height: 1.5; color: $cozy-ink; }
.addr-contact { margin-top: 4rpx; color: $cozy-muted; }

/* ── 本次获得（完成订单，位于实付下方，对齐确认页可得积分样式） ── */
.sp-reward {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 12rpx;
  padding: 0 4rpx;
}
.sp-reward-label { flex: none; color: $cozy-muted; font-size: 22rpx; }
.sp-reward-values { display: flex; align-items: center; gap: 16rpx; }
.sp-reward-item { display: flex; align-items: center; gap: 6rpx; }
.sp-reward-text { color: $cozy-primary; font-size: 22rpx; font-weight: 650; }
.sp-reward-sep { color: $cozy-border; font-size: 22rpx; }

/* ── 订单信息（低权重，位于商品卡实付下方） ── */
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
</style>
