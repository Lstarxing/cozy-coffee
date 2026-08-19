<!--
  兑换记录页 - 精确复现 prototype/points-redemptions.html：全部/虚拟/自提/快递筛选 + 订单卡片 + 取消/确认收货 + 查看详情
  数据源: /member/mall/orders (PointsOrderDTO)
-->
<template>
  <view class="redemptions-page">
    <!-- 顶部白色筛选条（全部/虚拟/自提/快递） -->
    <view class="order-top">
      <view class="filter-tabs">
        <view
          v-for="f in filters"
          :key="f.value"
          class="filter-tab"
          :class="{ active: activeFilter === f.value }"
          @click="switchFilter(f.value)"
        >{{ f.label }}</view>
      </view>
    </view>

    <!-- 列表 -->
    <LoadingState v-if="loading && orders.length === 0" text="正在加载兑换记录…" />
    <RetryState v-else-if="errorMessage && orders.length === 0" :description="errorMessage" @retry="loadOrders" />

    <view v-else-if="shownOrders.length" class="order-list">
      <view
        v-for="order in shownOrders"
        :key="order.id"
        class="order-card"
        @click="goToDetail(order)"
      >
        <view class="order-head">
          <text class="order-no">{{ order.orderNo }}</text>
          <text class="status-chip" :class="order.status">{{ getStatusText(order.status) }}</text>
        </view>

        <view class="order-product">
          <view class="order-thumb">
            <image v-if="order.productImage" :src="order.productImage" class="thumb-photo" mode="aspectFill" />
          </view>
          <view class="order-copy">
            <text class="order-name">{{ order.productName }}</text>
            <text class="order-meta">数量 ×{{ order.quantity || 1 }} · {{ formatTime(order.createdAt) }}</text>
          </view>
          <text class="order-points">{{ formatPoints(order.pointsCost) }}</text>
        </view>

        <view class="order-foot">
          <view class="order-fulfill">
            <text>{{ getFulfillmentText(order) }}</text>
            <text v-if="order.pickupCode" class="fulfill-code">取货码 {{ order.pickupCode }}</text>
            <text v-else-if="order.trackingNumber" class="fulfill-code">物流 {{ order.trackingNumber }}</text>
            <text v-else-if="order.fulfillmentType === 'VIRTUAL' && order.virtualCode" class="fulfill-code">兑换码 {{ order.virtualCode }}</text>
          </view>
          <view class="order-actions" @click.stop>
            <view class="mini-btn" @click="goToDetail(order)">查看详情</view>
            <view v-if="canCancel(order)" class="mini-btn" @click="askCancel(order)">取消兑换</view>
            <view v-if="canConfirm(order)" class="mini-btn primary" @click="askConfirm(order)">确认收货</view>
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
  return label && label !== '全部' ? `暂无${label}兑换记录` : '暂无兑换记录'
})

const STATUS_TEXT = { pending: '待处理', processing: '处理中', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
const FULFILL_TEXT = { PICKUP: '到店自提', DELIVERY: '快递配送', VIRTUAL: '已发放至券包' }

async function loadOrders() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getMyRedemptions()
    if (response.code === 200 && response.data) orders.value = response.data
    else errorMessage.value = response?.message || '兑换记录加载失败'
  } catch (error) {
    errorMessage.value = error?.message || '兑换记录加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function switchFilter(value) {
  if (activeFilter.value === value) return
  activeFilter.value = value
}

function getStatusText(status) { return STATUS_TEXT[status] || status || '未知状态' }
function getFulfillmentText(order) { return FULFILL_TEXT[order.fulfillmentType] || '兑换订单' }
function formatPoints(value) { return Number(value || 0).toLocaleString() }
function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

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

/* ── 顶部白色区（胶囊下方留白 + 筛选，对齐订单页） ── */
.order-top { background: #fff; }

/* ── 自取/外送筛选 ── */
.filter-tabs {
  display: flex;
  height: 96rpx;
  background: $bg-white;
  border-bottom: 1rpx solid $cozy-border;
}
.filter-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: $cozy-muted;
  position: relative;
  transition: color $cozy-duration $cozy-ease-out;

  &.active {
    color: $cozy-ink;
    font-weight: 600;
  }
  &.active::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 0;
    transform: translateX(-50%);
    width: 64rpx;
    height: 4rpx;
    border-radius: 2rpx;
    background: $cozy-primary;
  }
}

/* ── 订单列表 ── */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 32rpx 24rpx 0;
}
.order-card {
  padding: 36rpx;
  border-radius: 28rpx;
  background: $bg-white;
  transition: opacity .2s;

  &:active { opacity: .8; }
}
.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.order-no {
  font-size: 22rpx;
  color: $cozy-placeholder;
  letter-spacing: .04em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
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

.order-product {
  display: flex;
  align-items: center;
  gap: 28rpx;
  margin-top: 28rpx;
}
.order-thumb {
  flex: none;
  width: 104rpx;
  height: 104rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #E8DDD2, #D8C8B4);
  overflow: hidden;
}
.thumb-photo { width: 100%; height: 100%; }
.order-copy { min-width: 0; flex: 1; }
.order-name {
  display: block;
  font-family: $font-display;
  font-size: 30rpx;
  font-weight: 600;
  color: $cozy-ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.order-meta {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}
.order-points {
  flex: none;
  font-size: 28rpx;
  font-weight: 700;
  color: $cozy-primary;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $cozy-border;
}
.order-fulfill {
  min-width: 0;
  font-size: 24rpx;
  color: $cozy-muted;
}
.fulfill-code {
  display: block;
  margin-top: 6rpx;
  font-family: monospace;
  font-size: 22rpx;
  color: $cozy-primary;
  letter-spacing: .05em;
}
.order-actions { display: flex; gap: 16rpx; flex: none; }
.mini-btn {
  padding: 14rpx 28rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  font-weight: 600;
  border: 1rpx solid $cozy-border;
  background: $bg-white;
  color: $cozy-ink;

  &:active { background: $cozy-surface; }

  &.primary {
    border-color: $cozy-primary;
    background: $cozy-primary;
    color: #fff;
  }
  &.primary:active { background: $cozy-primary-hover; }
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
