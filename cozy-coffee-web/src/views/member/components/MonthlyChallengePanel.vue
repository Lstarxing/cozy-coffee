<!-- 本月挑战面板 + 黑卡加速包 -->
<template>
  <div class="task-center-section">
    <div class="section-title">
      <h4>本月挑战</h4>
      <span class="subtitle">Monthly Challenge</span>
      <button
        class="refresh-btn"
        :disabled="isRefreshingTask"
        title="刷新任务进度"
        @click="handleRefreshMonthlyTask"
      >
        <RefreshCw :size="16" :class="{ 'spinning': isRefreshingTask }" />
      </button>
    </div>

    <div v-if="userLevel === 'black'" class="black-accelerate-box">
      <div class="box-header">
        <div class="box-title">
          <span class="premium-icon">✨</span>
          <span class="title-text">黑卡加速包</span>
        </div>
        <div class="box-status">
          剩 <strong>¥{{ userInfo?.monthlyAccelerateRemaining ?? 0 }}</strong>
        </div>
      </div>
      <div class="accelerate-progress">
        <div class="progress-bar-bg">
          <div class="progress-fill" :style="{ width: accelerateProgressPercent + '%' }"></div>
        </div>
        <div class="progress-info">
          <span>已加速 ¥{{ (300 - (userInfo?.monthlyAccelerateRemaining ?? 0)).toFixed(2) }}</span>
        </div>
      </div>
    </div>

    <div :key="taskRefreshKey" class="task-list-premium">
      <div class="task-item-row" :class="{ 'task-completed': isOrderTaskCompleted }">
        <div class="task-icon-bg"><CalendarCheck :size="20" /></div>
        <div class="task-main">
          <div class="task-top">
            <span class="name">打卡达人</span>
            <span v-if="!isOrderTaskCompleted" class="reward">+40 积分</span>
            <span v-else class="reward claimed">✓ 已领取</span>
          </div>
          <div class="task-desc">本月完成订单 4 次</div>
          <div class="task-progress-bar">
            <div class="fill orange" :style="{ width: Math.min(100, ((taskData.monthlyOrderCount ?? userInfo?.monthlyOrderCount ?? 0) / 4) * 100) + '%' }"></div>
          </div>
        </div>
        <div class="task-action">
          <span class="status-text" :class="{ 'completed': isOrderTaskCompleted }">{{ taskData.monthlyOrderCount ?? userInfo?.monthlyOrderCount ?? 0 }}/4</span>
        </div>
      </div>

      <div class="task-item-row" :class="{ 'task-completed': isMorningTaskCompleted }">
        <div class="task-icon-bg"><Sun :size="20" /></div>
        <div class="task-main">
          <div class="task-top">
            <span class="name">晨间唤醒</span>
            <span v-if="!isMorningTaskCompleted" class="reward">+60 积分</span>
            <span v-else class="reward claimed">✓ 已领取</span>
          </div>
          <div class="task-desc">10:00前完成订单 3 次</div>
          <div class="task-progress-bar">
            <div class="fill yellow" :style="{ width: Math.min(100, ((taskData.morningOrderCount ?? userInfo?.morningOrderCount ?? 0) / 3) * 100) + '%' }"></div>
          </div>
        </div>
        <div class="task-action">
          <span class="status-text" :class="{ 'completed': isMorningTaskCompleted }">{{ taskData.morningOrderCount ?? userInfo?.morningOrderCount ?? 0 }}/3</span>
        </div>
      </div>

      <div class="task-item-row" :class="{ 'task-completed': isDeliveryTaskCompleted }">
        <div class="task-icon-bg"><Truck :size="20" /></div>
        <div class="task-main">
          <div class="task-top">
            <span class="name">外卖尝鲜</span>
            <span v-if="!isDeliveryTaskCompleted" class="reward">+50 积分</span>
            <span v-else class="reward claimed">✓ 已领取</span>
          </div>
          <div class="task-desc">完成 2 笔外卖订单</div>
          <div class="task-progress-bar">
            <div class="fill green" :style="{ width: Math.min(100, ((taskData.currentDeliveryOrders ?? userInfo?.monthlyDeliveryOrders ?? 0) / 2) * 100) + '%' }"></div>
          </div>
        </div>
        <div class="task-action">
          <span class="status-text" :class="{ 'completed': isDeliveryTaskCompleted }">{{ taskData.currentDeliveryOrders ?? userInfo?.monthlyDeliveryOrders ?? 0 }}/2</span>
        </div>
      </div>

      <div class="task-item-row" :class="{ 'task-completed': isNewProductTaskCompleted }">
        <div class="task-icon-bg"><ShoppingBag :size="20" /></div>
        <div class="task-main">
          <div class="task-top">
            <span class="name">新品猎人</span>
            <span v-if="!isNewProductTaskCompleted" class="reward">+80 积分</span>
            <span v-else class="reward claimed">✓ 已领取</span>
          </div>
          <div class="task-desc">尝试 3 款限定新品</div>
          <div class="task-progress-bar">
            <div class="fill purple" :style="{ width: Math.min(100, ((taskData.newProductCount ?? userInfo?.newProductCount ?? 0) / 3) * 100) + '%' }"></div>
          </div>
        </div>
        <div class="task-action">
          <span class="status-text" :class="{ 'completed': isNewProductTaskCompleted }">{{ taskData.newProductCount ?? userInfo?.newProductCount ?? 0 }}/3</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { CalendarCheck, Sun, Truck, ShoppingBag, RefreshCw } from 'lucide-vue-next'
