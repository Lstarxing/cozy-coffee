<template>
  <div class="orders-page">
    <AdminPageHeader title="咖啡订单管理" subtitle="咖啡订单处理与查询">
       <template #extra>
         <NewDataBanner 
            :visible="hasNewData" 
            message="有新订单，点击刷新"
            @refresh="handleNewDataRefresh"
            @dismiss="hasNewData = false"
          />
       </template>
    </AdminPageHeader>

    <AdminFilterBar @search="handleSearch" @reset="resetFilters" class="compact-filter">
      <el-form-item label="搜索">
        <el-input 
          v-model="filters.keyword" 
          placeholder="订单号 / 手机号" 
          clearable 
          @keyup.enter="handleSearch"
          style="width: 220px"
        />
      </el-form-item>
       <el-form-item label="下单时间">
        <el-date-picker 
          v-model="filters.dateRange" 
          type="daterange" 
          range-separator="至" 
          start-placeholder="开始" 
          end-placeholder="结束" 
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="handleSearch"
        />
      </el-form-item>
    </AdminFilterBar>

    <!-- 4. 快捷筛选标签栏 -->
    <div class="quick-filter-tabs">
       <button 
         class="filter-tab" 
         :class="{ active: filters.status === '' }"
         @click="handleQuickFilter('')"
       >
         全部
         <span class="tab-badge gray" v-if="orderCounts.total > 0">{{ orderCounts.total }}</span>
       </button>
       <button 
         class="filter-tab" 
         :class="{ active: filters.status === 'pending' }"
         @click="handleQuickFilter('pending')"
       >
         待处理
         <span class="tab-badge red" v-if="orderCounts.pending > 0">{{ orderCounts.pending }}</span>
       </button>
       <button 
         class="filter-tab highlight-processing" 
         :class="{ active: filters.status === 'preparing' }"
         @click="handleQuickFilter('preparing')"
       >
         <el-icon class="processing-icon" v-if="filters.status === 'preparing'"><Loading /></el-icon>
         制作中
         <span class="tab-badge orange" v-if="orderCounts.preparing > 0">{{ orderCounts.preparing }}</span>
       </button>
       <button 
         class="filter-tab" 
         :class="{ active: filters.status === 'completed' }"
         @click="handleQuickFilter('completed')"
       >
          已完成
          <!-- 已完成数量通常很大，可以不显示或者显示 -->
       </button>
       <button 
         class="filter-tab" 
         :class="{ active: filters.status === 'cancelled' }"
         @click="handleQuickFilter('cancelled')"
       >
          已取消
       </button>
    </div>

    <el-card shadow="never" class="table-card compact-card">
       <TableToolbar :last-updated="lastUpdated" @refresh="loadOrders" />

       <el-table 
        :data="paginatedOrders" 
        v-loading="loading"
        header-row-class-name="warm-header"
        size="default"
       >
        <!-- 1. 订单信息 (含客户) -->
        <el-table-column label="订单信息" width="240">
          <template #default="{ row }">
            <div class="order-info-cell">
              <div class="order-info-top">
                <el-tag 
                  size="small" 
                  :type="getDiningMethodTagType(row.diningMethod)" 
                  effect="dark" 
                  class="dining-tag"
                >
                  {{ getDiningMethodText(row.diningMethod) }}
                </el-tag>
                <span class="pickup-code-b">{{ row.pickupCode || '--' }}</span>
              </div>
              <div class="user-row">
                 <span class="user-inline-name">
                    {{ row.deliveryName || row.nickname || row.username || '匿名' }}
                 </span>
                 <span class="user-phone-small" v-if="row.phoneMasked">{{ row.phoneMasked }}</span>
                 <span v-if="row.memberLevel === 'diamond'" class="member-emoji" title="钻石会员">💎</span>
                 <span v-if="row.memberLevel === 'black'" class="member-emoji" title="黑金会员">👑</span>
              </div>
              <div class="order-no">{{ row.orderNo }}</div>
            </div>
          </template>
        </el-table-column>
        
        <!-- 3. 商品详情 (可视化胶囊标签) -->
        <el-table-column label="商品详情" min-width="300">
          <template #default="{ row }">
            <div class="product-list" v-if="row.items && row.items.length">
              <div 
                v-for="(item, index) in row.items.slice(0, 3)" 
                :key="index" 
                class="product-item"
              >
                <div class="prod-row">
                  <span class="prod-name">{{ item.productName }}</span>
                  <span v-if="item.quantity > 1" class="prod-qty">x{{ item.quantity }}</span>
                  <!-- Specs Capsules -->
                  <div class="specs-capsules">
                    <span 
                      v-for="tag in getSpecTags(item)" 
                      :key="tag" 
                      class="spec-capsule"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>
              </div>
              <div v-if="row.items.length > 3" class="more-items">......</div>
            </div>
            <div v-else class="product-summary">{{ row.itemsSummary || '-' }}</div>
          </template>
        </el-table-column>

        <!-- 4. 金额 (优化) -->
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <div class="amount-cell">
              <div class="pay-row">
                 <span class="pay-amount">¥{{ row.payAmount }}</span>
                 <el-tag v-if="row.totalAmount > row.payAmount" type="danger" size="small" effect="plain" class="coupon-tag">
                   优惠券
                 </el-tag>
              </div>
              <span class="original-price" v-if="row.totalAmount > row.payAmount">¥{{ row.totalAmount }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 5. 状态 -->
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag 
                :type="getOrderStatusTagType(row.status)" 
                effect="light" 
                round 
                class="status-capsule"
              >
                {{ getOrderStatusText(row.status) }}
              </el-tag>
              <div v-if="row.status === 'pending'" class="expire-text" :class="{ urgent: isExpiringSoon(row) }">
                {{ formatCountdown(row) }}
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 6. 时间 -->
        <el-table-column label="时间" width="120" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt).split(' ')[1] }}</span>
            <div class="date-text-small">{{ formatDate(row.createdAt).split(' ')[0] }}</div>
          </template>
        </el-table-column>

        <!-- 7. 操作 (新增打印) -->
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <!-- Print -->
              <el-tooltip content="打印小票" placement="top" :hide-after="0">
                 <el-button link class="action-icon-btn" @click="handlePrint(row)">
                   <el-icon><Printer /></el-icon>
                 </el-button>
              </el-tooltip>

              <el-divider direction="vertical" />

              <!-- Detail -->
              <el-button link type="primary" @click="viewDetail(row)">详情</el-button>

              <!-- Pending: Accept & Cancel -->
              <template v-if="row.status === 'pending'">
                <el-button link type="success" @click="handleAccept(row)">接单</el-button>
                <el-button link type="danger" @click="handleCancel(row)">取消</el-button>
              </template>
              
              <!-- Preparing: Complete -->
              <template v-else-if="row.status === 'preparing'">
                 <el-button link type="warning" @click="handleComplete(row)">出餐</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
       </el-table>

       <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="orders.length"
            layout="total, sizes, prev, pager, next"
            background
          />
       </div>
    </el-card>

    <OrderDetailDialog
      v-model="detailDialogVisible"
      :order-id="selectedOrderId"
      @refresh="loadOrders"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrders, getOrderCounts, acceptOrder, completeOrder, cancelOrder } from '../api'
