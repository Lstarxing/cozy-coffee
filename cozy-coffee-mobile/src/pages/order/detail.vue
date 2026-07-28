<template>
  <view class="detail-page">
    <LoadingState v-if="loading" text="正在加载订单…" />
    <RetryState v-else-if="errorMessage" :description="errorMessage" @retry="loadOrder" />
    <view v-else-if="order" class="detail-content">
      <view class="status-panel" :class="statusClass">
        <text class="status-title">{{ statusText(order.status) }}</text>
        <text class="status-description">{{ statusDescription(order.status) }}</text>
        <view v-if="order.pickupCode" class="pickup-code-row"><text>取餐码</text><text>{{ order.pickupCode }}</text></view>
      </view>

      <StoreSummary />

      <view class="detail-section">
        <view class="section-heading"><text>商品明细</text><text>{{ order.totalQuantity || itemCount }} 件</text></view>
        <view v-for="item in order.items || []" :key="item.id || item.productId" class="order-line">
          <image class="line-image" :src="item.productImage || '/static/images/default-product.png'" mode="aspectFill" />
          <view class="line-body"><text class="line-name">{{ item.productName }}</text><text class="line-spec">{{ itemSpec(item) }}</text><text class="line-qty">× {{ item.quantity }}</text></view>
          <text class="line-price">¥{{ money(item.itemAmount ?? Number(item.unitPrice || 0) * Number(item.quantity || 1)) }}</text>
        </view>
      </view>

      <view class="detail-section price-section">
        <view class="price-row"><text>商品金额</text><text>¥{{ money(order.totalAmount) }}</text></view>
        <view v-if="Number(order.discountAmount || 0)" class="price-row discount"><text>优惠</text><text>-¥{{ money(order.discountAmount) }}</text></view>
        <view class="price-row total"><text>实付</text><text>¥{{ money(order.payAmount ?? order.totalAmount) }}</text></view>
      </view>

      <view class="detail-section info-section">
        <view class="info-row"><text>订单号</text><text selectable>{{ order.orderNo }}</text></view>
        <view class="info-row"><text>下单时间</text><text>{{ formatTime(order.createdAt) }}</text></view>
        <view class="info-row"><text>取餐方式</text><text>到店自提</text></view>
        <view v-if="order.remark" class="info-row"><text>备注</text><text>{{ order.remark }}</text></view>
      </view>
      <view class="detail-spacer" />
    </view>

    <view v-if="canCancel || canReorder" class="detail-footer safe-area-bottom">
      <view v-if="canCancel" class="footer-button cancel-button" @click="confirmCancel">取消订单</view>
      <view v-if="canReorder" class="footer-button reorder-button" :class="{ disabled: reordering }" @click="reOrder">再来一单</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { cancelOrder, getOrderDetail } from '@/api/order'
import { useCartStore } from '@/stores/cart'
import { useSessionStore } from '@/stores/session'
import { restoreOrderToCart } from '@/services/order/ReorderService'
import { refreshMemberProfile } from '@/services/session/MemberProfileService'
import StoreSummary from '@/components/order/StoreSummary.vue'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'

