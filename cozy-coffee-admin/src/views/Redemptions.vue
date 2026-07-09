<template>
  <div class="redemptions-page">
    <AdminPageHeader title="兑换订单管理" subtitle="积分商城兑换履约与管理">
       <template #extra>
         <NewDataBanner 
            :visible="hasNewData" 
            message="有新兑换订单，点击刷新"
            @refresh="handleNewDataRefresh"
            @dismiss="hasNewData = false"
          />
      </template>
    </AdminPageHeader>

    <AdminFilterBar class="compact-filter" @search="handleSearch" @reset="resetFilters">
      <el-form-item label="关键词">
        <el-input 
          v-model="filters.keyword" 
          placeholder="订单号/手机号/商品名" 
          clearable 
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="交付方式">
        <el-select v-model="filters.fulfillmentType" placeholder="全部" clearable style="width: 120px" @change="handleSearch">
          <el-option label="虚拟发放" value="VIRTUAL" />
          <el-option label="到店自提" value="PICKUP" />
          <el-option label="快递配送" value="DELIVERY" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable style="width: 110px" @change="handleSearch">
          <el-option label="待处理" value="pending" />
          <el-option label="配货中" value="processing" />
          <el-option label="已发货" value="shipped" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
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

    <el-card shadow="never" class="table-card compact-card">
       <TableToolbar :last-updated="lastUpdated" @refresh="loadRedemptions" />

       <el-table 
        v-loading="loading" 
        :data="paginatedRedemptions"
        stripe
        size="default"
        header-row-class-name="warm-header"
       >
        <!-- 1. 兑换内容 (主信息列) -->
        <el-table-column label="兑换内容" min-width="260">
           <template #default="{ row }">
             <div class="column-content-main">
               <div class="primary-row">
                 <el-tag 
                    :type="row.fulfillmentType === 'VIRTUAL' ? 'primary' : (row.fulfillmentType === 'DELIVERY' ? 'success' : 'warning')" 
                    size="small" 
                    effect="dark"
                    class="type-tag"
                  >
                    {{ row.fulfillmentType === 'VIRTUAL' ? '虚拟券' : (row.fulfillmentType === 'DELIVERY' ? '快递邮寄' : '实物·到店') }}
                  </el-tag>
                 <span class="product-name-bold">{{ row.productName }}</span>
                 <span class="qty-badge">x{{ row.quantity }}</span>
               </div>
               <div class="secondary-row">
                 <span class="order-no-small">订单：{{ row.orderNo }}</span>
               </div>
             </div>
           </template>
        </el-table-column>

        <!-- 2. 会员 (新增列) -->
        <el-table-column label="会员" width="180">
          <template #default="{ row }">
            <div class="member-cell">
              <span class="member-name">{{ row.nickname || '匿名用户' }}</span>
              <span class="member-phone">{{ row.phoneMasked || (row.receiverPhone ? maskPhone(row.receiverPhone) : '--') }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 3. 消耗积分 -->
        <el-table-column label="消耗积分" width="140" align="center">
          <template #default="{ row }">
            <div class="points-cell" :class="{ 'high-value': row.pointsCost >= 1000 }">
              <span class="points-num">{{ row.pointsCost }}</span>
              <span class="points-suffix">积分</span>
            </div>
          </template>
        </el-table-column>

        <!-- 4. 履约状态 / 凭证 -->
        <el-table-column label="履约状态 / 凭证" width="200" align="center">
          <template #default="{ row }">
            <div class="fulfillment-cell">
              <!-- 实物订单 -->
              <template v-if="row.fulfillmentType !== 'VIRTUAL'">
                 <div v-if="row.pickupCode" class="pickup-code-large">{{ row.pickupCode }}</div>
                 <el-tag :type="getStatusTagType(row.status)" size="small" effect="plain" class="status-tag-pill">
                    {{ getStatusText(row.status) }}
                 </el-tag>
              </template>
              
              <!-- 虚拟订单 -->
              <template v-else>
                 <el-tag :type="row.status === 'completed' ? 'success' : 'primary'" size="small" effect="light" class="status-tag-pill">
                    {{ row.status === 'completed' ? '已发放 / 未使用' : '已核销' }}
                 </el-tag>
                 <!-- 这里假设 status completed 代表发货完成即发放 -->
              </template>
            </div>
          </template>
        </el-table-column>

        <!-- 5. 兑换时间 -->
        <el-table-column label="兑换时间" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <!-- 6. 操作 -->
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
             <div class="action-links">
                <!-- 实物订单核验 -->
                <el-button 
                  v-if="row.fulfillmentType === 'PICKUP' && row.status === 'processing'" 
                  link type="primary" 
                  class="action-link"
                  @click="handleComplete(row)"
                >核验</el-button>

                <!-- 虚拟券查看/隐藏交互 -->
                <div v-if="row.fulfillmentType === 'VIRTUAL'" class="virtual-action-group">
                   <template v-if="visibleCodes[row.id]">
                      <span class="revealed-code">{{ row.virtualCode || 'CC-ERROR' }}</span>
                      <el-tooltip content="点击隐藏" placement="top" :hide-after="0">
                        <EyeOff :size="14" class="hide-icon" @click="toggleCodeVisibility(row.id)" />
                      </el-tooltip>
                   </template>
                   <el-button 
                      v-else
                      link type="primary" 
                      class="action-link"
                      @click="toggleCodeVisibility(row.id)"
                    >查看券码</el-button>
                </div>
                
                <!-- 发货 -->
                <el-button 
                  v-if="row.status === 'processing' && row.fulfillmentType === 'DELIVERY'" 
                  link type="warning" 
                  class="action-link"
                  @click="showShipDialog(row)"
                >发货</el-button>

                <!-- 配货 -->
                <el-button 
                  v-if="row.status === 'pending' && row.fulfillmentType !== 'VIRTUAL'" 
                  link type="primary" 
                  class="action-link"
                  @click="handleProcess(row)"
                >配货</el-button>

                <!-- 详情 -->
                <el-button link class="action-link detail-link" @click="viewDetail(row)">详情</el-button>
             </div>
          </template>
        </el-table-column>
       </el-table>

       <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="redemptions.length" 
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>

    <!-- 发货对话框 -->
    <el-dialog v-model="shipDialogVisible" title="填写物流信息" width="480px">
      <el-form :model="shipForm" label-width="100px" label-position="right">
        <el-form-item label="快递公司">
          <el-select v-model="shipForm.company" placeholder="选择快递公司" style="width: 100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="申通快递" value="申通快递" />
            <el-option label="邮政EMS" value="邮政EMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="shipDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmShip">确认发货</el-button>
        </div>
      </template>
    </el-dialog>

    <RedemptionDetailDialog
      v-model="detailDialogVisible"
      :order-id="selectedOrderId"
      @refresh="loadRedemptions"
      @ship="showShipDialog"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRedemptions, processRedemption, shipRedemption, completeRedemption, deleteRedemption } from '../api'
