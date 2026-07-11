<!-- 积分明细弹窗 -->
<template>
  <div v-if="visible" class="points-detail-modal" @click.self="$emit('update:visible', false)">
    <div class="modal-content">
      <div class="modal-header">
        <h3><List :size="20" style="margin-right: 8px; vertical-align: text-bottom;" />积分明细</h3>
        <button class="close-btn" @click="$emit('update:visible', false)">&times;</button>
      </div>
      <div class="balance-summary">
        <div class="balance-item">
          <span class="label">当前积分</span>
          <span class="value">{{ userInfo?.currentPoints || 0 }}</span>
        </div>
        <div class="balance-item">
          <span class="label">累计获得</span>
          <span class="value">{{ userInfo?.totalPoints || 0 }}</span>
        </div>
      </div>
      <div v-if="!isLoading" class="transactions-list">
        <div v-if="transactions.length === 0" class="empty-state">暂无积分记录</div>
        <div v-for="item in transactions" v-else :key="item.id" class="transaction-item">
          <div class="transaction-left">
            <span class="transaction-type" :class="item.changeAmount > 0 ? 'income' : 'expense'">
              {{ getSourceTypeName(item.sourceType) }}
            </span>
            <span class="transaction-desc">{{ item.description }}</span>
            <span class="transaction-time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <div class="transaction-right">
            <span class="transaction-amount" :class="item.changeAmount > 0 ? 'income' : 'expense'">
              {{ item.changeAmount > 0 ? '+' : '' }}{{ item.changeAmount }}
            </span>
            <span class="transaction-balance">余额：{{ item.balanceAfter }}</span>
          </div>
        </div>
      </div>
      <div v-else class="loading-state">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { List } from 'lucide-vue-next'
import { getPointsTransactions } from '@/api/member'

const props = defineProps({
  visible: Boolean,
  userInfo: Object
})

defineEmits(['update:visible'])

const transactions = ref([])
const isLoading = ref(false)

function getSourceTypeName(type) {
  const map = { signin: '每日签到', register: '新用户注册', profile: '完善资料', consume: '消费赚积分', redeem: '积分兑换', cancel: '订单取消退还', invite: '邀请好友', invited: '受邀奖励' }
  return map[type] || type
}

function formatDateTime(dateStr) {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

watch(() => props.visible, (val) => {
  if (val) load()
})

async function load() {
  isLoading.value = true
  transactions.value = []
  try {
    const data = await getPointsTransactions({ limit: 50 })
    transactions.value = data.data || []
  } catch (error) {
    ElMessage.error(error.message || '系统错误')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.points-detail-modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal-content { background: white; border-radius: 16px; width: 90%; max-width: 500px; max-height: 80vh; overflow-y: auto; padding: 24px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.modal-header h3 { margin: 0; font-size: 18px; display: flex; align-items: center; }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: #888; }
.balance-summary { display: flex; gap: 16px; margin-bottom: 20px; }
.balance-item { flex: 1; background: #F9F9F9; padding: 16px; border-radius: 12px; text-align: center; }
.balance-item .label { font-size: 12px; color: #888; display: block; margin-bottom: 4px; }
.balance-item .value { font-size: 22px; font-weight: 700; color: #3E2723; }
.transactions-list { max-height: 400px; overflow-y: auto; }
.transaction-item { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #F5F5F5; }
.transaction-left { display: flex; flex-direction: column; }
.transaction-type { font-weight: 600; font-size: 14px; }
.transaction-type.income { color: #4CAF50; }
.transaction-type.expense { color: #F44336; }
.transaction-desc { font-size: 12px; color: #888; margin: 2px 0; }
.transaction-time { font-size: 11px; color: #aaa; }
.transaction-right { display: flex; flex-direction: column; align-items: flex-end; }
.transaction-amount { font-weight: 700; font-size: 16px; }
.transaction-amount.income { color: #4CAF50; }
.transaction-amount.expense { color: #F44336; }
.transaction-balance { font-size: 11px; color: #aaa; }
.empty-state, .loading-state { text-align: center; padding: 40px; color: #888; }
</style>
