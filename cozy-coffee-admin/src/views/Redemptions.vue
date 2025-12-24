<template>
  <div class="redemptions-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>兑换订单管理</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="pending">待处理</el-radio-button>
            <el-radio-button label="processing">备货中</el-radio-button>
            <el-radio-button label="shipped">已发货</el-radio-button>
            <el-radio-button label="completed">已完成</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="paginatedRedemptions" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="productName" label="商品" />
        <el-table-column prop="pointsCost" label="消费积分" width="100" />
        <el-table-column prop="deliveryType" label="配送方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.deliveryType === 'express' ? 'primary' : 'success'" size="small">
              {{ row.deliveryType === 'express' ? '快递' : '自提' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button text type="info" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'pending'" text type="primary" @click="handleProcess(row)">备货</el-button>
            <el-button v-if="row.status === 'processing' && row.deliveryType === 'express'" 
              text type="warning" @click="showShipDialog(row)">发货</el-button>
            <el-button v-if="row.status === 'processing' && row.deliveryType === 'pickup'" 
              text type="success" @click="handleComplete(row)">核销</el-button>
            <el-button v-if="row.status === 'shipped'" text type="success" @click="handleComplete(row)">确认完成</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredRedemptions.length"
        layout="total, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 发货对话框 -->
    <el-dialog v-model="shipDialogVisible" title="填写物流信息" width="400px">
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="快递公司">
          <el-select v-model="shipForm.company" placeholder="选择快递公司">
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
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="600px">
      <div class="order-detail" v-if="selectedOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ selectedOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(selectedOrder.status)">{{ getStatusText(selectedOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ selectedOrder.productName }}</el-descriptions-item>
          <el-descriptions-item label="商品图片">
            <el-image v-if="selectedOrder.productImage" 
              :src="selectedOrder.productImage" 
              style="width: 60px; height: 60px; border-radius: 4px;" 
              fit="cover" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="消费积分">{{ selectedOrder.pointsCost }} 积分</el-descriptions-item>
          <el-descriptions-item label="兑换数量">{{ selectedOrder.quantity || 1 }}</el-descriptions-item>
          <el-descriptions-item label="配送方式">
            <el-tag :type="selectedOrder.deliveryType === 'express' ? 'primary' : 'success'" size="small">
              {{ selectedOrder.deliveryType === 'express' ? '快递配送' : '门店自提' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatDate(selectedOrder.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider v-if="selectedOrder.deliveryType === 'express'" />
        
        <el-descriptions v-if="selectedOrder.deliveryType === 'express'" title="收货信息" :column="1" border>
          <el-descriptions-item label="收货人">{{ selectedOrder.receiverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ selectedOrder.receiverPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ selectedOrder.receiverAddress || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider v-if="selectedOrder.deliveryType === 'pickup'" />
        
        <el-descriptions v-if="selectedOrder.deliveryType === 'pickup'" title="自提信息" :column="1" border>
          <el-descriptions-item label="取餐码">
            <el-tag v-if="selectedOrder.pickupCode" type="warning" size="large" style="font-size: 18px;">
              {{ selectedOrder.pickupCode }}
            </el-tag>
            <span v-else>待生成</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider v-if="selectedOrder.shippingCompany" />
        
        <el-descriptions v-if="selectedOrder.shippingCompany" title="物流信息" :column="1" border>
          <el-descriptions-item label="快递公司">{{ selectedOrder.shippingCompany }}</el-descriptions-item>
          <el-descriptions-item label="快递单号">{{ selectedOrder.trackingNumber }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ formatDate(selectedOrder.shippedAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRedemptions, processRedemption, shipRedemption, completeRedemption } from '../api'

const loading = ref(false)
const allRedemptions = ref([])
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const shipDialogVisible = ref(false)
const shipForm = ref({ company: '', trackingNo: '' })
const shippingOrder = ref(null)

const detailDialogVisible = ref(false)
const selectedOrder = ref(null)

// 过滤后的兑换订单
const filteredRedemptions = computed(() => {
  let orders = allRedemptions.value
  if (statusFilter.value) {
    orders = orders.filter(o => o.status === statusFilter.value)
  }
  return orders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

// 分页数据
const paginatedRedemptions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRedemptions.value.slice(start, start + pageSize.value)
})

const getStatusType = (status) => {
  const map = { pending: 'warning', processing: 'primary', shipped: 'info', completed: 'success', cancelled: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待处理', processing: '备货中', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const loadRedemptions = async () => {
  loading.value = true
  try {
    const res = await getRedemptions()
    allRedemptions.value = res.data || []
  } catch (e) {
    // 暂时使用模拟数据
    allRedemptions.value = []
    console.error('加载兑换订单失败:', e)
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
}

const viewDetail = (row) => {
  selectedOrder.value = row
  detailDialogVisible.value = true
}

const handleProcess = async (row) => {
  try {
    await processRedemption(row.id)
    row.status = 'processing'
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
    shippingOrder.value.status = 'shipped'
    shippingOrder.value.shippingCompany = shipForm.value.company
    shippingOrder.value.trackingNumber = shipForm.value.trackingNo
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
    row.status = 'completed'
    ElMessage.success('订单已完成')
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

onMounted(() => {
  loadRedemptions()
})
</script>

<style scoped>
.redemptions-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-detail {
  padding: 10px 0;
}
</style>
