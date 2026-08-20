<!--
  兑换订单页 - 精确复现 prototype/points-redemptions.html：全部/虚拟/自提/快递筛选 + 订单卡片 + 取消/确认收货 + 查看详情
  数据源: /member/mall/orders (PointsOrderDTO)
-->
<template>
  <view class="redemptions-page">
    <!-- 顶部筛选条（全部/虚拟/自提/快递） -->
    <FilterTabs :options="filters" v-model="activeFilter" />

    <!-- 列表 -->
    <LoadingState v-if="loading && orders.length === 0" text="正在加载兑换订单…" />
    <RetryState v-else-if="errorMessage && orders.length === 0" :description="errorMessage" @retry="loadOrders" />

    <view v-else-if="shownOrders.length" class="order-list">
      <view
        v-for="order in shownOrders"
        :key="order.id"
        class="order-receipt"
        @click="goToDetail(order)"
      >
        <view class="receipt-head">
          <view>
            <text class="store-name">{{ storeName(order) }}</text>
            <text class="order-meta">{{ formatTime(order.createdAt) }}<text v-if="codeText(order)" class="meta-em"> · {{ codeText(order) }}</text></text>
          </view>
          <view class="status-block">
            <text class="status-chip" :class="order.status">{{ getStatusText(order.status) }}</text>
          </view>
        </view>

        <view class="receipt-items">
          <view class="order-item">
            <view class="item-image"><image v-if="order.productImage" :src="order.productImage" class="item-img" mode="aspectFill" /></view>
            <view class="item-info">
              <text class="item-name">{{ order.productName }}</text>
              <text class="item-spec">数量 ×{{ order.quantity || 1 }}</text>
            </view>
            <view class="item-right">
              <text class="item-points">{{ formatPoints(order.pointsCost) }} 积分</text>
            </view>
          </view>
        </view>

        <view class="receipt-summary">
          <text class="summary-label">共 {{ order.quantity || 1 }} 件 · {{ getFulfillmentText(order) }}</text>
          <view class="summary-actions" @click.stop>
            <view class="mini-btn" @click="goToDetail(order)">查看详情</view>
            <view v-if="canCancel(order)" class="mini-btn" @click="askCancel(order)">取消兑换</view>
            <view v-if="canConfirm(order)" class="mini-btn strong" @click="askConfirm(order)">确认收货</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <view class="empty-mark"><CozyIcon name="gift" :size="36" color="#753A22" /></view>
      <text class="empty-text">{{ emptyText }}</text>
      <text class="empty-hint">去积分商城兑换你的第一份心意</text>
    </view>

    <!-- 确认弹层 -->
    <view v-if="showConfirm" class="modal-mask" @click="closeConfirm">
      <view class="modal-card" @click.stop>
        <text class="modal-title">{{ confirmTitle }}</text>
        <text class="modal-content">{{ confirmContent }}</text>
        <view class="modal-actions">
          <view class="modal-btn ghost" @click="closeConfirm">再想想</view>
          <view class="modal-btn solid" @click="doConfirm">确认</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { cancelRedemption, confirmRedemptionReceipt, getMyRedemptions } from '@/api/member'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'
import CozyIcon from '@/components/CozyIcon.vue'
import FilterTabs from '@/components/common/FilterTabs.vue'
import { FIXED_STORE } from '@/config/store'
import { formatPoints, formatTime } from '@/utils/format'

const filters = [
  { value: 'all', label: '全部' },
  { value: 'virtual', label: '虚拟' },
  { value: 'pickup', label: '自提' },
  { value: 'delivery', label: '快递' }
]

const activeFilter = ref('all')
const orders = ref([])
const loading = ref(false)
const errorMessage = ref('')

const showConfirm = ref(false)
const confirmTitle = ref('')
const confirmContent = ref('')
let pendingAction = null // { type, order }

const FULFILL_FILTER = { virtual: 'VIRTUAL', pickup: 'PICKUP', delivery: 'DELIVERY' }
const shownOrders = computed(() => {
  if (activeFilter.value === 'all') return orders.value
  const key = FULFILL_FILTER[activeFilter.value]
  return orders.value.filter(o => o.fulfillmentType === key)
})
const emptyText = computed(() => {
  const label = filters.find(f => f.value === activeFilter.value)?.label
  return label && label !== '全部' ? `暂无${label}兑换订单` : '暂无兑换订单'
})

