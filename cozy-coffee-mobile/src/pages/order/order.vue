<!--
  订单页 - 精确复现 prototype/order.html：自提/外送分类，仅咖啡订单
  每单: 时间+取餐码/外送地址 + 状态 + 商品 + 再来一单
-->
<template>
  <view class="order-page">
    <view class="order-top" :style="{ paddingTop: (statusBarHeight + 40) + 'px' }">
      <view class="category-switch">
        <view
          v-for="cat in categories"
          :key="cat.value"
          class="category-item"
          :class="{ active: currentCategory === cat.value }"
          @click="switchCategory(cat.value)"
        >
          <text class="category-label">{{ cat.label }}</text>
          <text v-if="counts[cat.value]" class="category-count">{{ counts[cat.value] }}</text>
        </view>
      </view>
    </view>

    <LoadingState v-if="loading && !coffeeOrders.length" :text="loadingText" />
    <RetryState v-else-if="errorMessage && !coffeeOrders.length" :description="errorMessage" @retry="loadCurrent" />

    <view v-else-if="shownOrders.length" class="order-list">
      <view v-for="order in shownOrders" :key="order.id" class="order-receipt" @click="goToDetail(order.id)">
        <view class="receipt-head">
          <view>
            <text class="store-name">CozyCoffee 中心店</text>
            <text class="order-meta">{{ formatFullTime(order.createdAt) }} · <text v-if="order.pickupCode && !isDeliveryOrder(order)" class="meta-em">取餐码 {{ order.pickupCode }}</text></text>
          </view>
          <view class="status-block">
            <text class="order-status" :class="statusBadgeClass(order)">{{ statusBadgeText(order) }}</text>
            <text v-if="isPending(order.status)" class="expire-countdown" :class="{ urgent: isExpiringSoon(order) }">
              {{ formatCountdown(order) }}
            </text>
          </view>
        </view>

        <view class="receipt-items">
          <view v-for="item in order.items" :key="item.id || `${item.productName}-${item.spec}`" class="order-item">
            <view class="item-image"><image v-if="item.productImage" :src="item.productImage" class="item-img" mode="aspectFill" /></view>
            <view class="item-info">
              <text class="item-name">{{ item.productName }}</text>
              <text class="item-spec">{{ item.spec || '默认规格' }}</text>
            </view>
            <view class="item-right">
              <text class="item-price">¥{{ money(item.itemAmount ?? Number(item.unitPrice || 0) * Number(item.quantity || 1)) }}</text>
              <text class="item-qty">× {{ item.quantity }}</text>
            </view>
          </view>
        </view>

        <view class="receipt-summary">
          <text class="summary-label">共 {{ order.totalQty }} 件 · {{ fulfillmentLabel(order) }}</text>
          <view class="summary-actions">
            <template v-if="isPending(order.status)">
              <view class="mini-btn md" @click.stop="cancelOrderItem(order)">取消订单</view>
              <view class="mini-btn md strong" @click.stop="payOrderItem(order)">立即支付</view>
              <view class="mini-btn md" @click.stop="goToDetail(order.id)">查看详情</view>
            </template>
            <template v-else-if="canConfirmOrder(order)">
              <view class="order-action" @click.stop="confirmOrderItem(order)">{{ confirmActionText(order) }}</view>
            </template>
            <template v-else-if="isCompleted(order.status) || isCancelled(order.status)">
              <view class="order-action" @click.stop="reOrder(order)">再来一单</view>
            </template>
            <template v-else>
              <view class="mini-btn" @click.stop="goToDetail(order.id)">查看详情</view>
            </template>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <view class="empty-cup" aria-hidden="true">
        <view class="empty-cup__body" />
        <view class="empty-cup__handle" />
      </view>
      <text class="empty-title">暂无{{ currentCategory === 'pickup' ? '自提' : '外送' }}订单</text>
      <text class="empty-desc">选择一杯喜欢的咖啡，<br>我们会为你现制</text>
      <view class="empty-action" @click="goToMenu">去点单</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onUnload, onPullDownRefresh } from '@dcloudio/uni-app'
import { getOrderList, acceptOrder as acceptOrderApi, cancelOrder as cancelOrderApi, confirmOrder as confirmOrderApi } from '@/api/order'
import { formatCoffeeSpec } from '@/utils/spec'
import { formatFullTime, money } from '@/utils/format'
import { useCartStore } from '@/stores/cart'
import { restoreOrderToCart } from '@/services/order/ReorderService'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'

const categories = [
  { value: 'pickup', label: '自提' },
  { value: 'delivery', label: '外送' }
]

const currentCategory = ref('pickup')
const coffeeOrders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const nowTs = ref(Date.now())
const cartStore = useCartStore()
const reorderingOrderId = ref('')
let ticker = null

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

