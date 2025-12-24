<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon users">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.totalUsers }}</span>
            <span class="stat-label">总用户数</span>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon orders">
            <el-icon><List /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.todayOrders }}</span>
            <span class="stat-label">今日订单</span>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon revenue">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">¥{{ stats.todayRevenue }}</span>
            <span class="stat-label">今日营收</span>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon pending">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.pendingOrders }}</span>
            <span class="stat-label">待处理订单</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近订单 -->
    <el-card class="recent-orders">
      <template #header>
        <div class="card-header">
          <span>最近订单</span>
          <el-button text type="primary" @click="$router.push('/orders')">查看全部</el-button>
        </div>
      </template>
      
      <el-table :data="recentOrders" v-loading="ordersLoading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="productName" label="商品" />
        <el-table-column prop="totalAmount" label="金额">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="pickupCode" label="取餐码" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.pickupCode" type="warning" size="large">{{ row.pickupCode }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>  
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrders, getUsers } from '../api'

const stats = ref({
  totalUsers: 0,
  todayOrders: 0,
  todayRevenue: 0,
  pendingOrders: 0
})

const recentOrders = ref([])
const ordersLoading = ref(false)

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

const isToday = (dateStr) => {
  if (!dateStr) return false
  const d = new Date(dateStr)
  const today = new Date()
  return d.getFullYear() === today.getFullYear() &&
         d.getMonth() === today.getMonth() &&
         d.getDate() === today.getDate()
}

const loadStats = async () => {
  try {
    // 获取用户数
    const usersRes = await getUsers()
    stats.value.totalUsers = (usersRes.data || []).length

    // 获取订单统计
    const ordersRes = await getOrders()
    const allOrders = ordersRes.data || []
    
    // 今日订单（按创建时间筛选）
    const todayOrders = allOrders.filter(o => isToday(o.createdAt))
    stats.value.todayOrders = todayOrders.length
    
    // 今日营收（已完成的订单）
    const todayRevenue = todayOrders
      .filter(o => o.status !== 'cancelled')
      .reduce((sum, o) => sum + parseFloat(o.totalAmount || 0), 0)
    stats.value.todayRevenue = todayRevenue.toFixed(2)
    
    // 待处理订单
    stats.value.pendingOrders = allOrders.filter(o => o.status === 'pending').length

    // 最近5条订单（按时间倒序，createdAt为空时按id倒序）
    recentOrders.value = allOrders
      .sort((a, b) => {
        if (a.createdAt && b.createdAt) {
          return new Date(b.createdAt) - new Date(a.createdAt)
        }
        return (b.id || 0) - (a.id || 0)
      })
      .slice(0, 5)
  } catch (e) {
    console.error('加载统计失败:', e)
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-icon.users { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon.orders { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-icon.revenue { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.stat-icon.pending { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

.recent-orders {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
