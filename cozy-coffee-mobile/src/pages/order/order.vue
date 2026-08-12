<!--
  订单页 - 复现 prototype/order.html：自取/外送分类，仅咖啡订单
  每单: 时间+取餐码/外送地址 + 状态 + 商品 + 再来一单
-->
<template>
  <view class="order-page" :style="{ paddingTop: statusBarHeight + 44 + 'px' }">
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

    <LoadingState v-if="loading && !coffeeOrders.length" :text="loadingText" />
    <RetryState v-else-if="errorMessage && !coffeeOrders.length" :description="errorMessage" @retry="loadCurrent" />

    <view v-else-if="shownOrders.length" class="order-list">
      <view v-for="order in shownOrders" :key="order.id" class="order-receipt" @click="goToDetail(order.id)">
        <view class="receipt-head">
          <view>
            <text class="store-name">CozyCoffee 中心店</text>
            <text class="order-meta">{{ formatTime(order.createdAt) }} · {{ fulfillmentMeta(order) }}</text>
          </view>
          <view class="status-block">
            <text class="order-status" :class="statusClass(order.status)">{{ getStatusText(order.status) }}</text>
            <text v-if="isPending(order.status)" class="expire-countdown" :class="{ urgent: isExpiringSoon(order) }">
              {{ formatCountdown(order) }}
            </text>
          </view>
        </view>

        <view class="receipt-items">
          <view v-for="item in order.items" :key="item.id || `${item.productName}-${item.spec}`" class="order-item">
            <image :src="item.productImage || '/static/images/default-product.png'" class="item-image" mode="aspectFill" />
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
            <view v-if="isCompleted(order.status)" class="order-action" @click.stop="reOrder(order)">再来一单</view>
            <text v-else class="detail-link">查看详情 →</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="editorial-empty">
      <CozyIcon name="coffee" :size="48" color="#8B6958" class="empty-icon" />
      <text class="empty-title">暂无{{ currentCategory === 'pickup' ? '自取' : '外送' }}订单</text>
      <text class="empty-desc">选择一杯喜欢的咖啡，<br>我们会为你现制。</text>
      <view class="empty-action" @click="goToMenu">去点单 →</view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderList } from '@/api/order'
import { useCartStore } from '@/stores/cart'
import { restoreOrderToCart } from '@/services/order/ReorderService'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'
import CozyIcon from '@/components/CozyIcon.vue'

const categories = [
  { value: 'pickup', label: '自取' },
  { value: 'delivery', label: '外送' }
]

const currentCategory = ref('pickup')
const statusBarHeight = ref(20)
const coffeeOrders = ref([])
const loading = ref(false)
const errorMessage = ref('')
const nowTs = ref(Date.now())
const cartStore = useCartStore()
const reorderingOrderId = ref('')
let ticker = null

onLoad(options => {
  const cat = options?.category
  if (categories.some(c => c.value === cat)) currentCategory.value = cat
})

onMounted(() => {
  statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
})

onShow(() => {
  const saved = uni.getStorageSync('cozy_order_category')
  if (categories.some(c => c.value === saved)) currentCategory.value = saved
  if (saved) uni.removeStorageSync('cozy_order_category')
  startTicker()
  loadCurrent()
})

onHide(stopTicker)
onUnload(stopTicker)

const shownOrders = computed(() => coffeeOrders.value.filter(order => {
  const isDelivery = String(order.diningMethod || '').toUpperCase() === 'DELIVERY'
  return currentCategory.value === 'delivery' ? isDelivery : !isDelivery
}))

const counts = computed(() => {
  const delivery = coffeeOrders.value.filter(o => String(o.diningMethod || '').toUpperCase() === 'DELIVERY').length
  return { pickup: coffeeOrders.value.length - delivery, delivery }
})

const loadingText = computed(() =>
  currentCategory.value === 'pickup' ? '正在读取自取订单…' : '正在读取外送订单…'
)