import sseService from '../api/sse'
import dayjs from 'dayjs'
import { Printer, Loading } from '@element-plus/icons-vue' 
// import { Gem, Crown } from 'lucide-vue-next' // Removed as we use Emojis

import AdminPageHeader from '../components/ui/AdminPageHeader.vue'
import AdminFilterBar from '../components/ui/AdminFilterBar.vue'
import TableToolbar from '../components/ui/TableToolbar.vue'
import NewDataBanner from '../components/NewDataBanner.vue'
import OrderDetailDialog from '../components/OrderDetailDialog.vue'

// State
const loading = ref(false)
const orders = ref([])
const hasNewData = ref(false)
const lastUpdated = ref('')

const filters = reactive({
  keyword: '',
  status: '',
  dateRange: null
})

const orderCounts = ref({
  total: 0,
  pending: 0,
  preparing: 0
})

const currentPage = ref(1)
const pageSize = ref(10)

const detailDialogVisible = ref(false)
const selectedOrderId = ref(null)
const nowTs = ref(Date.now())

let secondTicker = null
let pollingTimer = null
let delayedRefreshTimer = null
let expireSyncTimer = null

// Computed for pagination
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return orders.value.slice(start, start + pageSize.value)
})

// Actions
const loadOrders = async () => {
  loading.value = true
  try {
     const params = {
        keyword: filters.keyword, // Send as keyword
        status: filters.status,
        startDate: filters.dateRange ? filters.dateRange[0] : null,
        endDate: filters.dateRange ? filters.dateRange[1] : null
     }
     
    // 如果没有日期过滤，顺便刷新一下角标计数（确保实时）
    if (!filters.dateRange && !filters.keyword) {
       loadOrderCounts()
    }

     const res = await getOrders(params)
     let list = res.data || []
     
     // Sorting
     list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
     
     orders.value = list
     lastUpdated.value = dayjs().format('HH:mm:ss')
  } catch (e) {
     console.error(e)
     ElMessage.error('加载订单失败')
  } finally {
     loading.value = false
  }
}