import sseService from '../api/sse'
import dayjs from 'dayjs'
import { EyeOff } from 'lucide-vue-next'

import AdminPageHeader from '../components/ui/AdminPageHeader.vue'
import AdminFilterBar from '../components/ui/AdminFilterBar.vue'
import TableToolbar from '../components/ui/TableToolbar.vue'
import StatusTag from '../components/ui/StatusTag.vue'
import CopyText from '../components/ui/CopyText.vue'
import NewDataBanner from '../components/NewDataBanner.vue'
import RedemptionDetailDialog from '../components/RedemptionDetailDialog.vue'

// Logic
const loading = ref(false)
const redemptions = ref([])
const hasNewData = ref(false)
const lastUpdated = ref('')

// Filter State
const filters = reactive({
  keyword: '',
  status: '',
  fulfillmentType: '',
  dateRange: null
})

const currentPage = ref(1)
const pageSize = ref(10)

const shipDialogVisible = ref(false)
const shipForm = ref({ company: '', trackingNo: '' })
const shippingOrder = ref(null)

const detailDialogVisible = ref(false)
const selectedOrderId = ref(null)
const visibleCodes = reactive({}) // State for toggling code visibility

// Computed for pagination
const paginatedRedemptions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return redemptions.value.slice(start, start + pageSize.value)
})