function switchCategory(value) {
  if (currentCategory.value === value) return
  currentCategory.value = value
}

async function loadCurrent() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getOrderList()
    if (response.code === 200 && response.data) {
      coffeeOrders.value = response.data.map(order => ({
        ...order,
        totalQty: order.totalQuantity || 0,
        items: (order.items || []).map(item => ({
          ...item,
          spec: [item.cupSize, item.temperature, item.sugarLevel].filter(Boolean).join(' · ')
        }))
      }))
    } else {
      errorMessage.value = response?.message || '订单加载失败，请稍后重试'
    }
  } catch (error) {
    errorMessage.value = error?.message || '订单加载失败，请稍后重试'
  } finally {
    loading.value = false
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
function isDeliveryOrder(order) { return String(order.diningMethod || '').toUpperCase() === 'DELIVERY' }

function getExpireMs(order) {
  if (order?.expireAt) {
    const timestamp = new Date(order.expireAt).getTime()
    if (!Number.isNaN(timestamp)) return timestamp
  }
  if (order?.createdAt) {
    const createdTimestamp = new Date(order.createdAt).getTime()
    if (!Number.isNaN(createdTimestamp)) return createdTimestamp + 60 * 1000
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
  if (remain == null) return '等待门店确认'
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
    pending: '等待接单', pending_payment: '待处理', preparing: '制作中', processing: '制作中',
    completed: '已完成', cancelled: '已取消', canceled: '已取消'
  })[normalizeStatus(status)] || '已提交'
}

function statusClass(status) {
  const normalized = normalizeStatus(status)
  if (['preparing', 'processing'].includes(normalized)) return 'processing'
  if (['cancelled', 'canceled'].includes(normalized)) return 'cancelled'
  if (normalized === 'completed') return 'completed'
  return 'pending'
}

function fulfillmentMeta(order) {
  if (isDeliveryOrder(order)) return '外送'
  return order.pickupCode ? `取餐码 <em class="pickup-em">${order.pickupCode}</em>` : '到店自提'
}

function fulfillmentLabel(order) {
  return isDeliveryOrder(order) ? '外送' : '到店自提'
}