import { getMonthlyTask } from '@/api/member'

const props = defineProps({
  userInfo: Object,
  userLevel: String
})

const userStore = useUserStore()

const taskData = ref({
  currentSpent: 0, currentDeliveryOrders: 0,
  monthlyOrderCount: 0, morningOrderCount: 0, newProductCount: 0,
  challengeOrderClaimed: false, challengeMorningClaimed: false,
  challengeDeliveryClaimed: false, challengeNewproductClaimed: false
})

const isOrderTaskCompleted = computed(() => {
  const claimed = taskData.value.challengeOrderClaimed ?? props.userInfo?.challengeOrderClaimed ?? false
  if (claimed) return true
  return (taskData.value.monthlyOrderCount ?? props.userInfo?.monthlyOrderCount ?? 0) >= 4
})
const isMorningTaskCompleted = computed(() => {
  const claimed = taskData.value.challengeMorningClaimed ?? props.userInfo?.challengeMorningClaimed ?? false
  if (claimed) return true
  return (taskData.value.morningOrderCount ?? props.userInfo?.morningOrderCount ?? 0) >= 3
})
const isDeliveryTaskCompleted = computed(() => {
  const claimed = taskData.value.challengeDeliveryClaimed ?? props.userInfo?.challengeDeliveryClaimed ?? false
  if (claimed) return true
  return (taskData.value.currentDeliveryOrders ?? props.userInfo?.monthlyDeliveryOrders ?? 0) >= 2
})
const isNewProductTaskCompleted = computed(() => {
  const claimed = taskData.value.challengeNewproductClaimed ?? props.userInfo?.challengeNewproductClaimed ?? false
  if (claimed) return true
  return (taskData.value.newProductCount ?? props.userInfo?.newProductCount ?? 0) >= 3
})

const accelerateProgressPercent = computed(() => {
  const remaining = parseFloat(props.userInfo?.monthlyAccelerateRemaining ?? 300)
  const used = Math.max(0, 300 - remaining)
  return Math.min(100, (used / 300) * 100)
})

const isRefreshingTask = ref(false)
const taskRefreshKey = ref(0)

let pollingTimer = null

async function loadMonthlyTaskData(retryCount = 0) {
  try {
    if (!userStore.token) return
    const data = await getMonthlyTask()
    const d = data.data
    const target = taskData.value
    target.currentSpent = d.currentSpent ?? target.currentSpent ?? 0
    target.currentDeliveryOrders = d.currentDeliveryOrders ?? d.deliveryOrderCount ?? 0
    target.monthlyOrderCount = d.monthlyOrderCount ?? 0
    target.morningOrderCount = d.morningOrderCount ?? 0
    target.newProductCount = d.newProductCount ?? 0
    target.challengeOrderClaimed = d.challengeOrderClaimed ?? false
    target.challengeMorningClaimed = d.challengeMorningClaimed ?? false
    target.challengeDeliveryClaimed = d.challengeDeliveryClaimed ?? false
    target.challengeNewproductClaimed = d.challengeNewproductClaimed ?? false
    target.taskMonth = d.taskMonth
    target.userId = d.userId
    taskRefreshKey.value++

    if (!retryCount) retryCount = 0
    const needRetry = (
      (d.monthlyOrderCount >= 4 && !d.challengeOrderClaimed) ||
      (d.morningOrderCount >= 3 && !d.challengeMorningClaimed) ||
      ((d.currentDeliveryOrders || d.monthlyDeliveryOrders) >= 2 && !d.challengeDeliveryClaimed) ||
      (d.newProductCount >= 3 && !d.challengeNewproductClaimed)
    )
    if (needRetry && retryCount < 2) {
      setTimeout(() => loadMonthlyTaskData(retryCount + 1), 500)
    }
  } catch (error) {
    console.error('加载月度任务数据失败:', error)
  }
}

