<template>
  <div class="orders-view">
    <header class="content-header">
      <h3>咖啡订单记录</h3>
      <button
        class="refresh-btn"
        :disabled="isRefreshingOrders"
        title="刷新订单"
        @click="handleRefreshOrders"
      >
        <RefreshCw :size="18" :class="{ 'spinning': isRefreshingOrders }" />
      </button>
    </header>

    <div v-if="coffeeOrders.length > 0" class="orders-list">
      <div v-for="order in coffeeOrders" :key="order.id" class="order-card">
        <div class="order-header">
          <span class="order-no">订单号: {{ order.orderNo }}</span>
          <div class="header-right">
            <span v-if="order.diningMethod" class="dining-badge">{{ getDiningMethodText(order.diningMethod) }}</span>
            <span class="order-status" :class="order.status">{{ getStatusText(order.status) }}</span>
          </div>
        </div>
        <div class="order-body">
          <div class="order-info">
            <div class="order-items-detail">
              <div v-for="(item, idx) in order.items" :key="idx" class="order-line-item">
                <img :src="getImageUrl(item.productImage, '/images/products/default.png')" class="line-item-img" />
                <div class="line-item-info">
                  <span class="line-item-name">{{ item.productName }} <span class="qty">x{{ item.quantity }}</span></span>
                  <span v-if="formatSpecs(item)" class="line-item-specs">{{ formatSpecs(item) }}</span>
                </div>
                <span class="line-item-price">¥{{ item.itemAmount }}</span>
              </div>
            </div>

            <div class="order-meta-row">
              <p class="order-amount">实付: ¥{{ order.payAmount }}</p>
              <p v-if="order.pointsEarned && order.status === 'completed'" class="points-earned">获得积分: +{{ order.pointsEarned }}</p>
            </div>
            <div class="order-meta-secondary">
              <div class="meta-left">
                <p class="order-time">{{ formatDate(order.createdAt) }}</p>
                <p v-if="order.pickupCode" class="pickup-code">取餐码: <strong>{{ order.pickupCode }}</strong></p>
                <p v-if="order.status === 'pending'" class="expire-countdown" :class="{ urgent: isAboutToExpire(order) }">{{ formatRemaining(order) }}</p>
              </div>
              <div v-if="order.status === 'pending'" class="meta-right">
                <button class="pay-btn-small" @click.stop="payCoffeeOrder(order)">去支付</button>
                <button class="cancel-btn-small" @click.stop="cancelCoffeeOrder(order.id)">取消订单</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="no-data">
      <p>暂无咖啡订单记录</p>
      <button class="go-order-btn" @click="router.push('/member/order')">去点单</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshCw } from 'lucide-vue-next'
import { cancelOrder, listOrders, acceptOrder } from '@/api/order'
import { getImageUrl } from '@/utils/image'

const router = useRouter()

const coffeeOrders = ref([])
const isRefreshingOrders = ref(false)
const nowTs = ref(Date.now())
let countdownTimer = null

