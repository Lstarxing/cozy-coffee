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

    <AdminFilterBar class="compact-filter" @search="handleSearch" @reset="resetFilters">
      <el-form-item label="搜索">
        <el-input
          v-model="filters.keyword"
          placeholder="订单号 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
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

    <el-alert
      v-if="orderServiceUnavailable"
      type="error"
      :closable="false"
      show-icon
      title="订单服务暂不可用（OrderService未注册到Nacos），页面已自动降频重试。"
      class="service-alert"
    />

    <!-- 4. 快捷筛选标签栏 -->
    <div class="quick-filter-tabs">
       <button
         class="filter-tab"
         :class="{ active: filters.status === '' }"
         @click="handleQuickFilter('')"
       >
         全部
         <span v-if="orderCounts.total > 0" class="tab-badge gray">{{ orderCounts.total }}</span>
       </button>
       <button
         class="filter-tab"
         :class="{ active: filters.status === 'pending' }"
         @click="handleQuickFilter('pending')"
       >
         待支付
         <span v-if="orderCounts.pending > 0" class="tab-badge red">{{ orderCounts.pending }}</span>
       </button>
       <button
         class="filter-tab highlight-processing"
         :class="{ active: filters.status === 'preparing' }"
         @click="handleQuickFilter('preparing')"
       >
         <el-icon v-if="filters.status === 'preparing'" class="processing-icon"><Loading /></el-icon>
         制作中
         <span v-if="orderCounts.preparing > 0" class="tab-badge orange">{{ orderCounts.preparing }}</span>
       </button>
       <button
         class="filter-tab"
         :class="{ active: filters.status === 'completed' }"
         @click="handleQuickFilter('completed')"
       >
          已完成
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
        v-loading="loading"
        :data="paginatedOrders"
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
                 <span v-if="row.phoneMasked" class="user-phone-small">{{ row.phoneMasked }}</span>
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
            <div v-if="row.items && row.items.length" class="product-list">
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
              <span v-if="row.totalAmount > row.payAmount" class="original-price">¥{{ row.totalAmount }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 5. 状态 -->
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag
                :type="getOrderStatusTagType(getDisplayStatus(row))"
                effect="light"
                round
                class="status-capsule"
              >
                {{ getOrderStatusText(getDisplayStatus(row)) }}
              </el-tag>
              <OrderCountdown
                v-if="row.status === 'pending'"
                :expire-at="row.expireAt"
                :created-at="row.createdAt"
              />
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

              <!-- Pending: Cancel（待支付不显示接单，支付后移动端自动接单） -->
              <template v-if="getDisplayStatus(row) === 'pending'">
                <el-button link type="danger" @click="handleCancel(row)">取消</el-button>
              </template>

              <!-- Preparing: Complete -->
              <template v-else-if="getDisplayStatus(row) === 'preparing'">
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
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Printer, Loading } from '@element-plus/icons-vue'
import { completeOrder, cancelOrder } from '@/api'
import { ORDER_STATUS_MAP, DINING_METHOD_MAP, ORDER_SPEC } from '@/constants/order'
import { useOrderList } from '@/composables/useOrderList'
import AdminPageHeader from '@/components/ui/AdminPageHeader.vue'
import AdminFilterBar from '@/components/ui/AdminFilterBar.vue'
import TableToolbar from '@/components/ui/TableToolbar.vue'
import NewDataBanner from '@/components/NewDataBanner.vue'
import OrderDetailDialog from '@/components/OrderDetailDialog.vue'
import OrderCountdown from '@/components/OrderCountdown.vue'

// -- composable: all data-fetching, SSE, timers, lifecycle --
const {
  orders,
  orderCounts,
  loading,
  hasNewData,
  lastUpdated,
  orderServiceUnavailable,
  filters,
  currentPage,
  pageSize,
  paginatedOrders,
  loadOrders,
  handleSearch,
  resetFilters,
  handleQuickFilter,
  handleNewDataRefresh,
  getDisplayStatus,
  formatDate
} = useOrderList()

// -- dialogs --
const detailDialogVisible = ref(false)
const selectedOrderId = ref(null)

// -- order actions --
const viewDetail = (row) => {
  selectedOrderId.value = row.id
  detailDialogVisible.value = true
}

const handlePrint = (row) => {
  ElMessage.success('已发送至打印机: ' + row.pickupCode)
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
    // user cancelled the confirm dialog
  }
}

// -- display helpers --
const getOrderStatusText = (status) => ORDER_STATUS_MAP[status]?.label || status
const getOrderStatusTagType = (status) => ORDER_STATUS_MAP[status]?.tagType || 'info'

const getDiningMethodText = (method) => DINING_METHOD_MAP[method] || '自提'
const getDiningMethodTagType = (method) => {
  if (method === 'DELIVERY') return 'danger'
  return 'success'
}

const getSpecTags = (item) => {
  if (!item) return []

  const isBakeryItem = !item.temperature && !item.cupSize
  if (isBakeryItem) return []

  const tags = []
  const { TEMP_MAP, SUGAR_MAP, SIZE_MAP, STRENGTH_MAP } = ORDER_SPEC

  if (item.cupSize) tags.push(SIZE_MAP[item.cupSize] || item.cupSize)
  if (item.temperature) tags.push(TEMP_MAP[item.temperature] || item.temperature)

  if (item.sugarLevel && item.sugarLevel !== 'STANDARD' && item.sugarLevel !== 'standard') {
    tags.push(SUGAR_MAP[item.sugarLevel] || item.sugarLevel)
  }

  if (item.coffeeStrength && item.coffeeStrength === 'STRONG') {
    tags.push(STRENGTH_MAP[item.coffeeStrength])
  }

  const milkMap = { OAT: '燕麦奶', SOY: '豆奶', COCONUT: '椰奶', WHOLE: '全脂奶', SKIM: '脱脂奶' }
  if (item.milkType && item.milkType !== 'WHOLE') {
    tags.push(milkMap[item.milkType] || item.milkType)
  } else if (item.optionsJson) {
    try {
      const opts = JSON.parse(item.optionsJson)
      const m = opts.milkType || opts.milk
      if (m && m !== 'WHOLE') tags.push(milkMap[m] || m)
    } catch (e) { /* ignore parse error */ }
  }

  return tags
}
</script>

<style scoped lang="scss">
.orders-page {
  :deep(.admin-page-header) { margin-bottom: 12px; }
}

.service-alert {
  margin-bottom: 12px;
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
.user-phone-small { font-size: 13px; color: #6B7280; font-family: monospace; }

.member-emoji { font-size: 14px; margin-left: 4px; line-height: 1; }
.verify-diamond { color: #8B5CF6; }
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
    background-color: #EF4444;
    color: white;
}

.tab-badge.orange {
    background-color: #F59E0B;
    color: white;
}

.tab-badge.gray {
    background-color: #E5E7EB;
    color: #374151;
}

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