const loadOrderCounts = async () => {
  try {
    const res = await getOrderCounts()
    if (res.success) {
      orderCounts.value = res.data
      const total = Object.values(res.data || {})
        .reduce((sum, val) => sum + (Number(val) || 0), 0)
      orderCounts.value.total = total
    }
  } catch (e) {
    console.warn("Failed to load counts", e)
  }
}

const handleSearch = () => { loadOrders() }

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  filters.dateRange = null
  loadOrders()
}

// Quick Filter Tab Handler
const handleQuickFilter = (status) => {
  filters.status = status
  loadOrders()
}

const handleNewDataRefresh = () => {
  hasNewData.value = false
  loadOrders()
}

const getExpireMs = (row) => {
  if (!row) return null
  if (row.expireAt) {
    const ts = new Date(row.expireAt).getTime()
    if (!Number.isNaN(ts)) return ts
  }
  if (row.createdAt) {
    const createdTs = new Date(row.createdAt).getTime()
    if (!Number.isNaN(createdTs)) {
      return createdTs + 60 * 1000
    }
  }
  return null
}

const getRemainingSeconds = (row) => {
  const expireMs = getExpireMs(row)
  if (!expireMs) return null
  const seconds = Math.floor((expireMs - nowTs.value) / 1000)
  return seconds > 0 ? seconds : 0
}

