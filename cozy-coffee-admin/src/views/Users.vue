<template>
  <div class="users-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-input v-model="searchKeyword" placeholder="搜索用户名/手机号" 
            style="width: 240px;" clearable @input="handleSearch">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="paginatedUsers" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="会员等级" width="120">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.memberLevel)">
              {{ getLevelText(row.memberLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前积分" width="100">
          <template #default="{ row }">
            <span class="points-value">{{ row.currentPoints || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计积分" width="100">
          <template #default="{ row }">
            <span class="points-total">{{ row.totalPoints || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button text type="primary" @click="openPointsDialog(row)">调整积分</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredUsers.length"
        layout="total, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 积分调整对话框 -->
    <el-dialog v-model="dialogVisible" title="调整积分" width="400px">
      <el-form :model="pointsForm" label-width="80px">
        <el-form-item label="用户">
          <span>{{ selectedUser?.username }}</span>
        </el-form-item>
        <el-form-item label="当前积分">
          <span class="current-points">{{ selectedUser?.currentPoints || 0 }}</span>
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="pointsForm.amount" :min="-99999" :max="99999" />
          <span style="margin-left: 8px; color: #999;">正数增加，负数扣减</span>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="pointsForm.reason" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust" :loading="adjusting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, adjustUserPoints } from '../api'

const loading = ref(false)
const allUsers = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const selectedUser = ref(null)
const adjusting = ref(false)
const pointsForm = ref({
  amount: 0,
  reason: ''
})

// 搜索过滤后的用户
const filteredUsers = computed(() => {
  if (!searchKeyword.value) return allUsers.value
  const kw = searchKeyword.value.toLowerCase()
  return allUsers.value.filter(u => 
    (u.username && u.username.toLowerCase().includes(kw)) ||
    (u.phone && u.phone.includes(kw))
  )
})

// 当前页的用户
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredUsers.value.slice(start, end)
})

const getLevelType = (level) => {
  const map = { basic: 'info', silver: '', gold: 'warning', black: 'danger' }
  return map[level] || 'info'
}

const getLevelText = (level) => {
  const map = { basic: '普通会员', silver: '白银会员', gold: '黄金会员', black: '黑金会员' }
  return map[level] || '普通会员'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getUsers()
    allUsers.value = res.data || []
  } catch (e) {
    ElMessage.error('加载用户失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1 // 搜索时重置到第一页
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const openPointsDialog = (user) => {
  selectedUser.value = user
  pointsForm.value = { amount: 0, reason: '' }
  dialogVisible.value = true
}

const confirmAdjust = async () => {
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
    await adjustUserPoints(selectedUser.value.id, pointsForm.value.amount, pointsForm.value.reason)
    ElMessage.success('积分调整成功')
    // 更新本地数据
    selectedUser.value.currentPoints = (selectedUser.value.currentPoints || 0) + pointsForm.value.amount
    if (pointsForm.value.amount > 0) {
      selectedUser.value.totalPoints = (selectedUser.value.totalPoints || 0) + pointsForm.value.amount
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error('调整失败: ' + e.message)
  } finally {
    adjusting.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.users-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.points-value {
  color: #e6a23c;
  font-weight: 600;
}

.points-total {
  color: #909399;
}

.current-points {
  font-size: 18px;
  font-weight: 700;
  color: #e6a23c;
}
</style>
