<!-- 订单页：咖啡订单与积分兑换订单两类，收据式层级呈现。 -->
<template>
  <view class="order-page">
    <view class="order-intro">
      <text class="intro-brand">COZY ORDERS</text>
      <text class="intro-title cozy-display">每一杯，都有清楚进度</text>
      <text class="intro-copy">查看咖啡订单进度与积分兑换记录。</text>
    </view>

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

    <LoadingState v-if="loading && !currentOrders.length" :text="loadingText" />
    <RetryState v-else-if="errorMessage && !currentOrders.length" :description="errorMessage" @retry="loadCurrent" />

    <view v-else-if="currentOrders.length" class="order-list">
      <!-- 咖啡订单 -->
      <template v-if="currentCategory === 'coffee'">
        <view v-for="order in currentOrders" :key="order.id" class="order-receipt" @click="goToDetail(order.id)">
          <view class="receipt-head">
            <view>
              <text class="store-name">CozyCoffee 中心店</text>
              <text class="order-no">订单 {{ order.orderNo || order.id }}</text>
            </view>
            <view class="status-block">
              <text class="order-status" :class="statusClass(order.status)">{{ getStatusText(order.status) }}</text>
              <text v-if="isPending(order.status)" class="expire-countdown" :class="{ urgent: isExpiringSoon(order) }">
                {{ formatCountdown(order) }}
              </text>
            </view>
          </view>

          <view class="receipt-items">
            <view v-for="item in order.items" :key="item.id || `${item.name}-${item.spec}`" class="order-item">
              <image :src="item.image" class="item-image" mode="aspectFill" />
              <view class="item-info">
                <text class="item-name">{{ item.name }}</text>
                <text class="item-spec">{{ item.spec || '默认规格' }}</text>
              </view>
              <view class="item-right">
                <text class="item-price">¥{{ money(item.price) }}</text>
                <text class="item-qty">× {{ item.quantity }}</text>
              </view>
            </view>
          </view>

          <view class="receipt-summary">
            <view>
              <text class="summary-label">共 {{ order.totalQty }} 件 · 到店自提</text>
              <text class="summary-total">实付 ¥{{ money(order.totalPrice) }}</text>
            </view>
            <view class="order-action" v-if="normalizeStatus(order.status) === 'completed'" @click.stop="reOrder(order)">再次点单</view>
            <text v-else class="detail-link">查看详情 →</text>
          </view>
        </view>
      </template>

      <!-- 兑换订单 -->
      <template v-else>
        <view v-for="order in currentOrders" :key="order.id" class="order-receipt" @click="goToRedemptionDetail(order.id)">
          <view class="receipt-head">
            <view>
              <text class="store-name">积分兑换</text>
              <text class="order-no">订单 {{ order.orderNo || order.id }}</text>
            </view>
            <text class="order-status" :class="redemptionStatusClass(order.status)">{{ getRedemptionStatusText(order.status) }}</text>
          </view>

          <view class="receipt-items">
            <view class="order-item">
              <image :src="order.productImage ? order.productImage : '/static/images/default-product.png'" class="item-image" mode="aspectFill" />
              <view class="item-info">
                <text class="item-name">{{ order.productName }}</text>
                <text class="item-spec">数量 × {{ order.quantity || 1 }}</text>
              </view>
              <view class="item-right">
                <text class="item-points">{{ order.pointsCost }} 积分</text>
              </view>
            </view>
          </view>

          <view class="receipt-summary">
            <view>
              <text class="summary-label">{{ formatRedeemTime(order.createdAt) }}</text>
              <text v-if="order.fulfillmentType === 'VIRTUAL'" class="summary-total virtual-tag">已发放至券包</text>
              <text v-else class="summary-total">{{ order.receiverName }} · {{ order.receiverPhone }}</text>
            </view>
            <text class="detail-link">查看详情 →</text>
          </view>
        </view>
      </template>
    </view>

    <EmptyState
      v-else
      :icon="currentCategory === 'coffee' ? '杯' : '礼'"
      :title="currentCategory === 'coffee' ? '还没有咖啡订单' : '还没有兑换记录'"
      :description="currentCategory === 'coffee' ? '选择一杯喜欢的咖啡，门店会为你现制。' : '去积分商城兑换心仪礼品。'"
      :action-text="currentCategory === 'coffee' ? '去点单' : '去积分商城'"
      @action="currentCategory === 'coffee' ? goToMenu() : goToMall()"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderList } from '@/api/order'
import { getMyRedemptions } from '@/api/member'
import { useCartStore } from '@/stores/cart'
import { restoreOrderToCart } from '@/services/order/ReorderService'
import LoadingState from '@/components/states/LoadingState.vue'
import EmptyState from '@/components/states/EmptyState.vue'
import RetryState from '@/components/states/RetryState.vue'

const categories = [
  { value: 'coffee', label: '咖啡订单' },
  { value: 'redeem', label: '兑换订单' }
]