const orderId = ref('')
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const reordering = ref(false)
const cartStore = useCartStore()
const sessionStore = useSessionStore()
const itemCount = computed(() => (order.value?.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const normalizedStatus = computed(() => String(order.value?.status || '').toLowerCase())
const canCancel = computed(() => ['pending', 'pending_payment'].includes(normalizedStatus.value))
const canReorder = computed(() => Boolean(order.value?.items?.length) && ['completed', 'cancelled', 'canceled'].includes(normalizedStatus.value))
const statusClass = computed(() => ({ completed: 'success', cancelled: 'muted', canceled: 'muted' })[normalizedStatus.value] || 'active')
let pollTimer = null
let memberRefreshTimer = null
let completedMemberRefreshed = false

onLoad(options => { orderId.value = options.id || options.orderId || '' })
onShow(() => { if (orderId.value) { loadOrder(); startPolling() } })
onHide(stopPolling)
onUnload(() => { stopPolling(); clearMemberRefreshTimer() })

async function loadOrder(silent = false) {
  if (!silent) loading.value = true
  errorMessage.value = ''
  try {
    const response = await getOrderDetail(orderId.value)
    order.value = response?.data ?? response
    refreshMemberWhenCompleted(order.value)
  }
  catch (error) { errorMessage.value = error?.message || '订单加载失败，请重试' }
  finally { if (!silent) loading.value = false }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(() => {
    if (!['completed', 'cancelled', 'canceled'].includes(normalizedStatus.value)) loadOrder(true)
    else stopPolling()
  }, 5000)
}
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

async function refreshMemberWhenCompleted(currentOrder) {
  if (String(currentOrder?.status || '').toLowerCase() !== 'completed' || completedMemberRefreshed) return
  completedMemberRefreshed = true
  try {
    await refreshMemberProfile(sessionStore)
  } catch (_) {
    completedMemberRefreshed = false
    return
  }

  clearMemberRefreshTimer()
  memberRefreshTimer = setTimeout(() => {
    refreshMemberProfile(sessionStore).catch(() => {})
  }, 1500)
}

function clearMemberRefreshTimer() {
  if (!memberRefreshTimer) return
  clearTimeout(memberRefreshTimer)
  memberRefreshTimer = null
}

function statusText(status) { return ({ pending: '等待门店接单', pending_payment: '订单待处理', preparing: '咖啡制作中', processing: '咖啡制作中', completed: '订单已完成', cancelled: '订单已取消' })[String(status || '').toLowerCase()] || '订单已提交' }
function statusDescription(status) { return ['preparing', 'processing'].includes(String(status).toLowerCase()) ? '咖啡师正在为你制作，请稍候' : '请留意订单状态，及时到店取餐' }
function money(value) { return Number(value || 0).toFixed(2) }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '--' }
function parseOptions(value) { try { return typeof value === 'string' ? JSON.parse(value) : (value || {}) } catch (_) { return {} } }
function itemSpec(item) {
  const options = parseOptions(item.optionsJson)
  return [item.cupSize, item.temperature, item.sugarLevel, options.milkType || item.milkType, item.coffeeStrength]
    .filter(Boolean)
    .join(' · ') || '默认规格'
}
function confirmCancel() { uni.showModal({ title: '取消订单', content: '确认取消这张订单？', success: async result => { if (!result.confirm) return; try { await cancelOrder(orderId.value); uni.showToast({ title: '订单已取消', icon: 'success' }); loadOrder() } catch (error) { uni.showToast({ title: error?.message || '取消失败', icon: 'none' }) } } }) }

async function reOrder() {
  if (reordering.value || !order.value) return
  reordering.value = true
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
.status-panel { margin-bottom: 20rpx; padding: 34rpx; border-radius: $cozy-radius-lg; color: #fff; text-align: center; }
.status-panel.active { background: $cozy-primary; }.status-panel.success { background: $cozy-accent; }.status-panel.muted { background: #756a63; }
.status-title { display: block; font-size: 36rpx; font-weight: 750; }.status-description { display: block; margin-top: 10rpx; color: rgba(255,255,255,.82); font-size: 22rpx; }
.pickup-code-row { margin-top: 24rpx; padding-top: 20rpx; display: flex; align-items: baseline; justify-content: center; gap: 18rpx; border-top: 1rpx solid rgba(255,255,255,.2); font-size: 23rpx; }.pickup-code-row text:last-child { font-size: 48rpx; font-weight: 800; letter-spacing: .12em; }
.detail-section { margin-top: 20rpx; padding: 28rpx; border-radius: $cozy-radius-lg; background: #fff; }
.section-heading { display: flex; justify-content: space-between; margin-bottom: 16rpx; color: $cozy-ink; font-size: 28rpx; font-weight: 700; }.section-heading text:last-child { color: $cozy-muted; font-size: 22rpx; font-weight: 400; }
.order-line { display: flex; gap: 16rpx; padding: 20rpx 0; border-bottom: 1rpx solid $cozy-border; }.order-line:last-child { border-bottom: 0; }
.line-image { width: 96rpx; height: 96rpx; flex: none; border-radius: $cozy-radius-md; background: $cozy-surface; }.line-body { min-width: 0; flex: 1; }.line-name { display: block; color: $cozy-ink; font-size: 25rpx; font-weight: 650; }.line-spec,.line-qty { display: block; margin-top: 6rpx; color: $cozy-muted; font-size: 20rpx; }.line-price { color: $cozy-ink; font-size: 24rpx; font-weight: 700; }
.price-row,.info-row { min-height: 62rpx; display: flex; align-items: center; justify-content: space-between; gap: 24rpx; color: $cozy-muted; font-size: 24rpx; }.price-row text:last-child,.info-row text:last-child { color: $cozy-ink; text-align: right; }.price-row.discount,.price-row.discount text:last-child { color: $cozy-primary; }.price-row.total { margin-top: 8rpx; padding-top: 12rpx; border-top: 1rpx solid $cozy-border; color: $cozy-ink; font-weight: 700; }.price-row.total text:last-child { color: $cozy-primary; font-size: 32rpx; }
.detail-spacer { height: 150rpx; }.detail-footer { position: fixed; left: 0; right: 0; bottom: 0; padding: 14rpx 28rpx max(14rpx, env(safe-area-inset-bottom)); display: flex; gap: 16rpx; border-top: 1rpx solid $cozy-border; background: #fff; }.footer-button { flex: 1; height: 84rpx; display: flex; align-items: center; justify-content: center; border-radius: $cozy-radius-md; font-size: 26rpx; font-weight: 650; }.cancel-button { border: 2rpx solid $cozy-primary; color: $cozy-primary; }.reorder-button { background: $cozy-primary; color: #fff; }.reorder-button.disabled { opacity: .55; }
</style>
