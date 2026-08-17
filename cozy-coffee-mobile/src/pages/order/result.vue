<template>
  <view class="result-page">
    <LoadingState v-if="loading" text="正在读取订单结果…" />
    <RetryState v-else-if="errorMessage" title="订单已提交" :description="errorMessage" @retry="loadOrder" />
    <view v-else-if="order" class="result-content">
      <view class="success-mark">✓</view>
      <text class="result-title">{{ statusTitle }}</text>
      <text class="result-subtitle">模拟支付已完成，请留意门店制作进度</text>

      <view class="pickup-card">
        <view class="pickup-top">
          <text class="pickup-label">取餐码</text>
          <text class="pickup-store">CozyCoffee 中心店</text>
        </view>
        <text class="pickup-code">{{ order.pickupCode || '生成中' }}</text>
        <text class="pickup-hint">请在门店出示取餐码</text>
      </view>

      <view class="order-info">
        <view class="info-row"><text>订单号</text><text>{{ order.orderNo || '--' }}</text></view>
        <view class="info-row"><text>订单状态</text><text>{{ statusText(order.status) }}</text></view>
        <view class="info-row"><text>自提门店</text><text>CozyCoffee 中心店</text></view>
        <view class="info-row total"><text>实付金额</text><text>¥{{ money(order.payAmount ?? order.totalAmount) }}</text></view>
      </view>

      <view class="action-group">
        <view class="primary-button" @click="goToDetail">查看订单详情</view>
        <view class="secondary-button" @click="goToOrders">查看全部订单</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderDetail } from '@/api/order'
import { useSessionStore } from '@/stores/session'
import { refreshMemberProfile } from '@/services/session/MemberProfileService'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'

const orderId = ref('')
const order = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const sessionStore = useSessionStore()
const statusTitle = computed(() => ['cancelled', 'canceled'].includes(String(order.value?.status).toLowerCase()) ? '订单已取消' : '下单成功')
const normalizedStatus = computed(() => String(order.value?.status || '').toLowerCase())
let pollTimer = null
let memberRefreshTimer = null
const refreshedMemberStages = new Set()

onLoad(options => { orderId.value = options.orderId || options.id || ''; loadOrder() })
onShow(startPolling)
onHide(stopPolling)
onUnload(() => { stopPolling(); clearMemberRefreshTimer() })

async function loadOrder(silent = false) {
  if (!orderId.value) { loading.value = false; errorMessage.value = '缺少订单编号，请前往订单列表查看'; return }
  if (!silent) loading.value = true
  errorMessage.value = ''
  try {
    const response = await getOrderDetail(orderId.value)
    order.value = response?.data ?? response
    refreshMemberForOrder(order.value)
  } catch (error) {
    errorMessage.value = error?.message || '暂时无法读取订单详情，可前往订单列表查看'
  } finally { if (!silent) loading.value = false }
}

function startPolling() {
  stopPolling()
  if (!orderId.value) return
  pollTimer = setInterval(() => {
    if (!['completed', 'cancelled', 'canceled'].includes(normalizedStatus.value)) loadOrder(true)
    else stopPolling()
  }, 3000)
}
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

async function refreshMemberForOrder(currentOrder) {
  const status = String(currentOrder?.status || '').toLowerCase()
  if (['cancelled', 'canceled'].includes(status)) return
  const stage = status === 'completed' ? 'completed' : 'payment'
  if (refreshedMemberStages.has(stage)) return

  refreshedMemberStages.add(stage)
  try {
    await refreshMemberProfile(sessionStore)
  } catch (_) {
    refreshedMemberStages.delete(stage)
    return
  }

  if (stage === 'completed') {
    clearMemberRefreshTimer()
    memberRefreshTimer = setTimeout(() => {
      refreshMemberProfile(sessionStore).catch(() => {})
    }, 1500)
  }
}

function clearMemberRefreshTimer() {
  if (!memberRefreshTimer) return
  clearTimeout(memberRefreshTimer)
  memberRefreshTimer = null
}

function statusText(status) { return ({ pending: '待接单', pending_payment: '待处理', preparing: '制作中', processing: '制作中', completed: '已完成', cancelled: '已取消' })[String(status || '').toLowerCase()] || '已提交' }
function money(value) { return Number(value || 0).toFixed(2) }
function goToOrders() { uni.switchTab({ url: '/pages/order/order' }) }
function goToDetail() { uni.navigateTo({ url: `/pages/order/detail?id=${encodeURIComponent(orderId.value)}` }) }
</script>

<style lang="scss" scoped>
.result-page { min-height: 100vh; background: $cozy-surface; }
.result-content { padding: 72rpx 28rpx 48rpx; text-align: center; }
.success-mark { width: 120rpx; height: 120rpx; margin: 0 auto 32rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $cozy-accent; color: #fff; font-size: 64rpx; font-weight: 750; }
.result-title { display: block; color: $cozy-ink; font-family: $font-display; font-size: 44rpx; font-weight: 600; }
.result-subtitle { display: block; margin-top: 14rpx; color: $cozy-muted; font-size: 24rpx; }

/* 取餐码（深色卡） */
.pickup-card { margin-top: 48rpx; padding: 40rpx; border-radius: 28rpx; background: $cozy-surface-alt; color: #fff; }
.pickup-top { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; }
.pickup-label, .pickup-store { color: $cozy-muted-on-dark; font-size: 22rpx; }
.pickup-code { display: block; margin: 20rpx 0; font-size: 76rpx; font-weight: 800; letter-spacing: .14em; }
.pickup-hint { display: block; color: $cozy-muted-on-dark; font-size: 22rpx; }

/* 订单信息 */
.order-info { margin-top: 24rpx; padding: 20rpx 28rpx; border-radius: 28rpx; background: #fff; text-align: left; }
.info-row { min-height: 72rpx; display: flex; align-items: center; justify-content: space-between; gap: 24rpx; color: $cozy-muted; font-size: 24rpx; }
.info-row text:last-child { color: $cozy-ink; text-align: right; }
.info-row.total { margin-top: 8rpx; border-top: 1rpx solid $cozy-border; color: $cozy-ink; font-weight: 700; }
.info-row.total text:last-child { color: $cozy-primary; font-size: 32rpx; }

/* 操作 */
.action-group { display: flex; gap: 20rpx; margin-top: 40rpx; }
.primary-button { flex: 1.4; height: 92rpx; display: flex; align-items: center; justify-content: center; border-radius: 999rpx; background: $cozy-ink; color: #fff; font-size: 28rpx; font-weight: 600; }
.secondary-button { flex: 1; height: 92rpx; display: flex; align-items: center; justify-content: center; border-radius: 12rpx; border: 1rpx solid $cozy-border; background: #fff; color: $cozy-ink; font-size: 28rpx; font-weight: 600; }
</style>
