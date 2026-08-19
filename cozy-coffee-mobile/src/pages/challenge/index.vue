<!--
  月度挑战页 - 复现 prototype/challenge.html：Hero 仪表盘（进度 + 积分）+ 4 任务，达标自动发放积分（claimed），无手动领取
  数据源: /member/monthly-task (MonthlyTaskDTO)
-->
<template>
  <view class="challenge-page">
    <!-- Hero：会员成长仪表盘（非活动 Banner） -->
    <view class="challenge-hero">
      <text class="hero-eyebrow">COZY CHALLENGE</text>
      <text class="hero-title">{{ currentMonth }} 咖啡挑战</text>
      <text class="hero-sub">完成挑战，积分自动到账</text>

      <view class="hero-progress">
        <view class="hero-count-row">
          <text class="hero-count">{{ doneCount }}</text>
          <text class="hero-count-total">/ {{ tasks.length }}</text>
        </view>
        <text class="hero-progress-label">已完成挑战</text>
        <view class="hero-track">
          <view class="hero-track-fill" :style="{ transform: 'scaleX(' + (donePercent / 100) + ')' }"></view>
        </view>
        <text class="hero-earned">本月已获得 {{ earnedPoints }} pts</text>
      </view>
    </view>

    <!-- 任务列表 -->
    <view class="task-section">
      <LoadingState v-if="loading && tasks.length === 0" text="正在加载本月挑战…" />
      <RetryState v-else-if="errorMessage && tasks.length === 0" :description="errorMessage" @retry="loadTasks" />
      <template v-else>
        <view v-for="task in tasks" :key="task.key" class="task-row" :class="{ done: isDone(task) }">
          <view class="task-icon" :class="{ done: isDone(task) }">
            <CozyIcon :name="task.icon" :size="22" color="#753A22" />
          </view>
          <view class="task-main">
            <view class="task-top">
              <text class="task-name">{{ task.name }}</text>
              <text class="task-reward" :class="{ claimed: isDone(task) }">{{ isDone(task) ? '✓ 已发放' : `+${task.reward} 积分` }}</text>
            </view>
            <text class="task-desc">{{ task.desc }}</text>
            <view class="task-progress">
              <view class="task-progress-fill" :style="{ width: task.progress + '%' }"></view>
            </view>
          </view>
          <view class="task-status" :class="{ done: isDone(task) }">{{ displayCurrent(task) }}/{{ task.target }}</view>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMonthlyTask } from '@/api/member'
import LoadingState from '@/components/states/LoadingState.vue'
import RetryState from '@/components/states/RetryState.vue'
import CozyIcon from '@/components/CozyIcon.vue'

const loading = ref(false)
const errorMessage = ref('')
const taskData = ref({})

// 与 web 端 MonthlyChallengePanel / buildMonthlyChallenges 对齐
const TASKS = [
  { key: 'order', icon: 'calendar', name: '打卡达人', desc: '本月完成订单 4 次', target: 4, reward: 40, getCount: d => Number(d.monthlyOrderCount) || 0, getClaimed: d => Boolean(d.challengeOrderClaimed) },
  { key: 'morning', icon: 'sun', name: '晨间唤醒', desc: '10:00 前完成订单 3 次', target: 3, reward: 60, getCount: d => Number(d.morningOrderCount) || 0, getClaimed: d => Boolean(d.challengeMorningClaimed) },
  { key: 'delivery', icon: 'truck', name: '外卖尝鲜', desc: '完成 2 笔外卖订单', target: 2, reward: 50, getCount: d => Number(d.currentDeliveryOrders) || 0, getClaimed: d => Boolean(d.challengeDeliveryClaimed) },
  { key: 'newproduct', icon: 'bag', name: '新品猎人', desc: '尝试 3 款限定新品', target: 3, reward: 80, getCount: d => Number(d.newProductCount) || 0, getClaimed: d => Boolean(d.challengeNewproductClaimed) }
]

const currentMonth = computed(() => {
  const d = taskData.value?.taskMonth
  if (d && /^\d{4}-\d{2}$/.test(d)) return Number(d.slice(5)) + ' 月'
  return new Date().getMonth() + 1 + ' 月'
})

const tasks = computed(() => TASKS.map(task => {
  const current = task.getCount(taskData.value)
  const claimed = task.getClaimed(taskData.value)
  return {
    ...task,
    current,
    displayCurrent: Math.min(current, task.target),
    progress: Math.min(100, Math.round((current / task.target) * 100)),
    done: claimed || current >= task.target
  }
}))

