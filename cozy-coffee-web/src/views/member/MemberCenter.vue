<!-- 会员中心 - 容器组件 -->
<template>
  <div class="dashboard-view">
    <header class="content-header">
      <h3>会员概览</h3>
      <div class="header-badges">
        <span v-if="isCozyDay" class="cozy-day-badge">
          <span class="badge-icon">☕</span>
          <span class="badge-text">今日会员日 · 积分+0.5x</span>
        </span>
        <span class="date-badge">{{ new Date().toLocaleDateString() }}</span>
      </div>
    </header>

    <div v-if="userStore.userInfo?.expiringPoints > 0" class="expiring-alert">
      <span class="alert-icon">⏰</span>
      <span>您有 <strong>{{ userStore.userInfo.expiringPoints }}</strong> 积分即将过期，快去使用吧</span>
      <button class="use-btn" @click="onNavigate('/member/benefits')">去兑换</button>
    </div>

    <MemberHeroCard
      :user-info="userStore.userInfo"
      :user-level="userStore.userLevel"
      :next-level-points="nextLevelPoints"
    />

    <SigninWidget
      :user-info="userStore.userInfo"
      :is-signed-today="isSignedToday"
      @signin-success="onSigninSuccess"
    />

    <div class="dashboard-split-layout">
      <div class="layout-col-left">
        <PointsGuidePanel
          :user-info="userStore.userInfo"
          :user-level="userStore.userLevel"
          :is-signed-today="isSignedToday"
          :profile-complete="profileComplete"
          :next-level-points="nextLevelPoints"
          @navigate="onNavigate"
        />
      </div>
      <div class="layout-col-right">
        <MonthlyChallengePanel
          ref="monthlyChallengePanelRef"
          :user-info="userStore.userInfo"
          :user-level="userStore.userLevel"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSseTicket } from '@/api/member'
import MemberHeroCard from './components/MemberHeroCard.vue'
import SigninWidget from './components/SigninWidget.vue'
import PointsGuidePanel from './components/PointsGuidePanel.vue'
import MonthlyChallengePanel from './components/MonthlyChallengePanel.vue'

const userStore = useUserStore()
const router = useRouter()
const monthlyChallengePanelRef = ref(null)

// Shared computed
const isCozyDay = computed(() => new Date().getDay() === 5)

const isSignedToday = computed(() => {
  const d = new Date()
  const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const lastSign = userStore.userInfo?.lastSigninDate || userStore.userInfo?.lastSignIn
  return lastSign === today
})

const profileComplete = computed(() => {
  const phone = userStore.userInfo?.phone
  const email = userStore.userInfo?.email
  return (phone && phone.length > 0) && (email && email.length > 0)
})

const nextLevelPoints = computed(() => {
  const lvl = userStore.userLevel || 'basic'
  const map = { basic: 500, silver: 1500, gold: 4000, diamond: 9000, black: 99999 }
  return map[lvl] || 500
})

// Event handlers
function onSigninSuccess(result) {
  userStore.userInfo.currentPoints = result.currentPoints
  userStore.userInfo.totalPoints = result.totalPoints
  userStore.userInfo.signInDays = result.consecutiveDays
  userStore.userInfo.consecutiveSignDays = result.consecutiveDays
  const d = new Date()
  userStore.userInfo.lastSigninDate = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function onNavigate(path) {
  router.push(path)
}

// SSE
const sseEventSource = ref(null)
const sseRetryCount = ref(0)
const MAX_SSE_RETRY = 10

function sseBackoff() {
  const base = Math.min(sseRetryCount.value, MAX_SSE_RETRY)
  const delay = Math.min(1000 * Math.pow(2, base), 30000)
  return delay + Math.random() * 1000
}

let sseRetryTimer = null

async function connectSSE() {
  try {
    if (!userStore.token) return
    const res = await getSseTicket()
    const ticket = res.data?.ticket || res.ticket
    if (sseEventSource.value) { sseEventSource.value.close(); sseEventSource.value = null }
    const sseBase = (import.meta.env.VITE_API_BASE_URL || '') + '/api'
    sseEventSource.value = new EventSource(`${sseBase}/member/sse/events?ticket=${ticket}`)
    sseEventSource.value.addEventListener('order_completed', (event) => {
      try {
        const payload = JSON.parse(event.data)
        ElMessage.success({ message: payload.message || '订单已完成', duration: 5000, showClose: true })
        userStore.fetchMemberInfo()
        monthlyChallengePanelRef.value?.refreshTaskData()
        sseRetryCount.value = 0
      } catch (e) { console.error('SSE消息解析失败', e) }
    })
    sseEventSource.value.onerror = () => {
      sseEventSource.value?.close()
      sseEventSource.value = null
      sseRetryCount.value++
      const delay = sseBackoff()
      console.warn(`SSE 连接断开，${Math.round(delay / 1000)}s 后重连 (第${sseRetryCount.value}次)`)
      sseRetryTimer = setTimeout(() => { if (userStore.isLoggedIn) connectSSE() }, delay)
    }
  } catch (e) { console.error('SSE 连接初始化失败', e) }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  userStore.fetchUserInfo()
  userStore.fetchMemberInfo()
  connectSSE()
})

onUnmounted(() => {
  if (sseEventSource.value) { sseEventSource.value.close(); sseEventSource.value = null }
  if (sseRetryTimer) { clearTimeout(sseRetryTimer); sseRetryTimer = null }
})
</script>

<style scoped>
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes cozy-day-pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.02); } }

.dashboard-view { animation: fadeIn 0.4s ease-out; max-width: 1400px; margin: 0 auto; }

.content-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 40px; gap: 8px; }
.content-header h3 { font-size: 28px; font-weight: 300; color: #1a1a1a; margin: 0; }
.date-badge { color: #888; font-size: 14px; }

.header-badges { display: flex; align-items: center; gap: 12px; }
.cozy-day-badge { display: inline-flex; align-items: center; gap: 6px; background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 500; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3); animation: cozy-day-pulse 2s ease-in-out infinite; }

.expiring-alert { background: #FFF3E0; border: 1px solid #FFE0B2; color: #E65100; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; font-size: 14px; }
.expiring-alert .use-btn { margin-left: auto; background: #fff; border: 1px solid #FF9800; color: #FF9800; padding: 4px 12px; border-radius: 14px; font-size: 12px; cursor: pointer; }
.expiring-alert .use-btn:hover { background: #FF9800; color: #fff; }

.dashboard-split-layout { display: grid; grid-template-columns: 360px 1fr; gap: 24px; margin-top: 24px; align-items: stretch; }
.layout-col-left, .layout-col-right { display: flex; flex-direction: column; height: 100%; }
</style>