onLoad(options => {
  const cat = options?.category
  if (categories.some(c => c.value === cat)) currentCategory.value = cat
})

onShow(() => {
  const saved = uni.getStorageSync('cozy_order_category')
  if (categories.some(c => c.value === saved)) currentCategory.value = saved
  if (saved) uni.removeStorageSync('cozy_order_category')
  startTicker()
  loadCurrent()
})

onUnload(stopTicker)

onPullDownRefresh(async () => {
  await loadCurrent(true)
  uni.stopPullDownRefresh()
})

const shownOrders = computed(() => coffeeOrders.value.filter(order => {
  const isDelivery = String(order.diningMethod || '').toUpperCase() === 'DELIVERY'
  return currentCategory.value === 'delivery' ? isDelivery : !isDelivery
}))

const counts = computed(() => {
  const delivery = coffeeOrders.value.filter(o => String(o.diningMethod || '').toUpperCase() === 'DELIVERY').length
  return { pickup: coffeeOrders.value.length - delivery, delivery }
})

const loadingText = computed(() =>
  currentCategory.value === 'pickup' ? '正在读取自提订单…' : '正在读取外送订单…'
)

function switchCategory(value) {
  if (currentCategory.value === value) return
  currentCategory.value = value
}

async function loadCurrent(silent = false) {
  if (!silent) loading.value = true
  errorMessage.value = ''
  try {
    const response = await getOrderList()
    if (response.code === 200 && response.data) {
      coffeeOrders.value = response.data.map(order => ({
        ...order,
        totalQty: order.totalQuantity || 0,
        items: (order.items || []).map(item => ({
          ...item,
          spec: formatCoffeeSpec(item)
        }))
      }))
    } else {
      errorMessage.value = response?.message || '订单加载失败，请稍后重试'
    }
  } catch (error) {
    errorMessage.value = error?.message || '订单加载失败，请稍后重试'
  } finally {
    if (!silent) loading.value = false
  }
}

function startTicker() {
  stopTicker()
  ticker = setInterval(() => { nowTs.value = Date.now() }, 1000)
}

function stopTicker() {
  if (!ticker) return
  clearInterval(ticker)
  ticker = null
}

function normalizeStatus(status) { return String(status || '').toLowerCase() }
function isPending(status) { return ['pending', 'pending_payment'].includes(normalizeStatus(status)) }
function isCompleted(status) { return normalizeStatus(status) === 'completed' }
function isCancelled(status) { return ['cancelled', 'canceled'].includes(normalizeStatus(status)) }
function isDeliveryOrder(order) { return String(order.diningMethod || '').toUpperCase() === 'DELIVERY' }

// 待确认取餐/收货：自提 completed 未发奖、外送 delivering 未发奖
function rewardsGrantedOf(order) { return order.rewardsGranted === true || order.rewardsGranted === 1 }
function canConfirmOrder(order) {
  if (rewardsGrantedOf(order)) return false
  return isDeliveryOrder(order) ? isCompleted(order.status) || normalizeStatus(order.status) === 'delivering'
                                : isCompleted(order.status)
}
function confirmActionText(order) { return isDeliveryOrder(order) ? '确认收货' : '确认取餐' }

async function confirmOrderItem(order) {
  const isDeliver = isDeliveryOrder(order)
  try {
    await confirmOrderApi(order.id)
    uni.showToast({ title: isDeliver ? '确认收货成功，积分已到账' : '确认取餐成功，积分已到账', icon: 'none', duration: 2000 })
    loadCurrent(true)
  } catch (e) {
    uni.showToast({ title: e?.message || '确认失败，请稍后重试', icon: 'none' })
  }
}

function getExpireMs(order) {
  if (order?.expireAt) {
    const timestamp = new Date(order.expireAt).getTime()
    if (!Number.isNaN(timestamp)) return timestamp
  }
  if (order?.createdAt) {
    const createdTimestamp = new Date(order.createdAt).getTime()
    if (!Number.isNaN(createdTimestamp)) return createdTimestamp + 15 * 60 * 1000
  }
  return null
}

function getRemainingSeconds(order) {
  const expireMs = getExpireMs(order)
  if (!expireMs) return null
  return Math.max(0, Math.floor((expireMs - nowTs.value) / 1000))
}

function formatCountdown(order) {
  const remain = getRemainingSeconds(order)
  if (remain == null) return '等待支付'
  if (remain <= 0) return '即将自动取消'
  const minutes = String(Math.floor(remain / 60)).padStart(2, '0')
  const seconds = String(remain % 60).padStart(2, '0')
  return `剩余 ${minutes}:${seconds}`
}

function isExpiringSoon(order) {
  const remain = getRemainingSeconds(order)
  return remain != null && remain <= 30
}

