<template>
  <el-dialog
    v-model="visible"
    title="兑换详情"
    width="720px"
    top="8vh"
    :align-center="false"
    destroy-on-close
    class="detail-dialog redemption-detail-dialog"
    @close="handleClose"
  >
    <div v-if="order" class="detail-content">
      <!-- 订单信息 -->
      <el-descriptions title="基本信息" :column="2" border class="mb-4 detail-descriptions">
        <el-descriptions-item label="订单号">
          <div class="desc-value-row">
            <span class="break-all">{{ order.orderNo }}</span>
            <CopyText :text="order.orderNo" icon-only />
          </div>
        </el-descriptions-item>

        <el-descriptions-item label="状态">
          <StatusTag :status="order.status" type="redemption" />
        </el-descriptions-item>

        <el-descriptions-item label="下单时间">{{ formatDate(order.createdAt) }}</el-descriptions-item>

        <el-descriptions-item label="交付方式">
          <el-tag :type="getFulfillmentTypeTag(order.fulfillmentType)" size="small">
            {{ getFulfillmentTypeText(order.fulfillmentType) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- User -->
      <el-descriptions title="用户信息" :column="2" border class="mb-4 detail-descriptions">
        <el-descriptions-item label="用户">
          <div v-if="order.username" class="user-link" @click="goToUserDetail">
            <span class="break-all">{{ order.username }} ({{ order.phoneMasked || '无手机号' }})</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ order.userId }}</el-descriptions-item>
      </el-descriptions>

      <!-- Goods -->
      <div class="section-title">商品明细</div>
      <div class="product-card mb-4 no-image">
        <div class="p-info">
          <div class="p-name">{{ order.productName }}</div>
          <div class="p-price text-secondary">
            消耗积分: <span class="points-val">{{ order.pointsCost }}</span> × {{ order.quantity }}
          </div>
        </div>
        <div class="p-total">
          总计积分: <span class="points-val-lg">{{ (order.pointsCost || 0) * (order.quantity || 1) }}</span>
        </div>
      </div>

      <el-divider border-style="dashed" />

      <!-- 交付详情 -->
      <template v-if="order.fulfillmentType === 'VIRTUAL'">
        <el-descriptions title="虚拟发放" :column="1" border class="detail-descriptions">
          <el-descriptions-item label="兑换码">
            <span class="virtual-code">{{ order.virtualCode || '待发放' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="发放时间" v-if="order.issuedAt">{{ formatDate(order.issuedAt) }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template v-else-if="order.fulfillmentType === 'PICKUP'">
        <el-descriptions title="自提信息" :column="2" border class="detail-descriptions">
          <el-descriptions-item label="门店">{{ order.storeName || order.storeId || '总店' }}</el-descriptions-item>
          <el-descriptions-item label="自提码">
            <div class="desc-value-row">
              <span class="pickup-code">{{ order.pickupCode || '—' }}</span>
              <CopyText v-if="order.pickupCode" :text="order.pickupCode" icon-only />
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template v-else-if="order.fulfillmentType === 'DELIVERY'">
        <el-descriptions title="收货信息" :column="2" border class="mb-4 detail-descriptions">
          <el-descriptions-item label="收件人">{{ order.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ order.receiverAddress }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions
          title="物流状态"
          :column="2"
          border
          class="detail-descriptions"
          v-if="order.status === 'shipped' || order.status === 'completed'"
        >
          <el-descriptions-item label="快递公司">{{ order.shippingCompany }}</el-descriptions-item>
          <el-descriptions-item label="运单号">
            <div class="desc-value-row">
              <span class="break-all">{{ order.trackingNumber }}</span>
              <CopyText v-if="order.trackingNumber" :text="order.trackingNumber" icon-only />
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ formatDate(order.shippedAt) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleRefresh" :loading="loading" :icon="Refresh">刷新</el-button>
        <el-button @click="handleClose">关闭</el-button>

        <template v-if="order?.status === 'pending'">
          <el-button type="primary" @click="handleProcess">
            {{ order.fulfillmentType === 'VIRTUAL' ? '发放' : '开始备货' }}
          </el-button>
        </template>

        <template v-if="order?.status === 'processing'">
          <el-button v-if="order.fulfillmentType === 'DELIVERY'" type="warning" @click="$emit('ship', order)">发货</el-button>
          <el-button v-if="order.fulfillmentType === 'PICKUP'" type="success" @click="handleComplete">核销</el-button>
        </template>

        <template v-if="order?.status === 'shipped'">
          <el-button type="success" @click="handleComplete">确认完成</el-button>
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
import { getRedemptionDetail, processRedemption, completeRedemption } from '../api'
import StatusTag from './ui/StatusTag.vue'
import CopyText from './ui/CopyText.vue'
import dayjs from 'dayjs'

const router = useRouter()
const props = defineProps({
  modelValue: Boolean,
  orderId: Number
})

const emit = defineEmits(['update:modelValue', 'refresh', 'ship'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const order = ref(null)
const loading = ref(false)

const loadOrderDetail = async () => {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await getRedemptionDetail(props.orderId)
    order.value = res.data
  } catch (e) {
    // 可选：提示错误
  } finally {
    loading.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val && props.orderId) loadOrderDetail()
})

const formatDate = (date) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '—')

const getFulfillmentTypeTag = (type) => ({ VIRTUAL: 'success', PICKUP: 'info', DELIVERY: 'warning' }[type] || 'info')
const getFulfillmentTypeText = (type) => ({ VIRTUAL: '虚拟发放', PICKUP: '到店自提', DELIVERY: '快递配送' }[type] || type)

const goToUserDetail = () => {
  if (order.value?.userId) {
    router.push(`/users/${order.value.userId}`)
    visible.value = false
  }
}

const handleRefresh = loadOrderDetail
const handleClose = () => { visible.value = false }

const handleProcess = async () => {
  try {
    if (order.value.fulfillmentType === 'VIRTUAL') {
      await completeRedemption(order.value.id)
      ElMessage.success('已发放')
    } else {
      await processRedemption(order.value.id)
      ElMessage.success('已开始备货')
    }
    emit('refresh')
    loadOrderDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const handleComplete = async () => {
  try {
    await completeRedemption(order.value.id)
    ElMessage.success('订单已完成')
    emit('refresh')
    loadOrderDetail()
  } catch (e) {
    ElMessage.error(e.message)
  }
}
</script>

<style scoped lang="scss">
/*
  关键 UI 改进点：
  1) 宽度从 800 -> 720，更精致；
  2) 不再 align-center（居中会“占满视线”），改 top=8vh 让上方留白更像企业后台；
  3) 限制 body 最大高度并滚动，避免弹窗“撑满中间屏幕”；
  4) header/body/footer 的 padding 更克制一致。
*/
.redemption-detail-dialog {
  :deep(.el-dialog) {
    border-radius: 10px;
  }

  :deep(.el-dialog__header) {
    padding: 14px 16px 10px 16px;
    margin-right: 0;
  }

  :deep(.el-dialog__body) {
    padding: 0; /* 交给 detail-content 控制 */
    max-height: 68vh;
    overflow: auto;
  }

  :deep(.el-dialog__footer) {
    padding: 10px 16px 14px 16px;
  }
}

/* 顶对齐（避免换行时左右不齐） */
.detail-descriptions {
  :deep(.el-descriptions__body th),
  :deep(.el-descriptions__body td) {
    vertical-align: top;
  }
  :deep(.el-descriptions__label),
  :deep(.el-descriptions__content) {
    padding-top: 10px;
  }
}

.detail-content {
  padding: 12px 16px 16px 16px;
}

.mb-4 { margin-bottom: 16px; }
.text-secondary { color: #9ca3af; }

.desc-value-row {
  display: inline-flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.break-all {
  word-break: break-all;
  overflow-wrap: anywhere;
  min-width: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
  margin-top: 16px; /* 比原来更克制，减少“占满”感 */
}

.user-link {
  color: #8b5e3c;
  cursor: pointer;
  display: inline-flex;
  align-items: flex-start;
  gap: 6px;

  &:hover { text-decoration: underline; }

  .arrow { margin-top: 2px; }
}

.product-card {
  display: flex;
  align-items: center;
  background: #f9fafb;
  padding: 12px;
  border-radius: 6px;
  gap: 16px;

  &.no-image { padding: 14px 16px; }

  .p-info {
    flex: 1;
    .p-name { font-weight: 600; color: #111827; font-size: 15px; }
    .p-price { font-size: 13px; margin-top: 6px; }
  }

  .p-total { font-weight: 600; color: #b45309; }
}

.points-val { color: #b45309; font-weight: 600; }
.points-val-lg { color: #b45309; font-weight: 700; font-size: 16px; }

.virtual-code {
  font-family: monospace;
  font-size: 18px;
  font-weight: 700;
  color: #059669;
}

.pickup-code {
  font-family: monospace;
  font-size: 16px;
  font-weight: 700;
  color: #b45309;
}
</style>