// Actions
const loadRedemptions = async () => {
  loading.value = true
  try {
    const params = {
        keyword: filters.keyword,
        status: filters.status,
        startDate: filters.dateRange ? filters.dateRange[0] : null,
        endDate: filters.dateRange ? filters.dateRange[1] : null
    }
    
    const res = await getRedemptions(params)
    let list = res.data || []
    
    // Client-side filtering for properties missed in Backend if any
    if (filters.fulfillmentType) {
        list = list.filter(o => o.fulfillmentType === filters.fulfillmentType)
    }
    
    // Sorting (Backend might not sort)
    list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    
    redemptions.value = list
    lastUpdated.value = dayjs().format('HH:mm:ss')
  } catch (e) {
    console.error('加载兑换订单失败:', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { loadRedemptions() }

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  filters.fulfillmentType = ''
  filters.dateRange = null
  loadRedemptions()
}

const handleNewDataRefresh = () => {
  hasNewData.value = false
  loadRedemptions()
}

const viewDetail = (row) => {
  selectedOrderId.value = row.id // Ensure ID is passed as Number (row.id is Long)
  detailDialogVisible.value = true
}

const handleProcess = async (row) => {
  try {
    await processRedemption(row.id)
    loadRedemptions()
    ElMessage.success('已开始备货')
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

const showShipDialog = (row) => {
  shippingOrder.value = row
  shipForm.value = { company: '', trackingNo: '' }
  shipDialogVisible.value = true
}

const confirmShip = async () => {
  if (!shipForm.value.company || !shipForm.value.trackingNo) {
    ElMessage.warning('请填写完整的物流信息')
    return
  }
  try {
    await shipRedemption(shippingOrder.value.id, shipForm.value.company, shipForm.value.trackingNo)
    loadRedemptions()
    ElMessage.success('已发货')
    shipDialogVisible.value = false
  } catch (e) {
    ElMessage.error('发货失败: ' + e.message)
  }
}

const handleComplete = async (row) => {
  await ElMessageBox.confirm('确认该订单已完成？', '提示')
  try {
    await completeRedemption(row.id)
    loadRedemptions()
    ElMessage.success('订单已完成')
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

const handleDelete = async (row) => {
  try {
    await deleteRedemption(row.id)
    ElMessage.success('订单已删除')
    loadRedemptions()
  } catch (e) {
    ElMessage.error('删除订单失败: ' + e.message)
  }
}

const toggleCodeVisibility = (id) => {
  visibleCodes[id] = !visibleCodes[id]
}

// Helpers
const getStatusTagType = (status) => {
  const map = {
    pending: 'warning',
    processing: 'primary',
    shipped: 'primary',
    completed: 'success',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending: '待处理',
    processing: '待取货/发货',
    shipped: '配送中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'

const maskPhone = (phone) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// SSE
let unsubscribeSse = null
onMounted(() => {
  loadRedemptions()
  unsubscribeSse = sseService.on('new_redemption', () => {
    hasNewData.value = true
  })
})
onUnmounted(() => {
  if (unsubscribeSse) unsubscribeSse()
})
</script>

<style scoped lang="scss">
.redemptions-page {
  :deep(.admin-page-header) { margin-bottom: 24px; }
}

/* Page Config */
.compact-filter {
  margin-bottom: 24px !important;
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
  background: #FDFBF7; /* Warm Beige Header */
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

/* 1. Content Column */
.column-content-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  
  .primary-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    
    .type-tag { font-weight: 600; border: none; }
    .product-name-bold { font-weight: 700; color: #18181B; font-size: 14px; }
    .qty-badge { color: #52525B; font-size: 13px; background: #F4F4F5; padding: 2px 6px; border-radius: 4px; font-weight: 500;}
  }

  .secondary-row {
     .order-no-small { color: #A1A1AA; font-size: 12px; font-family: monospace; }
  }
}

/* 2. Member Column */
.member-cell {
  display: flex;
  flex-direction: column;
  
  .member-name { font-weight: 600; color: #27272A; font-size: 14px; }
  .member-phone { color: #71717A; font-size: 12px; font-family: monospace; margin-top: 2px; }
}

/* 3. Points Cell */
.points-cell {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
  
  .points-num { font-size: 18px; font-weight: 700; color: #27272A; font-family: 'Inter', sans-serif; letter-spacing: -0.5px; }
  .points-suffix { font-size: 12px; color: #71717A; }
  
  &.high-value .points-num { color: #D97706; /* Gold/Amber */ }
  &.high-value .points-suffix { color: #B45309; }
}

/* 4. Fulfillment Cell */
.fulfillment-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  
  .pickup-code-large {
     font-size: 18px;
     font-weight: 800;
     color: #18181B;
     letter-spacing: 1px;
     font-family: monospace;
     background: #F4F4F5;
     padding: 2px 10px;
     border-radius: 6px;
  }
  .status-tag-pill {
     border-radius: 12px;
     padding: 0 10px;
     height: 22px;
     line-height: 20px;
     font-weight: 500;
  }
}

/* 5. Time */
.time-text {
  color: #52525B;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
}

/* 6. Actions */
.action-links {
  display: flex;
  justify-content: center;
  gap: 12px;
  
  .action-link {
    font-size: 13px;
    font-weight: 500;
    padding: 0;
    height: auto;
    
    &.detail-link { color: #71717A; &:hover { color: #27272A; } }
  }
}

/* Virtual Code Action Group */
.virtual-action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  
  .revealed-code {
    font-family: monospace;
    font-weight: 700;
    color: #D97706; /* Brand Gold */
    font-size: 14px;
    letter-spacing: 0.5px;
  }
  
  .hide-icon {
    color: #9CA3AF;
    cursor: pointer;
    transition: color 0.2s;
    &:hover { color: #4B5563; }
  }
}

.pagination-wrapper {
  margin-top: 0;
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #F4F4F5;
}
</style>