function getStatusText(status) {
  return ({
    pending: '待支付', pending_payment: '待支付', preparing: '制作中', processing: '制作中',
    delivering: '配送中', completed: '已完成', cancelled: '已取消', canceled: '已取消'
  })[normalizeStatus(status)] || '已提交'
}

function statusClass(status) {
  const normalized = normalizeStatus(status)
  if (['preparing', 'processing', 'delivering'].includes(normalized)) return 'processing'
  if (['cancelled', 'canceled'].includes(normalized)) return 'cancelled'
  if (normalized === 'completed') return 'completed'
  return 'pending'
}

// 出餐后未确认的自提单 → 高亮「待取餐」引导点击确认取餐
function statusBadgeText(order) {
  if (canConfirmOrder(order) && !isDeliveryOrder(order)) return '待取餐'
  return getStatusText(order.status)
}
function statusBadgeClass(order) {
  if (canConfirmOrder(order) && !isDeliveryOrder(order)) return 'await-confirm'
  return statusClass(order.status)
}

function fulfillmentLabel(order) {
  return isDeliveryOrder(order) ? '外送' : '到店自提'
}

function goToMenu() {
  uni.switchTab({
    url: '/pages/menu/menu',
    fail: () => uni.showToast({ title: '跳转失败，请重试', icon: 'none' })
  })
}
function goToDetail(orderId) { uni.navigateTo({ url: `/pages/order/detail?id=${encodeURIComponent(orderId)}` }) }

// 待支付订单：列表直接支付/取消，无需进详情
async function payOrderItem(order) {
  const amount = money(order.payAmount ?? order.totalAmount)
  const confirmed = await new Promise(resolve => {
    uni.showModal({
      title: '模拟支付',
      content: `订单金额 ¥${amount}，确认模拟支付？`,
      confirmText: '确认支付',
      cancelText: '取消',
      success: r => resolve(r.confirm)
    })
  })
  if (!confirmed) return
  uni.showLoading({ title: '正在支付', mask: true })
  try {
    await acceptOrderApi(order.id)
    uni.hideLoading()
    uni.showToast({ title: '支付成功，商家已接单', icon: 'none', duration: 2200 })
    loadCurrent()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e?.message || '支付失败，请稍后重试', icon: 'none' })
  }
}

async function cancelOrderItem(order) {
  const confirmed = await new Promise(resolve => {
    uni.showModal({
      title: '取消订单',
      content: '确定取消该订单吗？如使用优惠券会退回卡包',
      confirmText: '确认取消',
      cancelText: '再想想',
      success: r => resolve(r.confirm)
    })
  })
  if (!confirmed) return
  try {
    await cancelOrderApi(order.id)
    uni.showToast({ title: '订单已取消', icon: 'none' })
    loadCurrent()
  } catch (e) {
    uni.showToast({ title: e?.message || '取消失败，请稍后重试', icon: 'none' })
  }
}

async function reOrder(order) {
  if (reorderingOrderId.value) return
  reorderingOrderId.value = String(order?.id || '')
  uni.showLoading({ title: '正在恢复商品', mask: true })
  try {
    const result = await restoreOrderToCart({ order, cartStore })
    uni.hideLoading()
    if (!result.restoredQuantity) {
      uni.showToast({ title: '原订单商品均已下架，请重新选择', icon: 'none', duration: 2200 })
      setTimeout(() => openRestoredCart(), 500)
      return
    }

    const notices = []
    if (result.invalidItems.length) notices.push(`${result.invalidItems.length} 款商品已下架，未加入购物车`)
    if (result.adjustedItems.length) notices.push(`${result.adjustedItems.length} 款商品规格已按当前菜单调整`)

    openRestoredCart()
    if (notices.length) {
      uni.showToast({ title: `已恢复 ${result.restoredQuantity} 件商品 · ${notices[0]}`, icon: 'none', duration: 2200 })
    } else {
      uni.showToast({ title: `已恢复 ${result.restoredQuantity} 件商品`, icon: 'none' })
    }
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '恢复商品失败，请稍后重试', icon: 'none', duration: 2200 })
  } finally {
    reorderingOrderId.value = ''
  }
}

function openRestoredCart() {
  uni.setStorageSync('cozy_open_cart_on_menu', '1')
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>

<style lang="scss" scoped>
.order-page { min-height: 100vh; padding: 0 0 240rpx; background: $cozy-surface; }

/* ── 顶部白色区（胶囊下方留白 + 分类切换 + 分隔线） ── */
.order-top { background: #fff; }

/* ── 分类切换 ── */
.category-switch {
  display: flex;
  height: 96rpx;
  background: $bg-white;
  border-bottom: 1rpx solid $cozy-border;
}
.category-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-muted;
  position: relative;
}
.category-item.active { color: $cozy-primary; font-weight: 700; }
.category-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 64rpx;
  height: 4rpx;
  border-radius: 2rpx;
  background: $cozy-primary;
}
.category-count {
  min-width: 32rpx;
  padding: 0 8rpx;
  height: 32rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: $cozy-surface;
  font-size: 20rpx;
  font-weight: 700;
  color: $cozy-muted;
}