const STATUS_TEXT = { pending: '待处理', processing: '处理中', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
const FULFILL_TEXT = { PICKUP: '到店自提', DELIVERY: '快递配送', VIRTUAL: '已发放至券包' }

const isVirtualOrder = (order) => order.fulfillmentType === 'VIRTUAL'
const storeName = (order) => isVirtualOrder(order) ? '虚拟商品' : FIXED_STORE.name
const codeText = (order) => {
  if (order.pickupCode) return `取货码 ${order.pickupCode}`
  if (order.trackingNumber) return `物流 ${order.trackingNumber}`
  if (isVirtualOrder(order) && order.virtualCode) return `兑换码 ${order.virtualCode}`
  return ''
}

async function loadOrders() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getMyRedemptions()
    if (response.code === 200 && response.data) orders.value = response.data
    else errorMessage.value = response?.message || '兑换订单加载失败'
  } catch (error) {
    errorMessage.value = error?.message || '兑换订单加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function getStatusText(status) { return STATUS_TEXT[status] || status || '未知状态' }
function getFulfillmentText(order) { return FULFILL_TEXT[order.fulfillmentType] || '兑换订单' }

function canCancel(order) { return ['pending', 'processing'].includes(order.status) }
function canConfirm(order) { return order.status === 'shipped' && order.fulfillmentType === 'DELIVERY' }

function goToDetail(order) {
  uni.navigateTo({ url: `/pages/order/detail?id=${encodeURIComponent(order.id)}&type=redeem` })
}

function openConfirm(title, content, action) {
  confirmTitle.value = title
  confirmContent.value = content
  pendingAction = action
  showConfirm.value = true
}
function closeConfirm() {
  pendingAction = null
  showConfirm.value = false
}
function askCancel(order) {
  openConfirm('取消兑换', `取消后将退还 ${formatPoints(order.pointsCost)} 积分，优惠券/礼品将同时失效，是否继续？`, { type: 'cancel', order })
}
function askConfirm(order) {
  openConfirm('确认收货', '请确认已收到商品。确认后订单将标记为已完成。', { type: 'confirm', order })
}

async function doConfirm() {
  if (!pendingAction) return
  const { type, order } = pendingAction
  closeConfirm()
  try {
    if (type === 'cancel') {
      await cancelRedemption(order.id)
      uni.showToast({ title: `已取消，退还 ${formatPoints(order.pointsCost)} 积分`, icon: 'none' })
    } else {
      await confirmRedemptionReceipt(order.id)
      uni.showToast({ title: '已确认收货，感谢支持', icon: 'none' })
    }
    loadOrders()
  } catch (error) {
    uni.showToast({ title: error?.message || '操作失败，请稍后重试', icon: 'none' })
  }
}

onShow(loadOrders)
</script>

<style lang="scss" scoped>
.redemptions-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 0 0 240rpx;
}

/* ── 订单列表 ── */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 32rpx 24rpx 120rpx;
}
.order-receipt {
  overflow: hidden;
  border: 1rpx solid $cozy-border;
  border-radius: 24rpx;
  background: $bg-white;
  transition: opacity .2s;

  &:active { opacity: .8; }
}
.receipt-head {
  padding: 28rpx 28rpx 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  border-bottom: 1rpx solid $cozy-border;
}
.store-name { display: block; font-size: 30rpx; font-weight: 650; color: $cozy-ink; }
.store-addr { display: block; margin-top: 4rpx; font-size: 22rpx; color: $cozy-muted; }
.order-meta { display: block; margin-top: 8rpx; font-size: 20rpx; color: $cozy-muted; }
.meta-em { font-style: normal; color: $cozy-primary; font-weight: 650; }
.status-block { flex: none; text-align: right; }
.status-chip {
  flex: none;
  padding: 6rpx 20rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  font-weight: 650;
  letter-spacing: .02em;

  &.pending { background: $cozy-border; color: $cozy-muted; }
  &.processing { background: #F3E7D8; color: #8B5E2E; }
  &.shipped { background: #F1E4DA; color: $cozy-primary; }
  &.completed { background: #E6F1EB; color: #059669; }
  &.cancelled { background: $cozy-border; color: $cozy-placeholder; }
}
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
  width: 104rpx;
  height: 104rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #E8DDD2, #D8C8B4);
  overflow: hidden;
}
.item-img { width: 100%; height: 100%; }
.item-info { flex: 1; min-width: 0; }
.item-name {
  display: block;
  font-family: $font-display;
  font-size: 30rpx;
  font-weight: 600;
  color: $cozy-ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-spec { display: block; margin-top: 8rpx; font-size: 22rpx; color: $cozy-muted; }
.item-right { flex: none; }
.item-points { font-size: 28rpx; font-weight: 700; color: $cozy-primary; }
.receipt-summary {
  padding: 24rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20rpx;
}
.summary-label { flex: 1; min-width: 0; font-size: 22rpx; color: $cozy-muted; }
.summary-actions { flex: none; display: flex; align-items: center; gap: 12rpx; }
.mini-btn {
  flex: none;
  padding: 14rpx 28rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  font-weight: 600;
  border: 1rpx solid $cozy-border;
  background: $bg-white;
  color: $cozy-ink;

  &:active { background: $cozy-surface; }

  &.strong {
    border-color: $cozy-ink;
    background: transparent;
    color: $cozy-ink;
    font-weight: 650;
  }
  &.strong:active { opacity: .85; }
}

/* ── 空状态（复用优惠券页逻辑：图标 + 衬线大标题 + 视口居中） ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 320rpx);
  padding: 80rpx 40rpx;
}
.empty-mark {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: $cozy-surface;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $cozy-primary;
}
.empty-text {
  margin-top: 40rpx;
  font-family: $font-display;
  font-size: 40rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.empty-hint {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: $cozy-muted;
}

/* ── 确认弹层 ── */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(44,39,35,.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-card {
  width: 78%;
  max-width: 600rpx;
  border-radius: 28rpx;
  background: $bg-white;
  padding: 52rpx 44rpx 40rpx;
  text-align: center;
}
.modal-title {
  display: block;
  font-family: $font-display;
  font-size: 36rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.modal-content {
  display: block;
  margin-top: 24rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: $cozy-muted;
}
.modal-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 48rpx;
}
.modal-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;

  &.ghost { background: $cozy-surface; color: $cozy-ink; }
  &.solid { background: $cozy-primary; color: #fff; }
  &:active { opacity: .85; }
}
</style>
