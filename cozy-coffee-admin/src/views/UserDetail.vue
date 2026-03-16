<template>
  <div class="user-detail-page">
    <AdminPageHeader 
      title="用户详情" 
      show-back
    />

    <div v-loading="loading" class="content-wrapper" v-if="user">
       <!-- Top Row: Basic Info & Member Info -->
       <el-row :gutter="20">
          <el-col :span="14">
             <el-card shadow="never" class="mb-4">
                <template #header>
                   <div class="card-header">
                      <span class="title">基本信息</span>
                      <StatusTag :status="user.status" />
                   </div>
                </template>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="用户ID">{{ user.id }}</el-descriptions-item>
                  <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
                  <el-descriptions-item label="昵称">{{ user.nickname || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="会员码">{{ user.memberCode || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="邀请码">{{ user.inviteCode || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="注册时间">{{ formatDate(user.createdAt) }}</el-descriptions-item>
                  <el-descriptions-item label="角色">
                    <el-tag :type="user.role === 'admin' ? 'danger' : 'info'" size="small">
                      {{ user.role === 'admin' ? '管理员' : '普通用户' }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>
             </el-card>
          </el-col>

          <el-col :span="10">
             <el-card shadow="never" class="mb-4 member-card">
                <template #header>
                   <div class="card-header">
                      <span class="title">会员信息</span>
                      <el-button link type="primary" @click="showPointsDialog">调整积分</el-button>
                   </div>
                </template>
                
                <div class="member-stats">
                   <div class="stat-item">
                      <div class="label">会员等级</div>
                      <div class="value">
                         <span :class="['level-badge', `level-${user.memberLevel || 'basic'}`]">
                           {{ getLevelText(user.memberLevel) }}
                         </span>
                      </div>
                   </div>
                   <div class="stat-item">
                      <div class="label">成长值 (EXP)</div>
                      <div class="value exp-text">{{ user.expTotal || 0 }}</div>
                   </div>
                   <div class="stat-item">
                      <div class="label">当前积分</div>
                      <div class="value points-text">{{ user.currentPoints || 0 }}</div>
                   </div>
                   <div class="stat-item" v-if="user.expiringPoints > 0">
                      <div class="label">即将到期</div>
                      <div class="value expiring-text">{{ user.expiringPoints }}</div>
                   </div>
                </div>
             </el-card>
             
             <!-- Statistics -->
             <el-card shadow="never" class="mb-4">
                <template #header><span class="title">消费统计</span></template>
                <div class="simple-stat-row">
                   <span>咖啡订单: <b>{{ allTransactions.filter(o => o._type === 'coffee').length }}</b></span>
                   <span style="margin-left: 24px;">兑换订单: <b>{{ allTransactions.filter(o => o._type === 'redemption').length }}</b></span>
                </div>
             </el-card>
          </el-col>
       </el-row>

       <!-- Bottom Row: Orders -->
       <el-card shadow="never" class="table-card">
          <template #header>
             <div class="card-header">
                <span class="title">历史订单</span>
                <el-radio-group v-model="orderTypeFilter" size="small">
                   <el-radio-button value="all">全部</el-radio-button>
                   <el-radio-button value="coffee">咖啡订单</el-radio-button>
                   <el-radio-button value="redemption">兑换订单</el-radio-button>
                </el-radio-group>
             </div>
          </template>
          
          <el-table :data="filteredOrders" v-loading="ordersLoading" size="small" empty-text="暂无交易记录">
             <el-table-column label="类型" width="90" align="center">
                <template #default="{ row }">
                   <el-tag :type="row._type === 'coffee' ? '' : 'warning'" size="small" effect="plain">
                      {{ row._type === 'coffee' ? '咖啡' : '兑换' }}
                   </el-tag>
                </template>
             </el-table-column>
             <el-table-column prop="orderNo" label="订单号" width="180">
                <template #default="{ row }">
                   <span class="mono-font">{{ row.orderNo }}</span>
                </template>
             </el-table-column>
             <el-table-column prop="productName" label="商品" min-width="120" show-overflow-tooltip />
             <el-table-column label="金额/积分" width="120" align="right">
                <template #default="{ row }">
                   <span v-if="row._type === 'coffee'">¥{{ row.totalAmount }}</span>
                   <span v-else class="points-text">{{ row.pointsCost }} 积分</span>
                </template>
             </el-table-column>
             <el-table-column prop="status" label="状态" width="100" align="center">
                <template #default="{ row }">
                   <StatusTag :status="row.status" :type="row._type === 'coffee' ? 'order' : 'redemption'" />
                </template>
             </el-table-column>
             <el-table-column prop="createdAt" label="时间" width="160">
                <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
             </el-table-column>
          </el-table>
          
          <!-- 加载失败提示 -->
          <div v-if="ordersError" class="error-hint">
             <span>{{ ordersError }}</span>
             <el-button link type="primary" @click="loadUserOrders">重试</el-button>
          </div>
       </el-card>

    </div>

    <!-- 积分调整对话框 -->
    <el-dialog v-model="pointsDialogVisible" title="调整积分" width="480px">
      <el-form :model="pointsForm" label-width="80px">
         <el-form-item label="当前积分">
            <span class="text-xl font-bold">{{ user?.currentPoints || 0 }}</span>
         </el-form-item>
         <el-form-item label="调整数量">
            <el-input-number v-model="pointsForm.amount" :min="-9999" :max="9999" />
            <div class="text-secondary text-xs mt-1">正数增加，负数扣减</div>
         </el-form-item>
         <el-form-item label="调整原因">
            <el-input v-model="pointsForm.reason" placeholder="请输入调整原因（必填）" />
         </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
           <el-button @click="pointsDialogVisible = false">取消</el-button>
           <el-button type="primary" @click="submitPointsAdjust" :loading="adjusting">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserDetail, adjustUserPoints, updateUserStatus, getOrders, getRedemptions } from '../api'
import dayjs from 'dayjs'

import AdminPageHeader from '../components/ui/AdminPageHeader.vue'
import StatusTag from '../components/ui/StatusTag.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const ordersLoading = ref(false)
const ordersError = ref('')
const user = ref(null)
const allTransactions = ref([])
const orderTypeFilter = ref('all')

const pointsDialogVisible = ref(false)
const adjusting = ref(false)
const pointsForm = ref({ amount: 0, reason: '' })

const userId = ref(null)

// 根据 Tab 过滤订单
const filteredOrders = computed(() => {
  if (orderTypeFilter.value === 'all') return allTransactions.value
  return allTransactions.value.filter(o => o._type === orderTypeFilter.value)
})

const loadUser = async () => {
  loading.value = true
  try {
    const res = await getUserDetail(userId.value)
    user.value = res.data
  } catch (e) {
    ElMessage.error('加载用户信息失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const loadUserOrders = async () => {
  ordersLoading.value = true
  ordersError.value = ''
  
  try {
    // 并行请求咖啡订单和兑换订单（使用后端 userId 过滤）
    const [ordersRes, redemptionsRes] = await Promise.all([
      getOrders({ userId: userId.value }),
      getRedemptions({ userId: userId.value })
    ])
    
    // 咖啡订单规范化
    const coffeeOrders = (ordersRes.data || []).map(o => ({
      ...o,
      _type: 'coffee'
    }))
    
    // 兑换订单规范化
    const redemptionOrders = (redemptionsRes.data || []).map(o => ({
      ...o,
      _type: 'redemption'
    }))
    
    // 合并并按时间倒序排序
    allTransactions.value = [...coffeeOrders, ...redemptionOrders]
      .sort((a, b) => {
        const dateA = a.createdAt ? dayjs(a.createdAt) : dayjs(0)
        const dateB = b.createdAt ? dayjs(b.createdAt) : dayjs(0)
        return dateB.valueOf() - dateA.valueOf()
      })
      
  } catch (e) {
    console.error('加载订单失败:', e)
    ordersError.value = '加载订单失败: ' + (e.message || '请稍后重试')
  } finally {
    ordersLoading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const getLevelType = (level) => {
  const map = { basic: 'info', silver: '', gold: 'warning', black: 'danger' }
  return map[level] || 'info'
}

const getLevelText = (level) => {
  const map = { basic: '普通会员', silver: '银卡会员', gold: '金卡会员', diamond: '钻石会员', black: '黑卡会员' }
  return map[level] || level
}

const showPointsDialog = () => {
  pointsForm.value = { amount: 0, reason: '' }
  pointsDialogVisible.value = true
}

const submitPointsAdjust = async () => {
  if (!pointsForm.value.amount) {
    ElMessage.warning('请输入调整数量')
    return
  }
  if (!pointsForm.value.reason) {
    ElMessage.warning('请输入调整原因')
    return
  }
  
  adjusting.value = true
  try {
    await adjustUserPoints(userId.value, pointsForm.value.amount, pointsForm.value.reason)
    ElMessage.success('积分调整成功')
    pointsDialogVisible.value = false
    await loadUser()
  } catch (e) {
    ElMessage.error('积分调整失败: ' + e.message)
  } finally {
    adjusting.value = false
  }
}

const handleDisable = async () => {
  try {
    await ElMessageBox.confirm(
      '',
      '确认禁用该用户？',
      {
        confirmButtonText: '确认禁用',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true,
        message: `
          <div style="font-size: 14px; color: #374151; line-height: 1.6;">
            <p style="margin-bottom: 8px; font-weight: 500;">禁用后将产生以下影响：</p>
            <ul style="margin: 0; padding-left: 20px; color: #6B7280; list-style-type: disc;">
              <li>用户将无法登录小程序或商城</li>
              <li>用户无法进行下单、支付或积分兑换</li>
              <li>已有订单的履约流程不受影响</li>
              <li>您可以随时重新启用该用户</li>
            </ul>
          </div>
        `
      }
    )
    await updateUserStatus(userId.value, 'disabled')
    ElMessage.success('用户已禁用')
    await loadUser()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('禁用用户失败:', e)
      ElMessage.error(e.message || '禁用失败，请稍后重试')
    }
  }
}

const handleEnable = async () => {
  try {
    await updateUserStatus(userId.value, 'active')
    ElMessage.success('用户已启用')
    await loadUser()
  } catch (e) {
    console.error('启用用户失败:', e)
    ElMessage.error(e.message || '启用失败')
  }
}

onMounted(() => {
  userId.value = Number(route.params.id)
  if (userId.value) {
    loadUser()
    loadUserOrders()
  } else {
    ElMessage.error('用户ID无效')
    router.push('/users')
  }
})
</script>

<style scoped lang="scss">
.content-wrapper { text-align: left; }

.mb-4 { margin-bottom: 24px; } // Larger spacing for page layout

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .title {
    font-size: 16px;
    font-weight: 600;
    color: #111827;
  }
}

.member-card {
   .member-stats {
      display: flex;
      justify-content: space-around;
      padding: 16px 0;
      
      .stat-item {
         text-align: center;
         .label { color: #6B7280; font-size: 12px; margin-bottom: 8px; }
         .value { font-size: 20px; font-weight: 600; color: #111827; }
         .points-text { color: #B45309; }
         .exp-text { color: #8B5CF6; }
         .expiring-text { color: #EF4444; font-size: 16px; }
      }
   }
}

.simple-stat-row {
  display: flex;
  gap: 20px;
  color: #374151;
}

.mono-font {
  font-family: monospace;
}

.points-text {
  color: #B45309;
  font-weight: 500;
}

.error-hint {
  padding: 12px 0;
  color: #EF4444;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

// 会员等级样式
.level-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 500;
  
  &.level-basic {
    background: #F3F4F6;
    color: #6B7280;
  }
  
  &.level-silver {
    background: linear-gradient(135deg, #E5E7EB 0%, #D1D5DB 100%);
    color: #4B5563;
  }
  
  &.level-gold {
    background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
    color: #92400E;
  }
  
  &.level-diamond {
    background: linear-gradient(135deg, #E0E7FF 0%, #C7D2FE 100%);
    color: #4338CA;
    font-weight: 600;
  }

  &.level-black {
    background: linear-gradient(135deg, #1F2937 0%, #374151 100%);
    color: #F59E0B;
    font-weight: 600;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }
}

/* Fix description alignment */
:deep(.el-descriptions__cell) {
  vertical-align: top;
}
:deep(.el-descriptions__content) {
  word-break: break-all;
}
</style>