.order-list { padding: 32rpx 24rpx 0; }

/* ── 订单收据卡 ── */
.order-receipt {
  margin-bottom: 24rpx;
  overflow: hidden;
  border: 1rpx solid $cozy-border;
  border-radius: 24rpx;
  background: $bg-white;
}
.receipt-head {
  padding: 28rpx 28rpx 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.store-name { display: block; font-size: 28rpx; font-weight: 650; color: $cozy-ink; }
.order-meta { display: block; margin-top: 6rpx; font-size: 20rpx; color: $cozy-muted; }
.meta-em { font-style: normal; color: $cozy-primary; font-weight: 650; }
.status-block { flex: none; text-align: right; }
.order-status {
  display: inline-block;
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 700;
}
.order-status.processing { background: #EAF0F2; color: #285B70; }
.order-status.completed { background: #E6F1EB; color: #059669; }
.order-status.pending { background: #F4EBDD; color: #8A4D10; }
.order-status.cancelled { background: $cozy-border; color: $cozy-muted; }
.order-status.await-confirm { background: $cozy-primary; color: #fff; }
.expire-countdown {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: $cozy-muted;
}
.expire-countdown.urgent { color: #9B3932; font-weight: 600; }

.receipt-items { padding: 4rpx 28rpx; }
.order-item {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
}
.item-image {
  flex: none;
  width: 112rpx;
  height: 112rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #E8DDD2, #D8C8B4);
  overflow: hidden;
}
.item-img { width: 100%; height: 100%; }
.item-info { flex: 1; min-width: 0; }
.item-name { display: block; font-size: 26rpx; font-weight: 650; color: $cozy-ink; }
.item-spec { display: block; margin-top: 6rpx; font-size: 20rpx; color: $cozy-muted; }
.item-right { flex: none; text-align: right; }
.item-price { display: block; font-size: 26rpx; font-weight: 650; color: $cozy-ink; }
.item-qty { display: block; margin-top: 6rpx; font-size: 20rpx; color: $cozy-muted; }

.receipt-summary {
  padding: 24rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24rpx;
  background: $bg-white;
}
.summary-label { flex: 1; min-width: 0; font-size: 22rpx; color: $cozy-muted; }
.summary-actions { flex: none; display: flex; align-items: center; gap: 12rpx; }
.order-action {
  flex: none;
  padding: 16rpx 28rpx;
  border: 1rpx solid $cozy-ink;
  border-radius: 16rpx;
  background: transparent;
  color: $cozy-ink;
  font-size: 24rpx;
  font-weight: 650;

  &:active { opacity: .85; }
}
.mini-btn {
  flex: none;
  padding: 14rpx 28rpx;
  border: 1rpx solid $cozy-border;
  border-radius: 12rpx;
  background: $bg-white;
  color: $cozy-ink;
  font-size: 24rpx;
  font-weight: 600;

  &:active { background: $cozy-surface; }
}
.mini-btn.md { padding: 10rpx 20rpx; font-size: 22rpx; }
.mini-btn.strong { border-color: $cozy-ink; background: transparent; color: $cozy-ink; font-weight: 650; }
.mini-btn.strong:active { opacity: .85; }

/* ── 空状态 ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 360rpx 80rpx 120rpx;
  text-align: center;
}
.empty-cup { position: relative; width: 100rpx; height: 76rpx; }
.empty-cup__body {
  position: absolute;
  left: 6rpx;
  bottom: 6rpx;
  width: 70rpx;
  height: 56rpx;
  border: 5rpx solid $cozy-muted;
  border-radius: 0 0 16rpx 16rpx;
  box-sizing: border-box;
}
.empty-cup__handle {
  position: absolute;
  right: 2rpx;
  top: 10rpx;
  width: 22rpx;
  height: 30rpx;
  border: 5rpx solid $cozy-muted;
  border-left: none;
  border-radius: 0 16rpx 16rpx 0;
  box-sizing: border-box;
}
.empty-title {
  margin-top: 40rpx;
  font-family: $font-display;
  font-size: 44rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.empty-desc { margin-top: 20rpx; font-size: 26rpx; line-height: 1.7; color: $cozy-muted; }
.empty-action {
  margin-top: 56rpx;
  padding: 16rpx 64rpx;
  border-radius: 999rpx;
  background: $cozy-ink;
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
  line-height: 1.4;

  &:active { opacity: .8; }
}
</style>