const currentCategory = ref('coffee')
const coffeeOrders = ref([])
const redeemOrders = ref([])
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

onShow(() => {
  const saved = uni.getStorageSync('cozy_order_category')
  if (categories.some(c => c.value === saved)) currentCategory.value = saved
  if (saved) uni.removeStorageSync('cozy_order_category')
  startTicker()
  loadCurrent()
})

onHide(stopTicker)
onUnload(stopTicker)

const currentOrders = computed(() =>
  currentCategory.value === 'coffee' ? coffeeOrders.value : redeemOrders.value
)

const counts = computed(() => ({
  coffee: coffeeOrders.value.length,
  redeem: redeemOrders.value.length
}))

const loadingText = computed(() =>
  currentCategory.value === 'coffee' ? '正在读取咖啡订单…' : '正在读取兑换订单…'
)

function switchCategory(value) {
  if (currentCategory.value === value) return
  currentCategory.value = value
  loadCurrent()
}

async function loadCurrent() {
  loading.value = true
  errorMessage.value = ''
  try {
    if (currentCategory.value === 'coffee') {
      await loadCoffeeOrders()
    } else {
      await loadRedeemOrders()
    }
  } finally {
    loading.value = false
  }
}

async function loadCoffeeOrders() {
  try {
    const response = await getOrderList()
    if (response.code === 200 && response.data) {
      coffeeOrders.value = response.data.map(order => ({
        ...order,
        totalPrice: order.payAmount || order.totalAmount,
        totalQty: order.totalQuantity || 0,
        items: (order.items || []).map(item => ({
          ...item,
          name: item.productName,
          price: item.unitPrice,
          image: item.productImage || '/static/images/default-product.png',
          spec: [item.cupSize, item.temperature, item.sugarLevel].filter(Boolean).join(' · ')
        }))
      }))
    }
  } catch (error) {
    errorMessage.value = error?.message || '订单加载失败，请稍后重试'
  }
}

async function loadRedeemOrders() {
  try {
    const response = await getMyRedemptions()
    if (response.code === 200 && response.data) {
      redeemOrders.value = response.data
    }
  } catch (error) {
    errorMessage.value = error?.message || '兑换记录加载失败，请稍后重试'
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

function getRedemptionStatusText(status) {
  return ({
    pending: '待处理', processing: '处理中', completed: '已完成',
    cancelled: '已取消', canceled: '已取消', issued: '已发放'
  })[normalizeStatus(status)] || '已提交'
}

function redemptionStatusClass(status) {
  const normalized = normalizeStatus(status)
  if (['completed', 'issued'].includes(normalized)) return 'completed'
  if (['cancelled', 'canceled'].includes(normalized)) return 'cancelled'
  if (normalized === 'processing') return 'processing'
  return 'pending'
}

function formatRedeemTime(createdAt) {
  if (!createdAt) return ''
  const d = new Date(createdAt)
  if (Number.isNaN(d.getTime())) return String(createdAt)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function money(value) { return Number(value || 0).toFixed(2) }
function goToMenu() { uni.switchTab({ url: '/pages/menu/menu' }) }
function goToMall() { uni.navigateTo({ url: '/pages/mall/index' }) }
function goToDetail(orderId) { uni.navigateTo({ url: `/pages/order/detail?id=${encodeURIComponent(orderId)}` }) }
function goToRedemptionDetail(orderId) { uni.navigateTo({ url: `/pages/mall/index?orderId=${encodeURIComponent(orderId)}` }) }
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
.order-intro { padding: 34rpx 28rpx 28rpx; background: #fff; }
.intro-brand { display: block; color: $cozy-primary; font-size: 18rpx; font-weight: 800; letter-spacing: .14em; }
.intro-title { display: block; margin-top: 12rpx; color: $cozy-ink; font-size: 39rpx; }
.intro-copy { display: block; margin-top: 9rpx; color: $cozy-muted; font-size: 21rpx; }

/* 一级分类切换：咖啡订单 / 兑换订单 */
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
.order-no { display: block; margin-top: 7rpx; color: $cozy-muted; font-size: 19rpx; }
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
.item-points { display: block; color: $cozy-primary; font-size: 26rpx; font-weight: 750; }
.receipt-summary { padding: 22rpx 26rpx 25rpx; display: flex; align-items: flex-end; justify-content: space-between; gap: 20rpx; background: $cozy-surface; }
.summary-label { display: block; color: $cozy-muted; font-size: 19rpx; }
.summary-total { display: block; margin-top: 7rpx; color: $cozy-primary; font-size: 26rpx; font-weight: 750; }
.summary-total.virtual-tag { color: $cozy-accent; }
.order-action { min-height: 66rpx; padding: 0 20rpx; display: flex; align-items: center; border: 1rpx solid $cozy-primary; border-radius: $cozy-radius-md; color: $cozy-primary; font-size: 21rpx; font-weight: 650; }
.detail-link { padding-bottom: 9rpx; color: $cozy-primary; font-size: 20rpx; font-weight: 650; }
</style>
