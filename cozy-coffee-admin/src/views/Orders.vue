<template>
  <div class="orders-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="pending">待处理</el-radio-button>
            <el-radio-button label="preparing">制作中</el-radio-button>
            <el-radio-button label="completed">已完成</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 筛选表单 -->
      <div class="filter-form">
        <el-input v-model="orderNoFilter" placeholder="订单号" clearable style="width: 180px;" @change="handleFilterChange" />
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" 
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
          style="width: 260px; margin-left: 10px;" @change="handleFilterChange" />
        <el-button type="primary" style="margin-left: 10px;" @click="loadOrders">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="paginatedOrders" v-loading="loading" style="margin-top: 16px;">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="productName" label="商品" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="pickupCode" label="取餐码" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.pickupCode" type="warning" size="large" class="pickup-code">{{ row.pickupCode }}</el-tag>
            <span v-else class="no-code">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" type="primary" size="small" 
              @click="handleAccept(row)" :loading="row.loading">
              接单
            </el-button>
            <el-button v-if="row.status === 'preparing'" type="success" size="small" 
              @click="handleComplete(row)" :loading="row.loading">
              完成
            </el-button>
            <el-button v-if="row.status === 'pending' || row.status === 'preparing'" 
              type="danger" size="small" @click="handleCancel(row)" :loading="row.loading">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredOrders.length"
        layout="total, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrders, acceptOrder, completeOrder, cancelOrder } from '../api'

const loading = ref(false)
const allOrders = ref([])
const statusFilter = ref('')
const orderNoFilter = ref('')
const dateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)

// 根据状态和关键字筛选后的订单（按时间倒序）
const filteredOrders = computed(() => {
  let orders = allOrders.value
  if (statusFilter.value) {
    orders = orders.filter(o => o.status === statusFilter.value)
  }
  if (orderNoFilter.value) {
    orders = orders.filter(o => o.orderNo && o.orderNo.includes(orderNoFilter.value))
  }
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    orders = orders.filter(o => {
      if (!o.createdAt) return false
      const date = o.createdAt.split('T')[0]
      return date >= start && date <= end
    })
  }
  // 按创建时间倒序排列
  return orders.sort((a, b) => {
    if (a.createdAt && b.createdAt) {
      return new Date(b.createdAt) - new Date(a.createdAt)
    }
    return (b.id || 0) - (a.id || 0)
  })
})

// 当前页的订单
const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredOrders.value.slice(start, end)
})

const getStatusType = (status) => {
  const map = { pending: 'warning', preparing: 'primary', completed: 'success', cancelled: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待处理', preparing: '制作中', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString()
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params = { status: statusFilter.value || undefined }
    if (orderNoFilter.value) params.orderNo = orderNoFilter.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getOrders(params.status, params.orderNo, params.startDate, params.endDate)
    allOrders.value = (res.data || []).map(o => ({ ...o, loading: false }))
  } catch (e) {
    ElMessage.error('加载订单失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  statusFilter.value = ''
  orderNoFilter.value = ''
  dateRange.value = null
  currentPage.value = 1
  loadOrders()
}

const handleFilterChange = () => {
  currentPage.value = 1 // 切换过滤条件时重置到第一页
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleAccept = async (row) => {
  row.loading = true
  try {
    const res = await acceptOrder(row.id)
    row.status = res.data.status
    row.pickupCode = res.data.pickupCode
    ElMessage.success(`已接单，取餐码: ${res.data.pickupCode}`)
  } catch (e) {
    ElMessage.error('接单失败: ' + e.message)
  } finally {
    row.loading = false
  }
}

const handleComplete = async (row) => {
  row.loading = true
  try {
    await completeOrder(row.id)
    row.status = 'completed'
    ElMessage.success('订单已完成')
  } catch (e) {
    ElMessage.error('完成失败: ' + e.message)
  } finally {
    row.loading = false
  }
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
  row.loading = true
  try {
    await cancelOrder(row.id)
    row.status = 'cancelled'
    ElMessage.success('订单已取消')
  } catch (e) {
    ElMessage.error('取消失败: ' + e.message)
  } finally {
    row.loading = false
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.orders-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pickup-code {
  font-size: 18px;
  font-weight: 700;
  padding: 8px 12px;
}

.no-code {
  color: #999;
}
</style>