const doneCount = computed(() => tasks.value.filter(t => t.done).length)
const earnedPoints = computed(() => tasks.value.filter(t => t.claimed).reduce((sum, t) => sum + t.reward, 0))
const donePercent = computed(() => {
  if (tasks.value.length === 0) return 0
  return Math.min(100, Math.round((doneCount.value / tasks.value.length) * 100))
})

function displayCurrent(task) { return task.displayCurrent }
function isDone(task) { return task.done }

async function loadTasks() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getMonthlyTask()
    if (response.code === 200 && response.data) taskData.value = response.data
    else errorMessage.value = response?.message || '月度挑战加载失败'
  } catch (error) {
    errorMessage.value = error?.message || '月度挑战加载失败'
  } finally {
    loading.value = false
  }
}

onShow(loadTasks)
</script>

<style lang="scss" scoped>
.challenge-page {
  min-height: 100vh;
  background: $cozy-surface;
  padding: 12rpx 40rpx 240rpx;
}

/* ── Hero（会员成长仪表盘 · 浅色卡） ── */
.challenge-hero {
  padding: 52rpx 48rpx 48rpx;
  border-radius: 24rpx;
  background: $bg-white;
  color: $cozy-ink;
}
.hero-eyebrow {
  display: block;
  font-size: 18rpx;
  font-weight: 700;
  letter-spacing: .26em;
  color: $cozy-muted;
}
.hero-title {
  display: block;
  margin-top: 24rpx;
  font-family: $font-display;
  font-size: 48rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.hero-sub {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: $cozy-muted;
}
.hero-progress { margin-top: 56rpx; }
.hero-count-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}
.hero-count {
  font-family: $font-display;
  font-size: 72rpx;
  font-weight: 700;
  line-height: 1;
  color: $cozy-ink;
}
.hero-count-total {
  font-family: $font-display;
  font-size: 36rpx;
  font-weight: 600;
  color: $cozy-placeholder;
}
.hero-progress-label {
  display: block;
  margin-top: 16rpx;
  font-size: 22rpx;
  font-weight: 650;
  letter-spacing: .12em;
  color: $cozy-muted;
}
.hero-track {
  margin-top: 32rpx;
  height: 6rpx;
  border-radius: 4rpx;
  background: $cozy-border;
  overflow: hidden;
}
.hero-track-fill {
  height: 100%;
  border-radius: 4rpx;
  background: $cozy-ink;
  transform-origin: left;
  transition: transform .5s ease;
}
.hero-earned {
  display: block;
  margin-top: 28rpx;
  font-size: 22rpx;
  letter-spacing: .04em;
  color: $cozy-muted;
}

/* ── 任务列表 ── */
.task-section {
  margin-top: 32rpx;
  padding: 16rpx 40rpx 24rpx;
  border-radius: 24rpx;
  background: $bg-white;
}
.task-row {
  display: flex;
  align-items: center;
  gap: 28rpx;
  padding: 32rpx 0;
  border-bottom: 1rpx solid $cozy-border;

  &:last-child { border-bottom: 0; }
  &.done { opacity: .72; }
}
.task-icon {
  flex: none;
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: $cozy-surface;
  color: $cozy-primary;
  display: flex;
  align-items: center;
  justify-content: center;

  &.done { background: $cozy-primary-soft; }
}
.task-main { flex: 1; min-width: 0; }
.task-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}
.task-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $cozy-ink;
}
.task-reward {
  flex: none;
  font-size: 24rpx;
  font-weight: 700;
  color: $cozy-primary;

  &.claimed {
    color: $cozy-caramel;
    font-weight: 650;
  }
}
.task-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $cozy-muted;
}
.task-progress {
  margin-top: 16rpx;
  height: 6rpx;
  border-radius: 4rpx;
  background: $cozy-border;
  overflow: hidden;
}
.task-progress-fill {
  height: 100%;
  border-radius: 4rpx;
  background: $cozy-primary;
  transition: width .5s ease;
}
.task-row.done .task-progress-fill { background: $cozy-ink; }
.task-status {
  flex: none;
  min-width: 88rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  text-align: center;
  background: $cozy-surface;
  color: $cozy-muted;
  font-size: 22rpx;
  font-weight: 700;

  &.done { background: $cozy-primary-soft; color: $cozy-primary; }
}
</style>