async function handleRefreshMonthlyTask() {
  if (isRefreshingTask.value) return
  isRefreshingTask.value = true
  try {
    await loadMonthlyTaskData()
    taskRefreshKey.value++
    ElMessage.success({ message: '任务进度已更新', duration: 1500 })
  } catch (e) { ElMessage.error('刷新失败') }
  finally { isRefreshingTask.value = false }
}

function startPolling() {
  stopPolling()
  pollingTimer = setInterval(() => loadMonthlyTaskData(), 30000)
}

function stopPolling() {
  if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null }
}

onMounted(() => {
  loadMonthlyTaskData()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})

defineExpose({ refreshTaskData: loadMonthlyTaskData })
</script>

<style scoped>
.task-center-section { background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); height: 100%; display: flex; flex-direction: column; }
.section-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-title h4 { font-size: 18px; font-weight: 700; color: #1f2937; letter-spacing: -0.5px; margin: 0; }
.section-title .subtitle { font-size: 13px; color: #9ca3af; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
.refresh-btn { background: none; border: none; cursor: pointer; color: #8D6E63; padding: 4px; border-radius: 50%; }
.refresh-btn:hover { background: #F5F0E6; }
.refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.black-accelerate-box { background: linear-gradient(135deg, #1a1a1a 0%, #2c2c2c 100%); border-radius: 16px; padding: 20px; color: #F7E7CE; margin-bottom: 24px; box-shadow: 0 8px 24px rgba(0,0,0,0.4); }
.black-accelerate-box .box-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.black-accelerate-box .box-title { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.black-accelerate-box .box-status strong { font-size: 18px; color: #fff; }
.title-text { color: #F7E7CE; font-weight: 700; }
.accelerate-progress { width: 100%; }
.progress-bar-bg { width: 100%; height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; margin-bottom: 8px; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #FDB931, #FFD700); border-radius: 3px; transition: width 0.5s ease; }
.progress-info { font-size: 11px; color: rgba(247, 231, 206, 0.6); display: flex; justify-content: flex-end; }

.task-list-premium { display: flex; flex-direction: column; gap: 12px; flex: 1; }
.task-item-row { display: flex; align-items: center; gap: 12px; padding: 12px; background: #FAFAFA; border-radius: 12px; transition: all 0.2s; }
.task-item-row:hover { background: #F5F0E6; }
.task-item-row.task-completed { opacity: 0.6; }
.task-icon-bg { width: 40px; height: 40px; border-radius: 10px; background: #F5F5F5; display: flex; align-items: center; justify-content: center; color: #8D6E63; flex-shrink: 0; }
.task-main { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.task-top { display: flex; justify-content: space-between; align-items: center; }
.task-top .name { font-weight: 600; color: #3E2723; font-size: 14px; }
.task-top .reward { font-size: 12px; color: #D97706; font-weight: 600; background: #FEF3C7; padding: 1px 8px; border-radius: 8px; }
.task-top .reward.claimed { color: #888; background: #F5F5F5; }
.task-desc { font-size: 12px; color: #888; }
.task-progress-bar { height: 3px; background: #E0E0E0; border-radius: 2px; overflow: hidden; }
.fill { height: 100%; border-radius: 2px; transition: width 0.5s ease; }
.fill.orange { background: #F59E0B; }
.fill.yellow { background: #EAB308; }
.fill.green { background: #22C55E; }
.fill.purple { background: #A855F7; }
.task-action { flex-shrink: 0; }
.status-text { font-size: 13px; color: #3E2723; font-weight: 600; }
.status-text.completed { color: #22C55E; }
</style>
