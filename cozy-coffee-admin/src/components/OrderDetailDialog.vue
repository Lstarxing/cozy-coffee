<template>
  <el-dialog 
    v-model="visible" 
    title="订单详情" 
    width="800px" 
    class="detail-dialog"
    destroy-on-close
    @close="handleClose"
  >
    <div v-if="order" class="detail-content">
      <!-- 订单基本信息 -->
      <el-descriptions title="订单信息" :column="2" border class="mb-4 detail-descriptions">
        <el-descriptions-item label="订单号">
          <!-- 改：这里不要 items-center，避免换行后垂直居中 -->
          <div class="desc-value-row">
            <span class="break-all">{{ order.orderNo }}</span>
            <CopyText :text="order.orderNo" icon-only />
          </div>
        </el-descriptions-item>

        <el-descriptions-item label="状态">
          <StatusTag :status="order.status" type="order" />
        </el-descriptions-item>

        <el-descriptions-item label="下单时间">{{ formatDate(order.createdAt) }}</el-descriptions-item>

        <el-descriptions-item label="取餐码">
          <span v-if="order.pickupCode" class="pickup-code-active">{{ order.pickupCode }}</span>
          <span v-else class="text-secondary">待生成</span>
        </el-descriptions-item>

        <el-descriptions-item label="用餐方式">
          <el-tag size="small" type="info">{{ getDiningMethodText(order.diningMethod) }}</el-tag>
        </el-descriptions-item>

        <el-descriptions-item v-if="order.couponCode" label="优惠券">
          <el-tag size="small" type="warning">{{ order.couponCode }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 用户 & 门店 -->
      <el-descriptions title="用户与门店" :column="2" border class="mb-4 detail-descriptions">
        <el-descriptions-item label="用户">
          <div v-if="order.username" class="user-link" @click="goToUserDetail">
            <span class="break-all">{{ order.username }} ({{ order.phoneMasked }})</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="门店ID">{{ order.storeId || '—' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 商品列表 -->
      <div class="section-title">商品明细</div>
      <div class="order-items">
        <!-- 多商品模式：显示 items 数组 -->
        <template v-if="order.items && order.items.length > 0">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="item-main">
              <span class="item-name">{{ item.productName }}</span>
              <span class="item-qty">x{{ item.quantity }}</span>
            </div>
            <div v-if="getItemSpecsText(item)" class="item-specs text-secondary">
              {{ getItemSpecsText(item) }}
            </div>
            <div class="item-price">
              单价: ¥{{ item.unitPrice }} | 小计: <span class="total-price">¥{{ item.itemAmount }}</span>
            </div>
          </div>
        </template>
        <!-- 兼容旧数据：单商品模式 -->
        <template v-else>
          <div class="order-item">
            <div class="item-main">
              <span class="item-name">{{ order.productName || order.itemsSummary || '-' }}</span>
              <span v-if="order.quantity" class="item-qty">x{{ order.quantity }}</span>
            </div>
            <div class="item-specs text-secondary">
              {{ getSpecsText(order) }}
            </div>
            <div v-if="order.unitPrice" class="item-price">
              单价: ¥{{ order.unitPrice }} | 总计: <span class="total-price">¥{{ order.totalAmount }}</span>
            </div>
          </div>
        </template>
        <!-- 金额汇总 -->
        <div v-if="order.items && order.items.length > 0" class="order-summary">
          <div class="summary-row">
            <span>商品总计</span>
            <span>¥{{ order.totalAmount }}</span>
          </div>
          <div v-if="order.discountAmount > 0" class="summary-row">
            <span>优惠</span>
            <span class="discount">-¥{{ order.discountAmount }}</span>
          </div>
          <div class="summary-row total">
            <span>实付金额</span>
            <span class="pay-amount">¥{{ order.payAmount || order.totalAmount }}</span>
          </div>
        </div>
      </div>

      <el-divider border-style="dashed" />

      <!-- 备注 -->
      <div v-if="order.remark" class="remark-section">
        <span class="label">备注:</span>
        <span class="content">{{ order.remark }}</span>
      </div>
      <div v-else class="text-secondary text-sm">无备注</div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button :loading="refreshing" :icon="Refresh" @click="handleRefresh">刷新</el-button>
        <el-button @click="handleClose">关闭</el-button>

        <template v-if="order?.status === 'pending'">
          <el-divider direction="vertical" />
          <el-button type="primary" @click="handleAccept">接单</el-button>
        </template>

        <template v-if="order?.status === 'preparing'">
          <el-divider direction="vertical" />
          <el-button type="success" @click="handleComplete">完成</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'
import { getOrderDetail, acceptOrder, completeOrder } from '../api'
import StatusTag from './ui/StatusTag.vue'
import CopyText from './ui/CopyText.vue'
import dayjs from 'dayjs'

const router = useRouter()
const props = defineProps({
  modelValue: Boolean,
  orderId: Number
})

const emit = defineEmits(['update:modelValue', 'refresh'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const order = ref(null)
const refreshing = ref(false)

const loadOrderDetail = async () => {
  if (!props.orderId) return
  refreshing.value = true
  try {
    const res = await getOrderDetail(props.orderId)
    order.value = res.data
  } catch (e) {
    // 可选：提示错误
  } finally {
    refreshing.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val && props.orderId) loadOrderDetail()
})

const formatDate = (date) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '—')

const getSpecsText = (o) => {
  const specs = []
  const mapSize = { small: '小杯', medium: '中杯', large: '大杯' }
  const mapSugar = { none: '无糖', less: '少糖', normal: '正常糖', more: '多糖' }
  const mapTemp = { hot: '热饮', iced: '冰饮' }

  if (o.cupSize) specs.push(mapSize[o.cupSize] || o.cupSize)
  if (o.temperature) specs.push(mapTemp[o.temperature] || o.temperature)
  if (o.sugarLevel) specs.push(mapSugar[o.sugarLevel] || o.sugarLevel)
  if (o.coffeeStrength) specs.push(o.coffeeStrength === 'strong' ? '浓' : '正常')

  return specs.join(' / ') || '标准'
}

// 订单项规格文本（多商品模式）
// 订单项规格文本（多商品模式）
const getItemSpecsText = (item) => {
  if (!item) return ''
  
  // 烘焙/非定制商品的简单判断：如果没有杯型和温度，视为无需展示规格的商品
  if (!item.cupSize && !item.temperature && !item.optionsJson) {
      return '' 
  }

  const specs = []
  
  // 翻译映射 (与后端 Enum 对应)
  const mapSize = { 'STANDARD': '标准杯', 'LARGE': '大杯' }
  const mapTemp = { 'HOT': '热饮', 'COLD': '冰饮', 'WARM': '温饮' }
  const mapSugar = { 'NONE': '无糖', 'LIGHT': '微甜', 'STANDARD': '标准甜', 'MEDIUM': '少甜' }
  const mapStrength = { 'NORMAL': '标准浓度', 'STRONG': '加浓' }

  if (item.cupSize) specs.push(mapSize[item.cupSize] || item.cupSize)
  if (item.temperature) specs.push(mapTemp[item.temperature] || item.temperature)
  
  // 糖度处理
  if (item.sugarLevel) {
      if (item.sugarLevel === 'STANDARD') specs.push('标准甜')
      else specs.push(mapSugar[item.sugarLevel] || item.sugarLevel)
  }

  if (item.coffeeStrength) specs.push(mapStrength[item.coffeeStrength] || item.coffeeStrength)
  
  // 换基底 (解析 optionsJson)
  if (item.optionsJson) {
      try {
          const opts = JSON.parse(item.optionsJson)
          if (opts.milkType) {
              const mapMilk = { 'WHOLE': '', 'OAT': '换燕麦奶', 'COCONUT': '换椰奶' }
              const milkText = mapMilk[opts.milkType]
              if (milkText) specs.push(milkText)
          }
      } catch (e) {
          // ignore parse error
      }
  }

  return specs.join(' / ')
}

const getDiningMethodText = (method) => {
  if (!method) return '-'
  const map = { 'DINE_IN': '堂食', 'TAKEOUT': '自提', 'DELIVERY': '外卖' }
  return map[method] || method
}

const goToUserDetail = () => {
  if (order.value?.userId) {
    router.push(`/users/${order.value.userId}`)
    visible.value = false
  }
}

const handleRefresh = loadOrderDetail
const handleClose = () => { visible.value = false }

const handleAccept = async () => {
  try {
    const res = await acceptOrder(order.value.id)
    ElMessage.success(`已接单，取餐码: ${res.data.pickupCode}`)
    emit('refresh')
    loadOrderDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const handleComplete = async () => {
  try {
    await completeOrder(order.value.id)
    ElMessage.success('订单已完成')
    emit('refresh')
    loadOrderDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}
</script>

<style scoped lang="scss">
.detail-dialog {
  :deep(.el-dialog__body) {
    padding-top: 10px;
  }
}

/* 关键：只在详情弹窗范围内把 descriptions 的单元格改为顶对齐 */
.detail-descriptions {
  :deep(.el-descriptions__body th),
  :deep(.el-descriptions__body td) {
    vertical-align: top;
  }

  /* label cell 和 content cell 上下 padding 轻微对齐 */
  :deep(.el-descriptions__label) {
    padding-top: 10px;
  }
  :deep(.el-descriptions__content) {
    padding-top: 10px;
  }
}

.desc-value-row {
  display: inline-flex;
  align-items: flex-start; /* 关键：换行时 copy 按钮也贴顶 */
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.break-all {
  word-break: break-all;
  overflow-wrap: anywhere;
  min-width: 0;
}

.mb-4 { margin-bottom: 16px; }
.text-secondary { color: #9CA3AF; }

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 12px;
  margin-top: 24px;
}

.pickup-code-active {
  font-family: monospace;
  font-size: 16px;
  font-weight: 700;
  color: #B45309;
}

.user-link {
  color: #8B5E3C;
  cursor: pointer;
  display: inline-flex;
  align-items: flex-start; /* 关键：长内容换行时顶对齐 */
  gap: 6px;

  .arrow {
    margin-top: 2px; /* 微调箭头位置，让它与首行对齐 */
  }

  &:hover {
    text-decoration: underline;
  }
}

.order-items {
  background-color: #F9FAFB;
  border-radius: 6px;
  padding: 12px;
}

.order-item {
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px dashed #E5E7EB;
  
  &:last-of-type {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }

  .item-main {
    display: flex;
    justify-content: space-between;
    font-weight: 500;
    margin-bottom: 4px;
  }

  .item-specs {
    font-size: 12px;
    margin-bottom: 8px;
  }

  .item-price {
    text-align: right;
    font-size: 13px;
    color: #4B5563;

    .total-price {
      color: #111827;
      font-weight: 600;
      margin-left: 4px;
    }
  }
}

.order-summary {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #E5E7EB;

  .summary-row {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: #4B5563;
    margin-bottom: 6px;

    .discount { color: #10B981; }
    
    &.total {
      font-weight: 600;
      font-size: 14px;
      color: #111827;
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px dashed #E5E7EB;

      .pay-amount {
        color: #B45309;
        font-size: 16px;
      }
    }
  }
}

.remark-section {
  font-size: 14px;
  .label { color: #6B7280; margin-right: 8px; }
  .content { color: #111827; }
}
</style>