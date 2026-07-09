<template>
  <div class="users-page">
    <AdminPageHeader title="用户管理" subtitle="查看与管理注册会员">
      <template #extra>
         <div class="text-secondary text-xs">共 {{ users.length }} 位会员</div>
      </template>
    </AdminPageHeader>

    <AdminFilterBar class="compact-filter" @search="handleSearch" @reset="resetFilters">
      <el-form-item label="关键词">
        <el-input 
          v-model="filters.keyword" 
          placeholder="用户名/手机号/ID" 
          clearable 
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
       <el-form-item label="会员等级">
        <el-select v-model="filters.memberLevel" placeholder="全部" clearable style="width: 120px" @change="handleSearch">
           <el-option label="普通会员" value="basic" />
           <el-option label="银卡会员" value="silver" />
           <el-option label="金卡会员" value="gold" />
           <el-option label="钻石会员" value="diamond" />
           <el-option label="黑卡会员" value="black" />
        </el-select>
      </el-form-item>
       <el-form-item label="注册时间">
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
       <TableToolbar :last-updated="lastUpdated" @refresh="loadUsers" />

       <el-table 
        v-loading="loading" 
        :data="paginatedUsers"
        size="small"
        header-cell-class-name="table-header"
       >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        
        <el-table-column label="用户信息" min-width="150">
           <template #default="{ row }">
              <div class="user-info">
                 <el-avatar :size="24" class="mr-2">{{ row.username.charAt(0).toUpperCase() }}</el-avatar>
                 <span class="font-medium">{{ row.username }}</span>
              </div>
           </template>
        </el-table-column>

        <el-table-column label="手机号" width="120">
           <template #default="{ row }">{{ row.phoneMasked || row.phone || '-' }}</template>
        </el-table-column>

        <el-table-column label="等级" width="100" align="center">
           <template #default="{ row }">
              <span :class="['level-badge', `level-${row.memberLevel || 'basic'}`]">
                 {{ getLevelText(row.memberLevel) }}
              </span>
           </template>
        </el-table-column>

        <el-table-column prop="currentPoints" label="积分" width="100" align="right">
           <template #default="{ row }">
              <span class="points-val">{{ row.currentPoints || 0 }}</span>
           </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间" width="160" align="center">
           <template #default="{ row }">
              <span class="text-secondary">{{ formatDate(row.createdAt) }}</span>
           </template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
           <template #default="{ row }">
              <StatusTag :status="row.status" />
           </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
           <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/users/${row.id}`)">详情</el-button>
              <el-divider direction="vertical" />
              <el-button link type="primary" @click="handleAdjustPoints(row)">调分</el-button>
              <el-divider direction="vertical" />
              <el-button v-if="row.status !== 'disabled'" link type="danger" @click="handleDisable(row)">禁用</el-button>
              <el-button v-else link type="success" @click="handleEnable(row)">启用</el-button>
           </template>
        </el-table-column>
       </el-table>

       <div class="pagination-wrapper">
         <el-pagination
           v-model:current-page="currentPage"
           v-model:page-size="pageSize"
           :total="users.length"
           :page-sizes="[10, 20, 50]"
           layout="total, sizes, prev, pager, next"
           background
         />
       </div>
    </el-card>

    <!-- Points Dialog -->
    <el-dialog v-model="pointsDialogVisible" title="调整积分" width="400px">
      <el-form :model="pointsForm" label-width="80px">
         <el-form-item label="用户">
            {{ selectedUser?.username }}
         </el-form-item>
         <el-form-item label="当前积分">
            {{ selectedUser?.currentPoints }}
         </el-form-item>
         <el-form-item label="变动数量">
            <el-input-number v-model="pointsForm.amount" :min="-9999" :max="9999" />
            <div class="text-secondary text-xs mt-1">正数增加，负数扣减</div>
         </el-form-item>
         <el-form-item label="备注">
            <el-input v-model="pointsForm.reason" placeholder="原因" />
         </el-form-item>
      </el-form>
      <template #footer>
         <div class="dialog-footer">
            <el-button @click="pointsDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="submittingPoints" @click="submitPointsAdjust">确定</el-button>
         </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, adjustUserPoints, updateUserStatus } from '../api'
import dayjs from 'dayjs'

import AdminPageHeader from '../components/ui/AdminPageHeader.vue'
import AdminFilterBar from '../components/ui/AdminFilterBar.vue'
import TableToolbar from '../components/ui/TableToolbar.vue'
import StatusTag from '../components/ui/StatusTag.vue'

// State
const loading = ref(false)
const users = ref([])
const lastUpdated = ref('')

const filters = reactive({
  keyword: '',
  memberLevel: '',
  dateRange: null
})

const currentPage = ref(1)
const pageSize = ref(10)

// Points Dialog
const pointsDialogVisible = ref(false)
const selectedUser = ref(null)
const pointsForm = ref({ amount: 0, reason: '' })
const submittingPoints = ref(false)

// Computed
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return users.value.slice(start, start + pageSize.value)
})

// Actions
const loadUsers = async () => {
  loading.value = true
  try {
     const params = {
        keyword: filters.keyword,
        memberLevel: filters.memberLevel,
        startDate: filters.dateRange ? filters.dateRange[0] : null,
        endDate: filters.dateRange ? filters.dateRange[1] : null
     }
     const res = await getUsers(params)
     // 过滤掉管理员，只显示普通用户
     users.value = (res.data || []).filter(u => u.role !== 'admin')
     lastUpdated.value = dayjs().format('HH:mm:ss')
  } catch (e) {
     console.error(e)
     ElMessage.error('加载用户列表失败')
  } finally {
     loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadUsers() }
const resetFilters = () => {
   filters.keyword = ''
   filters.memberLevel = ''
   filters.dateRange = null
   handleSearch()
}

// Points
const handleAdjustPoints = (row) => {
   selectedUser.value = row
   pointsForm.value = { amount: 0, reason: '' }
   pointsDialogVisible.value = true
}

const submitPointsAdjust = async () => {
   if (!pointsForm.value.amount) return ElMessage.warning('请输入数量')
   if (!pointsForm.value.reason) return ElMessage.warning('请输入原因')
   
   submittingPoints.value = true
   try {
      await adjustUserPoints(selectedUser.value.id, pointsForm.value.amount, pointsForm.value.reason)
      ElMessage.success('调整成功')
      pointsDialogVisible.value = false
      loadUsers()
   } catch (e) {
      ElMessage.error(e.message)
   } finally {
      submittingPoints.value = false
   }
}

// Status
const handleDisable = async (row) => {
   try {
      await ElMessageBox.confirm('确定禁用该用户吗?', '警告', { type: 'warning' })
      await updateUserStatus(row.id, 'disabled')
      ElMessage.success('已禁用')
      loadUsers()
   } catch (e) {}
}

const handleEnable = async (row) => {
   try {
      await updateUserStatus(row.id, 'active')
      ElMessage.success('已启用')
      loadUsers()
   } catch (e) {}
}

// Helpers
const formatDate = (d) => d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '-'
const getLevelText = (lvl) => ({ basic:'普通', silver:'银卡', gold:'金卡', diamond:'钻石', black:'黑卡' }[lvl] || '普通')

onMounted(() => {
   loadUsers()
})
</script>

<style scoped lang="scss">
.users-page {
  :deep(.admin-page-header) { margin-bottom: 12px; }
}

.compact-filter {
  margin-bottom: 12px !important;
  padding: 12px 16px !important;
}

.compact-card {
  :deep(.el-card__body) { padding: 12px 16px; }
  :deep(.el-table__row td) { padding: 8px 0; }
}

.user-info { display: flex; align-items: center; }
.mr-2 { margin-right: 8px; }
.font-medium { font-weight: 500; }

.level-badge {
  /* 基础形态：更像成熟后台的徽章 */
  display: inline-flex;
  align-items: center;
  gap: 6px;

  height: 22px;
  padding: 0 10px;
  border-radius: 999px;

  font-size: 12px;
  line-height: 20px;
  font-weight: 600;
  letter-spacing: 0.2px;

  border: 1px solid transparent;
  user-select: none;
  white-space: nowrap;

  /* 轻微质感（非常克制，避免 AI 味） */
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);

  /* 可选：如果你 badge 里有 icon（例如 <el-icon>），统一一下大小 */
  :deep(.el-icon) {
    font-size: 12px;
    opacity: 0.95;
  }

  /* 普通会员：中性描边 + 浅底 */
  &.level-basic {
    background: #fafafa;
    border-color: #e5e7eb;
    color: #6b7280;
  }

  /* 白银会员：冷灰银（不要太黑） */
  &.level-silver {
    background: #f8fafc;
    border-color: #d1d5db;
    color: #374151;
  }

  /* 黄金会员：保留“好看”的优势，但更精致：金色描边 + 极浅金底 */
  &.level-gold {
    background: #fffbeb;
    border-color: #f59e0b;
    color: #92400e;
    box-shadow:
      inset 0 0 0 1px rgba(245, 158, 11, 0.10);
  }

  /* 钻石会员：蓝紫渐变 */
  &.level-diamond {
    background: linear-gradient(135deg, #e0f2fe 0%, #e0e7ff 100%);
    border-color: #6366f1;
    color: #4338ca;
    box-shadow:
      inset 0 0 0 1px rgba(99, 102, 241, 0.15);
  }

  /* 黑金会员：避免纯黑压抑，改深蓝黑 + 金色字 + 金色描边 */
  &.level-black {
    background: #111827;
    border-color: rgba(245, 158, 11, 0.45);
    color: #fbbf24;
    box-shadow:
      inset 0 0 0 1px rgba(255, 255, 255, 0.06);
  }

  /* 表格更紧凑时（你已在 compact-card 降低了行 padding），可选开启 */
  &.is-compact {
    height: 20px;
    padding: 0 8px;
    font-size: 11px;
  }
}

.points-val { font-family: monospace; font-weight: 600; color: #B45309; }
.text-secondary { color: #9CA3AF; }
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