function formatTime(value) {
  if (!value) return '--'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ').slice(0, 19)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function money(value) { return Number(value || 0).toFixed(2) }
function goToMenu() { uni.switchTab({ url: '/pages/menu/menu' }) }
function goToDetail(orderId) { uni.navigateTo({ url: `/pages/order/detail?id=${encodeURIComponent(orderId)}` }) }

async function reOrder(order) {
  if (reorderingOrderId.value) return
  reorderingOrderId.value = String(order?.id || '')
  uni.showLoading({ title: '正在恢复商品', mask: true })
  try {
    const result = await restoreOrderToCart({ order, cartStore })
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
    reorderingOrderId.value = ''
  }
}

function openRestoredCart() {
  uni.setStorageSync('cozy_open_cart_on_menu', '1')
  uni.switchTab({ url: '/pages/menu/menu' })
}
</script>

<style lang="scss" scoped>
.order-page { min-height: 100vh; padding-bottom: 140rpx; background: $cozy-surface; }

/* 分类切换：自取 / 外送 */
.category-switch {
  position: sticky; top: 0; z-index: 10;
  display: flex; height: 96rpx; background: #fff;
  border-top: 1rpx solid $cozy-border; border-bottom: 1rpx solid $cozy-border;
}
.category-item {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 10rpx;
  color: $cozy-muted; font-size: 25rpx; font-weight: 600;
  position: relative; transition: color $cozy-duration $cozy-ease-out;
}
.category-item.active { color: $cozy-primary; font-weight: 720; }
.category-item.active::after {
  content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 64rpx; height: 4rpx; border-radius: 2rpx; background: $cozy-primary;
}
.category-count {
  min-width: 32rpx; height: 32rpx; padding: 0 8rpx; display: inline-flex;
  align-items: center; justify-content: center; border-radius: 999rpx;
  background: $cozy-surface; color: $cozy-muted; font-size: 18rpx; font-weight: 700;
}
.category-item.active .category-count { background: $cozy-accent-soft; color: $cozy-accent; }

.order-list { padding: 24rpx; }
.order-receipt { margin-bottom: 22rpx; overflow: hidden; border-radius: $cozy-radius-lg; background: #fff; }
.receipt-head { padding: 26rpx 26rpx 22rpx; display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; border-bottom: 1rpx solid $cozy-border; }
.store-name { display: block; color: $cozy-ink; font-size: 27rpx; font-weight: 680; }
.order-meta { display: block; margin-top: 7rpx; color: $cozy-muted; font-size: 19rpx; }
.pickup-em { font-style: normal; color: $cozy-primary; font-weight: 650; }
.status-block { flex: none; display: flex; flex-direction: column; align-items: flex-end; gap: 8rpx; }
.order-status { padding: 6rpx 13rpx; border-radius: 999rpx; font-size: 19rpx; font-weight: 700; }
.order-status.pending { background: $cozy-warning-soft; color: #8a4d10; }
.order-status.processing { background: $cozy-info-soft; color: #285b70; }
.order-status.completed { background: $cozy-accent-soft; color: $cozy-accent; }
.order-status.cancelled { background: $cozy-error-soft; color: #9b3932; }
.expire-countdown { color: $cozy-muted; font-size: 18rpx; }
.expire-countdown.urgent { color: $cozy-error; font-weight: 650; }
.receipt-items { padding: 6rpx 26rpx; }
.order-item { display: flex; align-items: center; gap: 18rpx; padding: 20rpx 0; border-bottom: 1rpx solid $cozy-border; }
.order-item:last-child { border-bottom: 0; }
.item-image { width: 106rpx; height: 106rpx; flex: none; border-radius: $cozy-radius-md; background: $cozy-surface; }
.item-info { min-width: 0; flex: 1; }
.item-name { display: block; overflow: hidden; color: $cozy-ink; font-size: 25rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.item-spec { display: block; margin-top: 7rpx; overflow: hidden; color: $cozy-muted; font-size: 20rpx; text-overflow: ellipsis; white-space: nowrap; }
.item-right { flex: none; text-align: right; }
.item-price { display: block; color: $cozy-ink; font-size: 24rpx; font-weight: 650; }
.item-qty { display: block; margin-top: 7rpx; color: $cozy-muted; font-size: 19rpx; }
.receipt-summary { padding: 22rpx 26rpx 25rpx; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; background: $cozy-surface; }
.summary-label { color: $cozy-muted; font-size: 19rpx; }
.summary-actions { flex: none; display: flex; align-items: center; }
.order-action { min-height: 66rpx; padding: 0 20rpx; display: flex; align-items: center; border: 1rpx solid $cozy-primary; border-radius: $cozy-radius-md; color: $cozy-primary; font-size: 21rpx; font-weight: 650; }
.detail-link { padding-bottom: 9rpx; color: $cozy-primary; font-size: 20rpx; font-weight: 650; }

/* 编辑式空状态 */
.editorial-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 420rpx 60rpx 120rpx; text-align: center;
}
.empty-icon { margin-bottom: 32rpx; }
.empty-title { display: block; color: $cozy-ink; font-size: 34rpx; font-weight: 680; letter-spacing: 2rpx; }
.empty-desc { display: block; margin-top: 20rpx; color: $cozy-muted; font-size: 24rpx; line-height: 1.8; letter-spacing: 1rpx; }
.empty-action {
  margin-top: 44rpx; padding: 18rpx 48rpx;
  border: 1rpx solid #5B4033; border-radius: $cozy-radius-md;
  color: #5B4033; font-size: 24rpx; font-weight: 600;
  letter-spacing: 1rpx; transition: all $cozy-duration $cozy-ease-out;
}
</style>
