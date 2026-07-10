<template>
  <div class="recent-activity">
     <el-tabs v-model="activeTab" class="dashboard-tabs">
        <el-tab-pane label="最近咖啡订单" name="coffee">
           <el-table v-loading="loading" :data="orders" size="small" style="width: 100%">
              <el-table-column label="订单号" width="180">
                 <template #default="{ row }">
                   <CopyText :text="row.orderNo" />
                 </template>
              </el-table-column>
              <el-table-column label="用户" min-width="120">
                 <template #default="{ row }">
                    <span v-if="row.nickname || row.username">{{ row.nickname || row.username }}</span>
                    <span v-else class="text-secondary">-</span>
                 </template>
              </el-table-column>
              <el-table-column label="金额" width="100" align="right">
                 <template #default="{ row }">
                    <span class="font-mono">¥{{ row.totalAmount }}</span>
                 </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                 <template #default="{ row }">
                    <StatusTag :status="row.status" type="order" />
                 </template>
              </el-table-column>
              <el-table-column label="时间" width="140" align="right">
                 <template #default="{ row }">
                    <span class="text-secondary text-xs">{{ formatDate(row.createdAt) }}</span>
                 </template>
              </el-table-column>
              <el-table-column width="80" align="center">
                 <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="$router.push('/orders')">查看</el-button>
                 </template>
              </el-table-column>
           </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="最近兑换" name="redemption">
            <el-table v-loading="loading" :data="redemptions" size="small" style="width: 100%">
              <el-table-column label="订单号" width="180">
                 <template #default="{ row }">
                   <CopyText :text="row.orderNo" />
                 </template>
              </el-table-column>
              <el-table-column label="商品" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.productName }}</template>
              </el-table-column>
              <el-table-column label="消耗" width="100" align="right">
                 <template #default="{ row }">
                    <span class="font-mono text-amber">{{ row.pointsCost }}</span>
                 </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                 <template #default="{ row }">
                    <StatusTag :status="row.status" type="redemption" />
                 </template>
              </el-table-column>
              <el-table-column label="时间" width="140" align="right">
                 <template #default="{ row }">
                    <span class="text-secondary text-xs">{{ formatDate(row.createdAt) }}</span>
                 </template>
              </el-table-column>
              <el-table-column width="80" align="center">
                 <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="$router.push('/redemptions')">查看</el-button>
                 </template>
              </el-table-column>
           </el-table>
        </el-tab-pane>
     </el-tabs>
     
     <div class="footer-link">
        <el-button link @click="viewAll">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
     </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getRecentOrders, getRecentRedemptions } from '../../api'
import StatusTag from '../ui/StatusTag.vue'
import CopyText from '../ui/CopyText.vue'
import { ArrowRight } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const activeTab = ref('coffee')
const loading = ref(false)
const orders = ref([])
const redemptions = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const [oRes, rRes] = await Promise.all([
      getRecentOrders(6),
      getRecentRedemptions(6)
    ])
    orders.value = oRes.data || []
    redemptions.value = rRes.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const formatDate = (d) => d ? dayjs(d).format('MM-DD HH:mm') : '-'

const viewAll = () => {
  if (activeTab.value === 'coffee') router.push('/orders')
  else router.push('/redemptions')
}

defineExpose({ loadData })

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.recent-activity {
  position: relative;
}

.dashboard-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }
  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background-color: #F3F4F6;
  }
}

.footer-link {
  position: absolute;
  top: 0;
  right: 0;
  height: 40px; 
  display: flex;
  align-items: center;
}

.text-secondary { color: #9CA3AF; }
.font-mono { font-family: monospace; font-weight: 500; }
.text-amber { color: #B45309; }
.text-xs { font-size: 12px; }
</style>