const formatCountdown = (row) => {
  const remaining = getRemainingSeconds(row)
  if (remaining == null) return '即将超时'
  if (remaining <= 0) return '即将自动取消'

  const minutes = Math.floor(remaining / 60)
  const seconds = remaining % 60
  return `剩余 ${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const isExpiringSoon = (row) => {
  const remaining = getRemainingSeconds(row)
  return remaining != null && remaining <= 30
}

const viewDetail = (row) => {
  selectedOrderId.value = row.id
  detailDialogVisible.value = true
}

const handlePrint = (row) => {
  ElMessage.success('已发送至打印机: ' + row.pickupCode)
}

const handleAccept = async (row) => {
  try {
    await acceptOrder(row.id)
    loadOrders()
    ElMessage.success('已接单')
  } catch (e) {
    ElMessage.error('接单失败: ' + e.message)
  }
}

const handleComplete = async (row) => {
   try {
     await completeOrder(row.id)
     loadOrders()
     ElMessage.success('订单已完成')
   } catch (e) {
     ElMessage.error('操作失败: ' + e.message)
   }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
    await cancelOrder(row.id)
    loadOrders()
    ElMessage.success('订单已取消')
  } catch (e) {
    // cancel
  }
}

const formatDate = (d) => d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'

// -- Helpers --
const getDiningMethodText = (method) => {
  const map = { 'DINE_IN': '堂食', 'TAKEOUT': '自提', 'DELIVERY': '外卖' }
  return map[method] || '自提'
}
const getDiningMethodTagType = (method) => {
  if (method === 'DELIVERY') return 'danger' // Reddish
  if (method === 'DINE_IN') return 'warning' // Orange
  return 'success' // Green for Takeout
}

const getMemberLevelClass = (level) => {
  if (level === 'diamond') return 'diamond'
  if (level === 'black') return 'black'
  return ''
}

const getOrderStatusText = (status) => {
  const map = { 
    'pending': '待处理', 
    'preparing': '制作中', 
    'completed': '已完成', 
    'cancelled': '已取消' 
  }
  return map[status] || status
}
const getOrderStatusTagType = (status) => {
  const map = { 
    'pending': 'danger', 
    'preparing': 'warning', 
    'completed': 'success', 
    'cancelled': 'info' 
  }
  return map[status] || 'info'
}

const getSpecTags = (item) => {
  if (!item) return []
  
  // v5.3 修复：更严格判断是否为烘焙类（甜品/配料），避免显示饮品参数
  // 甜品不应该有 temperature 和 cupSize
  const isBakeryItem = !item.temperature && !item.cupSize
  if (isBakeryItem) {
    return []
  }

  const tags = []
  
  const mapTemp = { 'HOT': '热', 'COLD': '冰', 'WARM': '温', 'iced': '冰', 'hot': '热', 'warm': '温' }
  const mapSugar = { 'NONE': '无糖', 'LIGHT': '微甜', 'STANDARD': '标准甜', 'MEDIUM': '少甜', 'LESS': '少糖', 'HALF': '半糖', 'none': '无糖', 'light': '微甜', 'standard': '标准甜', 'less': '少甜', 'half': '半糖', 'full': '标准甜' }
  const mapSize = { 'STANDARD': '标准杯', 'LARGE': '大杯', 'MEDIUM': '中杯', 'standard': '标准杯', 'large': '大杯', 'medium': '中杯' }
  const mapStrength = { 'STRONG': '加浓', 'NORMAL': '标准浓' }

  // v5.3 修复：只显示非默认值的参数
  // 杯型（如果存在）
  if (item.cupSize) tags.push(mapSize[item.cupSize] || item.cupSize)
  
  // 温度
  if (item.temperature) tags.push(mapTemp[item.temperature] || item.temperature)
  
  // 甜度（只在非标准时显示）
  if (item.sugarLevel && item.sugarLevel !== 'STANDARD' && item.sugarLevel !== 'standard') {
    tags.push(mapSugar[item.sugarLevel] || item.sugarLevel)
  }
  
  // 浓度（只在加浓时显示）
  if (item.coffeeStrength && item.coffeeStrength === 'STRONG') {
    tags.push(mapStrength[item.coffeeStrength])
  }

  // v5.3 修复：奶类只在非默认值时显示（避免美式显示"全脂奶"）
  const milkMap = { 'OAT': '燕麦奶', 'SOY': '豆奶', 'COCONUT': '椰奶', 'WHOLE': '全脂奶', 'SKIM': '脱脂奶' }
  
  // 只有明确选择了非标准奶才显示
  if (item.milkType && item.milkType !== 'WHOLE') {
    tags.push(milkMap[item.milkType] || item.milkType)
  } else if (item.optionsJson) {
    try {
      const opts = JSON.parse(item.optionsJson)
      // Check both 'milkType' (standard) and 'milk' (legacy) keys
      const m = opts.milkType || opts.milk
      if (m) {
         tags.push(milkMap[m] || m)
      }
    } catch (e) { /* ignore parse error */ }
  }
  
  return tags
}

// SSE
let unsubscribeSse = null
onMounted(() => {
  loadOrders()
  secondTicker = window.setInterval(() => {
    nowTs.value = Date.now()
  }, 1000)
  expireSyncTimer = window.setInterval(() => {
    const hasZeroPending = orders.value.some((o) => o.status === 'pending' && getRemainingSeconds(o) === 0)
    if (hasZeroPending) {
      loadOrders()
    }
  }, 2000)
  pollingTimer = window.setInterval(() => {
    loadOrders()
  }, 8000)

  document.addEventListener('visibilitychange', handleVisibilityChange)

  unsubscribeSse = sseService.on('new_order', () => {
    hasNewData.value = true
    loadOrders()
    if (delayedRefreshTimer) {
      window.clearTimeout(delayedRefreshTimer)
    }
    delayedRefreshTimer = window.setTimeout(() => {
      loadOrders()
    }, 1200)
  })
})

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    loadOrders()
  }
}

onUnmounted(() => {
  if (unsubscribeSse) unsubscribeSse()
  if (secondTicker) window.clearInterval(secondTicker)
  if (expireSyncTimer) window.clearInterval(expireSyncTimer)
  if (pollingTimer) window.clearInterval(pollingTimer)
  if (delayedRefreshTimer) window.clearTimeout(delayedRefreshTimer)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped lang="scss">
.orders-page {
  :deep(.admin-page-header) { margin-bottom: 12px; }
}

/* Quick Filter Tabs */
.quick-filter-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  
  .filter-tab {
    padding: 8px 20px;
    border-radius: 20px;
    border: 1px solid #E5E7EB;
    background: #FFF;
    color: #6B7280;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      background: #F9FAFB;
      color: #374151;
    }
    
    &.active {
      background: #374151;
      color: #FFF;
      border-color: #374151;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    }
  }
  
  /* Highlight 'Processing' Tab */
  .filter-tab.highlight-processing {
    display: flex;
    align-items: center;
    gap: 6px;
    .processing-icon {
       animation: spin 2s linear infinite;
    }
    &.active {
       background: linear-gradient(135deg, #D97706, #B45309);
       border-color: #D97706;
    }
  }
}

@keyframes spin { 100% { transform: rotate(360deg); } }


.compact-filter {
  margin-bottom: 12px !important;
  background: white;
  border-radius: 8px;
  padding: 16px 20px !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #E4E4E7;
}

.table-card {
  border: 1px solid #E4E4E7;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
}

:deep(.el-table__header-wrapper) {
  background: #FDFBF7; /* Warm Beige */
}
:deep(.warm-header th) {
  background-color: #FDFBF7 !important;
  color: #5D4D40;
  font-weight: 600;
  font-size: 13px;
  height: 48px;
}

:deep(.el-table__row) {
  td { padding: 16px 0; }
}

/* 1. Order Info */
.order-info-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.order-info-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dining-tag { 
  font-weight: 600; 
  border-radius: 4px;
}
.pickup-code-b {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  font-family: monospace;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.user-inline-name {
  font-size: 13px;
  color: #4B5563;
  font-weight: 500;
}
/* .user-inline-name.diamond { color: #8B5CF6; } */
/* .user-inline-name.black { color: #3E2723; } */
.user-phone-small { font-size: 13px; color: #6B7280; font-family: monospace; }

.member-emoji { font-size: 14px; margin-left: 4px; line-height: 1; }
/* .member-icon { ... } removed */
.verify-diamond { color: #8B5CF6; } /* Kept for text color if needed */
.verify-black { color: #D4AF37; }

.order-no {
  font-size: 12px;
  color: #9CA3AF;
  font-family: monospace;
}

/* 3. Product */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.product-item {
  font-size: 13px;
}
.prod-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.prod-name {
  color: #111827;
  font-weight: 500;
}
.prod-qty {
  font-size: 12px;
  color: #6B7280;
  background: #F3F4F6;
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 6px;
  display: inline-block;
}

.specs-capsules {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.spec-capsule {
  font-size: 11px;
  color: #4B5563;
  background: #F7F7F7;
  padding: 2px 8px;
  border-radius: 10px;
  border: 1px solid #E5E7EB;
}

.more-items {
  color: #9CA3AF;
  letter-spacing: 2px;
}
.product-summary { color: #6B7280; font-size: 13px; }

/* 4. Amount */
.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.pay-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pay-amount {
  font-weight: 700;
  font-size: 15px;
  color: #111827;
}
.coupon-tag {
  height: 18px;
  line-height: 16px;
  padding: 0 4px;
  font-size: 10px;
}
.original-price {
  font-size: 12px;
  color: #9CA3AF;
  text-decoration: line-through;
}

/* 5. Status */
.status-capsule {
  border-radius: 12px;
  padding: 0 12px;
  height: 24px;
  line-height: 22px;
}

.status-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.expire-text {
  font-size: 11px;
  color: #6B7280;
}

.expire-text.urgent {
  color: #DC2626;
  font-weight: 600;
}

/* 6. Time */
.time-text {
  font-weight: 500;
  color: #374151;
  font-size: 13px;
}
.date-text-small {
  font-size: 11px;
  color: #9CA3AF;
}

/* 7. Actions */
.action-links {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}
.action-icon-btn {
  font-size: 16px;
  color: #4B5563;
  &:hover { color: #111827; background: #F3F4F6; border-radius: 4px; }
  padding: 4px;
  height: auto;
}

.pagination-wrapper { margin-top: 0; display: flex; justify-content: flex-end; padding: 16px; border-top: 1px solid #F4F4F5; }
.tab-badge {
    display: inline-block;
    padding: 0 6px;
    font-size: 11px;
    line-height: 16px;
    border-radius: 10px;
    margin-left: 6px;
    font-weight: 700;
    vertical-align: middle;
}

.tab-badge.red {
    background-color: #EF4444; /* Tailwind red-500 */
    color: white;
}

.tab-badge.orange {
    background-color: #F59E0B; /* Tailwind amber-500 */
    color: white;
}

.tab-badge.gray {
    background-color: #E5E7EB;
    color: #374151;
}

/* Active 状态下的 badge 颜色调整 */
.filter-tab.active .tab-badge.red {
    background-color: white;
    color: #EF4444;
}

.filter-tab.active .tab-badge.orange {
    background-color: white;
    color: #D97706;
}

.filter-tab.active .tab-badge.gray {
    background-color: white;
    color: #374151;
}

.processing-icon {
    margin-right: 4px;
    animation: spin 2s linear infinite;
}

</style>