function startCountdown() {
  stopCountdown()
  countdownTimer = setInterval(() => { nowTs.value = Date.now() }, 1000)
}
function stopCountdown() {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

function getOrderExpireMs(order) {
  if (order?.expireAt) {
    const t = new Date(order.expireAt).getTime()
    if (!Number.isNaN(t)) return t
  }
  if (order?.createdAt) {
    const ct = new Date(order.createdAt).getTime()
    if (!Number.isNaN(ct)) return ct + 15 * 60 * 1000
  }
  return null
}

function formatRemaining(order) {
  const expireMs = getOrderExpireMs(order)
  if (expireMs == null) return '等待支付'
  const remain = Math.max(0, Math.floor((expireMs - nowTs.value) / 1000))
  if (remain <= 0) return '即将自动取消'
  const mm = String(Math.floor(remain / 60)).padStart(2, '0')
  const ss = String(remain % 60).padStart(2, '0')
  return `剩余 ${mm}:${ss} 自动取消`
}

function isAboutToExpire(order) {
  const expireMs = getOrderExpireMs(order)
  if (expireMs == null) return false
  return Math.max(0, Math.floor((expireMs - nowTs.value) / 1000)) <= 30
}

async function loadCoffeeOrders() {
  try {
    const data = await listOrders()
    coffeeOrders.value = data.data || []
  } catch (error) {
    console.error('加载消费订单失败:', error)
  }
}

async function handleRefreshOrders() {
  if (isRefreshingOrders.value) return
  isRefreshingOrders.value = true
  try {
    await loadCoffeeOrders()
    ElMessage.success({ message: '订单列表已更新', duration: 1500 })
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    isRefreshingOrders.value = false
  }
}

async function payCoffeeOrder(order) {
  try {
    await ElMessageBox.confirm(`订单金额 ¥${Number(order?.payAmount ?? order?.totalAmount ?? 0).toFixed(2)}，确认模拟支付？`, '模拟支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await acceptOrder(order.id)
    if (res.success || res.status === 200) {
      ElMessage.success('支付成功，商家已接单')
      loadCoffeeOrders()
    } else {
      ElMessage.error(res.message || '支付失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('支付失败')
    }
  }
}

async function cancelCoffeeOrder(orderId) {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await cancelOrder(orderId)
    if (res.success || (res.data && res.data.success) || res.status === 200) {
      ElMessage.success('订单已取消')
      loadCoffeeOrders()
    } else {
      ElMessage.error(res.message || (res.data && res.data.message) || '取消失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('操作失败')
    }
  }
}

function getStatusText(status) {
  const map = {
    pending: '待支付',
    preparing: '制作中',
    processing: '处理中',
    shipped: '已发货',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

function getDiningMethodText(method) {
  const map = { 'TAKEOUT': '自提', 'DELIVERY': '外卖' }
  return map[method] || '自提'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function formatSpecs(item) {
  if (!item) return ''
  const isBakeryItem = !item.temperature && !item.cupSize
  if (isBakeryItem) return ''
  const parts = []
  const map = {
    'STANDARD': '标准杯', 'standard': '标准杯', 'LARGE': '大杯', 'large': '大杯', 'MEDIUM': '中杯', 'medium': '中杯',
    'HOT': '热', 'hot': '热', 'COLD': '冰', 'cold': '冰', 'iced': '冰', 'WARM': '温', 'warm': '温',
    'NONE': '无糖', 'none': '无糖', 'LESS': '少糖', 'less': '少糖', 'HALF': '半糖', 'half': '半糖',
    'LIGHT': '微甜', 'light': '微甜', 'full': '标准甜', 'STANDARD_SUGAR': '标准甜', 'NO_ADDED_SUGAR': '不另外加糖', 'no_added_sugar': '不另外加糖',
    'NORMAL': '标准浓度', 'STRONG': '加浓'
  }
  if (item.cupSize) parts.push(map[item.cupSize] || item.cupSize)
  if (item.temperature) parts.push(map[item.temperature] || item.temperature)
  if (item.sugarLevel) {
    if (item.sugarLevel !== 'STANDARD' && item.sugarLevel !== 'standard' && item.sugarLevel !== 'full') {
      parts.push(map[item.sugarLevel] || item.sugarLevel)
    }
  }
  if (item.coffeeStrength && item.coffeeStrength === 'STRONG') {
    parts.push(map[item.coffeeStrength] || item.coffeeStrength)
  }
  // V2：奶型以 addons_json 成交快照为准——黑咖无奶组→无奶项不显示；WHOLE_MILK 默认不显示
  if (item.addonsJson) {
    try {
      const addons = JSON.parse(item.addonsJson)
      const milkMap = { 'OAT_MILK': '燕麦奶', 'COCONUT_MILK': '椰奶', 'OAT': '燕麦奶', 'COCONUT': '椰奶' }
      const m = (Array.isArray(addons) ? addons : []).find(a => milkMap[a.code])
      if (m) parts.push(milkMap[m.code])
    } catch (_) { /* ignore parse error */ }
  }
  return parts.length > 0 ? parts.join(' / ') : ''
}

onMounted(() => {
  loadCoffeeOrders()
  startCountdown()
})
onUnmounted(stopCountdown)
</script>

<style scoped>
.orders-view {
  animation: fadeIn 0.4s ease-out;
  max-width: 1000px;
  margin: 0 auto;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}

.content-header h3 {
  font-size: 28px;
  font-weight: 300;
  color: #1a1a1a;
  margin: 0;
}

.refresh-btn {
  margin-left: auto;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8B4513;
  transition: all 0.3s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(139, 69, 19, 0.1);
  transform: rotate(15deg);
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.refresh-btn .spinning {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.06);
  border-color: #e6e6e6;
}

.order-header {
  padding: 16px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-size: 13px;
  color: #6b7280;
  font-family: monospace;
}

.order-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.order-status.pending { background: #fff7ed; color: #d97706; }
.order-status.completed { background: #E6F1EB; color: #059669; }
.order-status.cancelled { background: #fef2f2; color: #dc2626; }

.header-right {
  display: flex;
  align-items: center;
}

.dining-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #4b5563;
  margin-right: 8px;
  font-weight: 500;
  border: 1px solid #e5e7eb;
}

.order-body {
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.order-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-items-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
  width: 100%;
}

.order-line-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #eee;
}

.order-line-item:last-child {
  border-bottom: none;
}

.line-item-img {
  width: 40px; height: 40px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #f0f0f0;
}

.line-item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.line-item-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.line-item-name .qty {
  color: #999;
  font-size: 12px;
  margin-left: 4px;
}

.line-item-specs {
  font-size: 12px;
  color: #888;
}

.line-item-price {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.order-meta-row {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  align-items: center;
}

.order-meta-secondary {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f9f9f9;
}

.meta-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-right {
  display: flex;
  align-items: center;
}

.order-amount, .points-earned {
  font-size: 15px;
  font-weight: 700;
  color: #d97706;
}

.pickup-code {
  display: inline-block;
  background: #fff7ed;
  color: #d97706;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
  margin-top: 4px;
  border: 1px dashed #fdba74;
}

.expire-countdown {
  display: inline-block;
  margin-top: 6px;
  color: #d97706;
  font-size: 13px;
  font-weight: 600;
}
.expire-countdown.urgent {
  color: #ef4444;
  font-weight: 700;
}

.cancel-btn-small {
  background: #fff;
  border: 1px solid #d1d5db;
  color: #4b5563;
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto;
}

.pay-btn-small {
  background: #8b4513;
  border: 1px solid #8b4513;
  color: #fff;
  padding: 4px 14px;
  border-radius: 14px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.pay-btn-small:hover {
  background: #6d3610;
}

.cancel-btn-small:hover {
  background: #fee2e2;
  color: #ef4444;
  border-color: #fca5a5;
}

.no-data {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
  margin-top: 20px;
}

.no-data p {
  color: #9ca3af;
  font-size: 15px;
  margin-bottom: 20px;
}

.go-order-btn {
  padding: 10px 24px;
  background: #d97706;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  margin-top: 16px;
  transition: background 0.2s;
}

.go-order-btn:hover {
  background: #b45309;
}
</style>
