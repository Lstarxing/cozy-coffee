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
      <button class="use-btn" @click="router.push('/member/benefits')">去兑换</button>
    </div>

    <!-- 1. Hero Card -->
    <div class="digital-card premium-hero-card" :class="[userStore.userLevel || 'base', { dormant: userStore.userInfo?.memberStatus === 'DORMANT' }]">
      <div class="card-layer texture"></div>
      <div class="card-layer shine-effect"></div>
      <div v-if="userStore.userLevel === 'diamond'" class="card-layer holographic-overlay"></div>
      <div class="card-layer pattern-overlay"></div>

      <div class="hero-content-grid">
        <div class="brand-area">
          <div class="logo-circle">
            <svg class="start-logo" viewBox="0 0 24 24" fill="currentColor">
              <path d="M18.5,8H19C20.66,8 22,9.34 22,11V13C22,14.66 20.66,16 19,16H18.28C17.76,18.29 15.63,20 13,20H7C4.24,20 2,17.76 2,15V8H18.5ZM19,10H18V14H19C19.55,14 20,13.55 20,13V11C20,10.45 19.55,10 19,10ZM7,3H9V6H7V3ZM11,3H13V6H11V3ZM15,3H17V6H15V3Z"/>
            </svg>
          </div>
          <span class="brand-text">CozyCoffee</span>
        </div>

        <div class="points-area">
          <span class="caption">CURRENT POINTS</span>
          <span class="points-val">{{ userStore.userInfo?.currentPoints || 0 }}</span>
        </div>

        <div class="tier-emblem-area">
          <div class="emblem-3d-wrapper">
            <div v-if="userStore.userLevel === 'black'" class="emblem-shape crown">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M5 16L3 5L8.5 10L12 4L15.5 10L21 5L19 16H5M19 19C19 19.55 18.55 20 18 20H6C5.45 20 5 19.55 5 19V18H19V19Z" />
              </svg>
            </div>
            <div v-else-if="userStore.userLevel === 'diamond'" class="emblem-shape diamond">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,12l-7,10l-7,-10l3.5,-8h7l3.5,8z M12,3.5L8.5,8h7L12,3.5z"/>
              </svg>
            </div>
            <div v-else-if="['gold','silver'].includes(userStore.userLevel)" class="emblem-shape medal">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
            </div>
            <div v-else class="emblem-shape bean">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2 C17.5,2 22,6.5 22,12 C22,17.5 17.5,22 12,22 C6.5,22 2,17.5 2,12 C2,6.5 6.5,2 12,2 Z" />
                <path d="M12,5 C14,5 16,8 16,11 C16,15 13,18 12,18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="shine-overlay"></div>
          </div>
          <div class="tier-text">{{ levelName }}</div>
        </div>

        <div class="footer-area">
          <span class="member-code">ID: {{ userStore.userInfo?.memberCode }}</span>
          <span v-if="userStore.userInfo?.levelExpireDate" class="expiry">EXP: {{ userStore.userInfo.levelExpireDate }}</span>
        </div>
      </div>
    </div>

    <!-- 2. Stats Dashboard -->
    <div class="stats-premium-bar">
      <div class="stat-item">
        <div class="stat-icon-bg"><TrendingUp :size="25" /></div>
        <div class="stat-text">
          <span class="val">{{ userStore.userInfo?.expTotal || 0 }}</span>
          <span class="lbl">成长值</span>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon-bg"><CalendarCheck :size="25" /></div>
        <div class="stat-text">
          <span class="val">{{ userStore.userInfo?.signInDays || 0 }} <small>天</small></span>
          <span class="lbl">连续签到</span>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon-bg"><Target :size="25" /></div>
        <div class="stat-text">
          <span class="val highlight">{{ Math.max(0, nextLevelPoints - (userStore.userInfo?.expTotal || 0)) }}</span>
          <span class="lbl">距下一级</span>
        </div>
      </div>
    </div>

    <!-- 3. Sign In -->
    <div class="signin-premium-widget">
      <div class="widget-header">
        <div>
          <h4>每日签到</h4>
          <span class="sub">连续签到 7 天可领惊喜礼包</span>
        </div>
        <button class="signin-action-btn" :disabled="isSignedToday" @click="handleSignIn">
          {{ isSignedToday ? '今日已签' : '签到领豆' }}
        </button>
      </div>

      <div class="bean-track-wrapper">
        <div class="track-line-base">
          <div class="track-line-fill" :style="{ width: Math.max(0, (currentSignInCycleDay - 1) / 6 * 100) + '%' }"></div>
        </div>
        <div class="bean-steps">
          <div
v-for="day in 7" :key="day" class="bean-step"
            :class="{ 'is-active': day <= currentSignInCycleDay, 'is-today': day === currentSignInCycleDay }">
            <div class="bean-icon-box" :class="{ 'is-gift': day === 7 }">
              <template v-if="day === 7">
                <div class="gift-box-3d">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="gift-svg">
                    <polyline points="20 12 20 22 4 22 4 12"></polyline>
                    <rect x="2" y="7" width="20" height="5"></rect>
                    <line x1="12" y1="22" x2="12" y2="7"></line>
                    <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"></path>
                    <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"></path>
                  </svg>
                </div>
                <div v-if="day <= currentSignInCycleDay" class="gift-glow"></div>
              </template>
              <template v-else>
                <svg class="bean-svg" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,2 C6.5,2 2,6.5 2,12 C2,17.5 6.5,22 12,22 C17.5,22 22,17.5 22,12 C2,6.5 17.5,2 12,2 Z M12.5,5 C12.5,5 14,8 14,12 C14,16 12.5,19 12.5,19 C11,19 8,16 8,12 C8,8 11,5 12.5,5 Z" />
                </svg>
              </template>
            </div>
            <span class="step-label">{{ day === 7 ? '礼包' : '+2' }}</span>
          </div>
        </div>
      </div>

      <div v-if="currentSignInCycleDay < 7" class="widget-footer-info">
        <span class="info-icon">✨</span>
        <span>再签 <strong>{{ 7 - currentSignInCycleDay }}</strong> 天领满35-10券</span>
      </div>
      <div v-else class="widget-footer-info success">
        <span class="info-icon">🎁</span>
        <span>连续签到7天！礼包已到账</span>
      </div>
    </div>

    <!-- Web Split Layout -->
    <div class="dashboard-split-layout">
      <!-- Left Column: Points Guide -->
      <div class="layout-col-left">
        <div class="points-guide-section">
          <div class="section-title">
            <h4>积分获取</h4>
            <div class="header-right-action">
              <span class="subtitle">完成任务积攒成长值</span>
              <button class="link-btn-small" @click="openPointsDetailModal">明细 ></button>
            </div>
          </div>

          <div class="points-channels" style="gap: 20px;">
            <div class="channel-card" :class="{ done: isSignedToday }">
              <div class="channel-icon-bg"><i class="icon-calendar"></i></div>
              <div class="channel-info">
                <span class="channel-name">每日签到</span>
                <span class="channel-desc">连续签到额外奖励</span>
              </div>
              <div class="channel-points simple-row">
                <span>+{{ getSigninPointsByLevel() }} 积分/天</span>
              </div>
              <span v-if="isSignedToday" class="status-check"><Check :size="20" /></span>
            </div>

            <div class="channel-card" :class="{ done: profileComplete }">
              <div class="channel-icon-bg"><i class="icon-edit"></i></div>
              <div class="channel-info">
                <span class="channel-name">完善资料</span>
                <span class="channel-desc">填写手机号和邮箱</span>
              </div>
              <div class="channel-points simple-row">
                <span>+20 积分</span>
              </div>
              <span v-if="profileComplete" class="status-check"><Check :size="20" /></span>
              <button v-else class="go-btn" @click="router.push('/member/profile')">去完成</button>
            </div>

            <div class="channel-card">
              <div class="channel-icon-bg"><i class="icon-coffee"></i></div>
              <div class="channel-info">
                <span class="channel-name">消费赚积分</span>
              </div>
              <div class="channel-points simple-row">
                <span>1元={{ getConsumeMultiplier() }} 积分</span>
              </div>
              <button class="go-btn consume" @click="router.push('/member/order')">去下单</button>
            </div>
          </div>

          <div v-if="userStore.userLevel !== 'black'" class="level-tip">
            <div class="tip-content">
              <span class="tip-icon"><Rocket :size="18" /></span>
              <span class="tip-text">
                升级到 <strong>{{ nextLevelName }}</strong> 还需 <strong>{{ Math.max(0, nextLevelPoints - (userStore.userInfo?.expTotal || 0)) }}</strong> EXP
              </span>
            </div>
            <button class="view-benefits-text-btn" @click="router.push('/member/benefits')">查看权益</button>
          </div>

          <div class="promo-banner">
            <img src="/images/banner-promo.png" alt="Promo" />
          </div>
        </div>
      </div>

      <!-- Right Column: Monthly Challenge -->
      <div class="layout-col-right">
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

          <div v-if="userStore.userLevel === 'black'" class="black-accelerate-box">
            <div class="box-header">
              <div class="box-title">
                <span class="premium-icon">✨</span>
                <span class="title-text">黑卡加速包</span>
              </div>
              <div class="box-status">
                剩 <strong>¥{{ userStore.userInfo?.monthlyAccelerateRemaining ?? 0 }}</strong>
              </div>
            </div>
            <div class="accelerate-progress">
              <div class="progress-bar-bg">
                <div class="progress-fill" :style="{ width: accelerateProgressPercent + '%' }"></div>
              </div>
              <div class="progress-info">
                <span>已加速 ¥{{ (300 - (userStore.userInfo?.monthlyAccelerateRemaining ?? 0)).toFixed(2) }}</span>
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
                  <div class="fill orange" :style="{ width: Math.min(100, ((monthlyTaskData.monthlyOrderCount ?? userStore.userInfo?.monthlyOrderCount ?? 0) / 4) * 100) + '%' }"></div>
                </div>
              </div>
              <div class="task-action">
                <span class="status-text" :class="{ 'completed': isOrderTaskCompleted }">{{ monthlyTaskData.monthlyOrderCount ?? userStore.userInfo?.monthlyOrderCount ?? 0 }}/4</span>
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
                <div class="task-desc">10:00完成订单 3 次</div>
                <div class="task-progress-bar">
                  <div class="fill yellow" :style="{ width: Math.min(100, ((monthlyTaskData.morningOrderCount ?? userStore.userInfo?.morningOrderCount ?? 0) / 3) * 100) + '%' }"></div>
                </div>
              </div>
              <div class="task-action">
                <span class="status-text" :class="{ 'completed': isMorningTaskCompleted }">{{ monthlyTaskData.morningOrderCount ?? userStore.userInfo?.morningOrderCount ?? 0 }}/3</span>
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
                  <div class="fill green" :style="{ width: Math.min(100, ((monthlyTaskData.currentDeliveryOrders ?? userStore.userInfo?.monthlyDeliveryOrders ?? 0) / 2) * 100) + '%' }"></div>
                </div>
              </div>
              <div class="task-action">
                <span class="status-text" :class="{ 'completed': isDeliveryTaskCompleted }">{{ monthlyTaskData.currentDeliveryOrders ?? userStore.userInfo?.monthlyDeliveryOrders ?? 0 }}/2</span>
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
                  <div class="fill purple" :style="{ width: Math.min(100, ((monthlyTaskData.newProductCount ?? userStore.userInfo?.newProductCount ?? 0) / 3) * 100) + '%' }"></div>
                </div>
              </div>
              <div class="task-action">
                <span class="status-text" :class="{ 'completed': isNewProductTaskCompleted }">{{ monthlyTaskData.newProductCount ?? userStore.userInfo?.newProductCount ?? 0 }}/3</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 积分明细弹窗 -->
    <div v-if="showPointsDetailModal" class="points-detail-modal" @click.self="showPointsDetailModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3><List :size="20" style="margin-right: 8px; vertical-align: text-bottom;" />积分明细</h3>
          <button class="close-btn" @click="showPointsDetailModal = false">&times;</button>
        </div>
        <div class="balance-summary">
          <div class="balance-item">
            <span class="label">当前积分</span>
            <span class="value">{{ userStore.userInfo?.currentPoints || 0 }}</span>
          </div>
          <div class="balance-item">
            <span class="label">累计获得</span>
            <span class="value">{{ userStore.userInfo?.totalPoints || 0 }}</span>
          </div>
        </div>
        <div v-if="!isLoadingTransactions" class="transactions-list">
          <div v-if="pointsTransactions.length === 0" class="empty-state">暂无积分记录</div>
          <div v-for="item in pointsTransactions" v-else :key="item.id" class="transaction-item">
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { TrendingUp, CalendarCheck, Target, Check, Sun, Truck, ShoppingBag, Rocket, List, RefreshCw } from 'lucide-vue-next'
import { signIn, getMonthlyTask, getPointsTransactions, getSseTicket } from '@/api/member'

const userStore = useUserStore()
const router = useRouter()

// Sign-in computed
const isSignedToday = computed(() => {
  const d = new Date()
  const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const lastSign = userStore.userInfo?.lastSigninDate || userStore.userInfo?.lastSignIn
  return lastSign === today
})

const isCozyDay = computed(() => new Date().getDay() === 3)

const profileComplete = computed(() => {
  const phone = userStore.userInfo?.phone
  const email = userStore.userInfo?.email
  return (phone && phone.length > 0) && (email && email.length > 0)
})

const levelName = computed(() => {
  const lvl = userStore.userLevel || 'basic'
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[lvl] || '基础会员'
})

const nextLevelPoints = computed(() => {
  const lvl = userStore.userLevel || 'basic'
  const map = { basic: 500, silver: 1500, gold: 4000, diamond: 9000, black: 99999 }
  return map[lvl] || 500
})

const nextLevelName = computed(() => {
  const lvl = userStore.userLevel || 'basic'
  const map = { basic: '白银会员', silver: '黄金会员', gold: '钻石会员', diamond: '黑金会员', black: '黑金会员' }
  return map[lvl] || '白银会员'
})

const effectiveSignInDays = computed(() => {
  const info = userStore.userInfo
  if (!info || !info.lastSigninDate) return 0
  const days = info.signInDays || 0
  if (days === 0) return 0
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const lastSignDate = new Date(info.lastSigninDate); lastSignDate.setHours(0, 0, 0, 0)
  const diffTime = Math.abs(today - lastSignDate)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  if (diffDays > 1) return 0
  return days
})

const currentSignInCycleDay = computed(() => {
  const days = effectiveSignInDays.value
  if (days === 0) return 0
  return days % 7 === 0 ? 7 : days % 7
})

// Monthly task data
const monthlyTaskData = ref({
  currentSpent: 0, currentDeliveryOrders: 0,
  monthlyOrderCount: 0, morningOrderCount: 0, newProductCount: 0,
  challengeOrderClaimed: false, challengeMorningClaimed: false,
  challengeDeliveryClaimed: false, challengeNewproductClaimed: false
})

const isOrderTaskCompleted = computed(() => {
  const claimed = monthlyTaskData.value.challengeOrderClaimed ?? userStore.userInfo?.challengeOrderClaimed ?? false
  if (claimed) return true
  return (monthlyTaskData.value.monthlyOrderCount ?? userStore.userInfo?.monthlyOrderCount ?? 0) >= 4
})
const isMorningTaskCompleted = computed(() => {
  const claimed = monthlyTaskData.value.challengeMorningClaimed ?? userStore.userInfo?.challengeMorningClaimed ?? false
  if (claimed) return true
  return (monthlyTaskData.value.morningOrderCount ?? userStore.userInfo?.morningOrderCount ?? 0) >= 3
})
const isDeliveryTaskCompleted = computed(() => {
  const claimed = monthlyTaskData.value.challengeDeliveryClaimed ?? userStore.userInfo?.challengeDeliveryClaimed ?? false
  if (claimed) return true
  return (monthlyTaskData.value.currentDeliveryOrders ?? userStore.userInfo?.monthlyDeliveryOrders ?? 0) >= 2
})
const isNewProductTaskCompleted = computed(() => {
  const claimed = monthlyTaskData.value.challengeNewproductClaimed ?? userStore.userInfo?.challengeNewproductClaimed ?? false
  if (claimed) return true
  return (monthlyTaskData.value.newProductCount ?? userStore.userInfo?.newProductCount ?? 0) >= 3
})

const accelerateProgressPercent = computed(() => {
  const remaining = parseFloat(userStore.userInfo?.monthlyAccelerateRemaining ?? 300)
  const used = Math.max(0, 300 - remaining)
  return Math.min(100, (used / 300) * 100)
})

// Task refresh
const isRefreshingTask = ref(false)
const taskRefreshKey = ref(0)

// Points detail
const showPointsDetailModal = ref(false)
const pointsTransactions = ref([])
const isLoadingTransactions = ref(false)

// SSE
const sseEventSource = ref(null)

function getSigninPointsByLevel() { return 2 }

function getConsumeMultiplier() {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 1, silver: 1.1, gold: 1.2, diamond: 1.3, black: 1.5 }
  if (level === 'black' && (userStore.userInfo?.monthlyAccelerateRemaining || 0) > 0) return 1.7
  return map[level] || 1
}

function getSourceTypeName(type) {
  const map = { signin: '每日签到', register: '新用户注册', profile: '完善资料', consume: '消费赚积分', redeem: '积分兑换', cancel: '订单取消退还', invite: '邀请好友', invited: '受邀奖励' }
  return map[type] || type
}

function formatDateTime(dateStr) {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function handleSignIn() {
  try {
    if (!userStore.token) return
    const data = await signIn()
    const result = data.data
    ElMessage.success(result.message || `签到成功！积分+${result.pointsEarned}`)
    userStore.userInfo.currentPoints = result.currentPoints
    userStore.userInfo.totalPoints = result.totalPoints
    userStore.userInfo.signInDays = result.consecutiveDays
    userStore.userInfo.consecutiveSignDays = result.consecutiveDays
    const d = new Date()
    userStore.userInfo.lastSigninDate = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
  } catch (error) {
    ElMessage.error(error.message || '签到失败')
  }
}

async function loadMonthlyTaskData(retryCount = 0) {
  try {
    if (!userStore.token) return
    const data = await getMonthlyTask()
    const d = data.data
    const target = monthlyTaskData.value
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

async function openPointsDetailModal() {
  showPointsDetailModal.value = true
  isLoadingTransactions.value = true
  pointsTransactions.value = []
  try {
    const data = await getPointsTransactions({ limit: 50 })
    pointsTransactions.value = data.data || []
  } catch (error) {
    ElMessage.error(error.message || '系统错误')
  } finally {
    isLoadingTransactions.value = false
  }
}

async function connectSSE() {
  try {
    if (!userStore.token) return
    const res = await getSseTicket()
    const ticket = res.data?.ticket || res.ticket
    if (sseEventSource.value) sseEventSource.value.close()
    const sseBase = (import.meta.env.VITE_API_BASE_URL || '') + '/api'
    sseEventSource.value = new EventSource(`${sseBase}/member/sse/events?ticket=${ticket}`)
    sseEventSource.value.addEventListener('order_completed', (event) => {
      try {
        const payload = JSON.parse(event.data)
        ElMessage.success({ message: payload.message || '订单已完成', duration: 5000, showClose: true })
        userStore.fetchMemberInfo()
        loadMonthlyTaskData()
      } catch (e) { console.error('SSE消息解析失败', e) }
    })
    sseEventSource.value.onerror = (err) => { console.error('SSE 连接错误', err); sseEventSource.value.close() }
  } catch (e) { console.error('SSE 连接初始化失败', e) }
}

let monthlyTaskPollingTimer = null

function startMonthlyTaskPolling() {
  stopMonthlyTaskPolling()
  monthlyTaskPollingTimer = setInterval(() => loadMonthlyTaskData(), 30000)
}

function stopMonthlyTaskPolling() {
  if (monthlyTaskPollingTimer) { clearInterval(monthlyTaskPollingTimer); monthlyTaskPollingTimer = null }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  userStore.fetchUserInfo()
  userStore.fetchMemberInfo()
  await loadMonthlyTaskData()
  connectSSE()
  startMonthlyTaskPolling()
})

onUnmounted(() => {
  stopMonthlyTaskPolling()
  if (sseEventSource.value) { sseEventSource.value.close(); sseEventSource.value = null }
})
</script>

<style scoped>
.dashboard-view { animation: fadeIn 0.4s ease-out; max-width: 1400px; margin: 0 auto; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.content-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 40px; gap: 8px; }
.content-header h3 { font-size: 28px; font-weight: 300; color: #1a1a1a; margin: 0; }
.date-badge { color: #888; font-size: 14px; }

.header-badges { display: flex; align-items: center; gap: 12px; }
.cozy-day-badge { display: inline-flex; align-items: center; gap: 6px; background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 500; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3); animation: cozy-day-pulse 2s ease-in-out infinite; }
@keyframes cozy-day-pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.02); } }

.expiring-alert { background: #FFF3E0; border: 1px solid #FFE0B2; color: #E65100; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; font-size: 14px; }
.expiring-alert .use-btn { margin-left: auto; background: #fff; border: 1px solid #FF9800; color: #FF9800; padding: 4px 12px; border-radius: 14px; font-size: 12px; cursor: pointer; }
.expiring-alert .use-btn:hover { background: #FF9800; color: #fff; }

/* Hero Card */
.digital-card.premium-hero-card { position: relative; width: 100%; min-height: 240px; border-radius: 24px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0,0,0,0.3); font-family: 'Inter', sans-serif; color: #3E2723; margin-bottom: 24px; }
.hero-content-grid { position: relative; z-index: 10; height: 100%; padding: 32px; display: grid; grid-template-areas: "brand emblem" "points emblem" "footer emblem"; grid-template-columns: 1fr auto; grid-template-rows: auto 1fr auto; gap: 16px; }
.card-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; z-index: 1; }
.brand-area { grid-area: brand; display: flex; align-items: center; gap: 12px; }
.logo-circle { width: auto; height: auto; background: none !important; border-radius: 0; box-shadow: none !important; padding: 0; display: flex; align-items: center; justify-content: center; color: inherit; }
.start-logo { width: 28px; height: 28px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }
.brand-text { font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 800; letter-spacing: 3px; text-transform: uppercase; }
.points-area { grid-area: points; display: flex; flex-direction: column; justify-content: center; }
.points-area .caption { font-size: 11px; letter-spacing: 2px; opacity: 0.7; margin-bottom: 4px; font-weight: 600; }
.points-area .points-val { font-family: 'Playfair Display', serif; font-size: 56px; line-height: 1; font-weight: 700; }
.footer-area { grid-area: footer; display: flex; align-items: flex-end; gap: 16px; font-size: 12px; opacity: 0.8; font-family: monospace; }
.tier-emblem-area { grid-area: emblem; display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 100px; }
.emblem-3d-wrapper { width: 80px; height: 80px; position: relative; display: flex; align-items: center; justify-content: center; }
.emblem-shape { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 10px 20px rgba(0,0,0,0.3)); }
.emblem-shape svg { width: 64px; height: 64px; }
.tier-text { margin-top: 12px; font-weight: 700; font-size: 14px; letter-spacing: 1px; text-transform: uppercase; }

/* Tier card styles */
.premium-hero-card.base { background: #F5F0E6; color: #5D4037; }
.premium-hero-card.base .card-layer.texture { background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.1'/%3E%3C/svg%3E"); opacity: 0.4; }
.premium-hero-card.silver { background: linear-gradient(135deg, #E0E0E0 0%, #BDBDBD 100%); color: #424242; }
.premium-hero-card.gold { background: linear-gradient(135deg, #F3E5AB 0%, #D4AF37 100%); color: #4E342E; }
.premium-hero-card.diamond { background: linear-gradient(135deg, #CFD8DC 0%, #ECEFF1 50%, #B0BEC5 100%); color: #0D47A1; overflow: hidden; }
.premium-hero-card.diamond .card-layer.holographic-overlay { background: linear-gradient(45deg, rgba(33,150,243,0.1), rgba(0,229,255,0.1), rgba(101,31,255,0.1)); mix-blend-mode: color-dodge; animation: hologram 6s infinite linear; }
@keyframes hologram { 0% { filter: hue-rotate(0deg); } 100% { filter: hue-rotate(360deg); } }
.premium-hero-card.black { background: #121212; color: #FFD700; }
.premium-hero-card.black .brand-text, .premium-hero-card.black .points-val, .premium-hero-card.black .tier-text { background: linear-gradient(135deg, #FFD700 0%, #FDB931 100%); background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.premium-hero-card.dormant { background: linear-gradient(135deg, #2a2a2a, #3a3a3a, #2a2a2a) !important; border: 1px solid rgba(212, 175, 55, 0.3) !important; color: #888 !important; filter: saturate(0.5) brightness(0.8); }

/* Stats Bar */
.stats-premium-bar { display: grid; grid-template-columns: 1fr auto 1fr auto 1fr; align-items: center; background: #fff; padding: 24px; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); margin: 24px 0; border: 1px solid rgba(0,0,0,0.03); }
.stat-item { display: flex; align-items: center; gap: 16px; justify-content: center; }
.stat-icon-bg { width: 48px; height: 48px; border-radius: 12px; background: #F9F9F9; display: flex; align-items: center; justify-content: center; color: #8D6E63; }
.stat-text { display: flex; flex-direction: column; }
.stat-text .val { font-size: 20px; font-weight: 700; color: #3E2723; }
.stat-text .lbl { font-size: 12px; color: #9E9E9E; margin-top: 2px; }
.stat-divider { width: 1px; height: 40px; background: #EEEEEE; }

/* Signin Widget */
.signin-premium-widget { background: #fff; border-radius: 20px; padding: 24px; box-shadow: 0 8px 30px rgba(0,0,0,0.04); border: 1px solid rgba(0,0,0,0.02); margin-bottom: 24px; }
.widget-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.widget-header h4 { font-size: 18px; font-weight: 700; color: #3E2723; margin-bottom: 4px; }
.widget-header .sub { font-size: 13px; color: #8D6E63; }
.signin-action-btn { background: #3E2723; color: #FBEEA8; border: none; padding: 8px 24px; border-radius: 20px; font-size: 13px; font-weight: 600; cursor: pointer; }
.signin-action-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(62, 39, 35, 0.3); }
.signin-action-btn:disabled { background: #E0E0E0; color: #9E9E9E; cursor: not-allowed; transform: none; box-shadow: none; }
.bean-track-wrapper { position: relative; padding: 20px 0; }
.track-line-base { position: absolute; top: 36px; left: 20px; right: 20px; height: 4px; background: #F5F5F5; border-radius: 2px; transform: translateY(-50%); z-index: 1; }
.track-line-fill { height: 100%; background: #C69C6D; border-radius: 2px; transition: width 0.5s ease; }
.bean-steps { display: flex; justify-content: space-between; position: relative; z-index: 2; }
.bean-step { display: flex; flex-direction: column; align-items: center; gap: 8px; width: 40px; }
.bean-icon-box { width: 32px; height: 32px; background: #EFEBE9; border-radius: 50%; border: 2px solid #fff; display: flex; align-items: center; justify-content: center; color: #BCAAA4; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.bean-step.is-active .bean-icon-box { background: #5D4037; color: #FBEEA8; transform: scale(1.1); }
.bean-step.is-today .bean-icon-box { box-shadow: 0 0 0 4px rgba(198, 156, 109, 0.3); }
.bean-svg { width: 18px; height: 18px; }
.bean-icon-box.is-gift { width: 48px; height: 48px; border-radius: 16px; background: linear-gradient(135deg, #8D6E63 0%, #5D4037 100%); color: #FBEEA8; margin-top: -8px; display: flex; align-items: center; justify-content: center; }
.gift-svg { width: 24px; height: 24px; }
.step-label { font-size: 11px; color: #9E9E9E; font-weight: 500; }
.bean-step.is-active .step-label { color: #5D4037; font-weight: 700; }
.widget-footer-info { display: flex; align-items: center; gap: 8px; margin-top: 16px; padding: 12px 16px; background: #f9f9f9; border-radius: 8px; font-size: 14px; color: #555; }
.widget-footer-info.success { background: #f0fdf4; color: #059669; }

/* Split Layout */
.dashboard-split-layout { display: grid; grid-template-columns: 360px 1fr; gap: 24px; margin-top: 24px; align-items: stretch; }
.layout-col-left, .layout-col-right { display: flex; flex-direction: column; height: 100%; }

/* Points Guide */
.points-guide-section, .task-center-section { background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); height: 100%; display: flex; flex-direction: column; }
.section-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-title h4 { font-size: 18px; font-weight: 700; color: #1f2937; letter-spacing: -0.5px; margin: 0; }
.section-title .subtitle { font-size: 13px; color: #9ca3af; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
.header-right-action { display: flex; align-items: center; gap: 12px; }
.link-btn-small { background: none; border: none; font-size: 12px; color: #8D6E63; cursor: pointer; padding: 0; font-weight: 600; }
.link-btn-small:hover { text-decoration: underline; color: #5D4037; }
.promo-banner { margin-top: auto; padding-top: 24px; width: 100%; }
.promo-banner img { width: 100%; border-radius: 16px; display: block; object-fit: cover; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.channel-card { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 16px; border: 1px solid #F5F5F5; border-radius: 16px; background: #FAFAFA; transition: all 0.2s; }
.channel-info { flex: 1; display: flex; flex-direction: column; }
.channel-points.simple-row span { font-size: 14px; color: #C69C6D; font-weight: 400; white-space: nowrap; }
.channel-card:hover { background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.04); transform: translateY(-2px); border-color: #EFEBE9; }
.channel-card.done { background: #f8fff8; border-color: #4CAF50; }
.channel-name { display: block; font-weight: 600; color: #2C1810; font-size: 14px; }
.channel-desc { display: block; font-size: 12px; color: #888; margin-top: 2px; }
.go-btn { background: none; border: none; font-size: 13px; font-weight: 600; color: #8D6E63; cursor: pointer; padding: 6px 12px; border-radius: 8px; transition: all 0.2s; white-space: nowrap; }
.go-btn:hover { background: #EFEBE9; color: #5D4037; }
.status-check { color: #D97706; display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; background: #FEF3C7; border-radius: 50%; }
.level-tip { display: flex; align-items: center; gap: 10px; margin-top: 16px; padding: 12px 16px; background: white; border-radius: 10px; border: 1px dashed #C69C6D; }
.tip-content { display: flex; align-items: center; gap: 8px; flex: 1; }
.view-benefits-text-btn { background: none; border: none; color: #C69C6D; font-weight: 600; cursor: pointer; font-size: 12px; }

/* Task Section */
.black-accelerate-box { background: linear-gradient(135deg, #1a1a1a 0%, #2c2c2c 100%); border-radius: 16px; padding: 20px; color: #F7E7CE; margin-bottom: 24px; box-shadow: 0 8px 24px rgba(0,0,0,0.4); }
.black-accelerate-box .box-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.black-accelerate-box .box-title { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.black-accelerate-box .box-status strong { font-size: 18px; color: #fff; }
.title-text { color: #F7E7CE; font-weight: 700; }
.accelerate-progress { margin-bottom: 12px; }
.progress-bar-bg { height: 8px; background: rgba(255,255,255,0.15); border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #FDE68A 0%, #D97706 100%); border-radius: 4px; transition: width 0.6s ease; }
.accelerate-progress .progress-info { display: flex; justify-content: space-between; font-size: 12px; color: rgba(212, 175, 55, 0.7); }

.task-list-premium { display: flex; flex-direction: column; gap: 16px; }
.task-item-row { display: flex; align-items: center; gap: 16px; background: #fff; padding: 16px; border-radius: 16px; border: 1px solid #F6F6F6; transition: all 0.2s; }
.task-item-row:hover { border-color: #E0E0E0; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.04); }
.task-item-row.task-completed { background: linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%); border-color: #81C784; }
.task-item-row.task-completed .reward.claimed { color: #43A047; font-weight: 600; }
.task-icon-bg { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: #5D4037; background: #FAFAFA; border: 1px solid #EFEBE9; }
.task-main { flex: 1; }
.task-top { display: flex; justify-content: space-between; margin-bottom: 4px; }
.task-top .name { font-weight: 600; font-size: 14px; color: #3E2723; }
.task-top .reward { font-size: 12px; font-weight: 700; color: #C69C6D; }
.task-desc { font-size: 12px; color: #9E9E9E; margin-bottom: 6px; }
.task-progress-bar { height: 4px; background: #F5F5F5; border-radius: 2px; width: 100%; }
.task-progress-bar .fill { height: 100%; border-radius: 2px; }
.fill.orange { background: linear-gradient(90deg, #FFCC80 0%, #EF6C00 100%); }
.fill.yellow { background: linear-gradient(90deg, #FFF59D 0%, #FBC02D 100%); }
.fill.green { background: linear-gradient(90deg, #A5D6A7 0%, #43A047 100%); }
.fill.purple { background: linear-gradient(90deg, #CE93D8 0%, #8E24AA 100%); }
.task-action .status-text { font-size: 12px; font-weight: 600; color: #3E2723; background: #F5F5F5; padding: 4px 10px; border-radius: 12px; }

/* Refresh button */
.refresh-btn { margin-left: auto; background: transparent; border: none; cursor: pointer; padding: 6px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #8B4513; transition: all 0.3s ease; }
.refresh-btn:hover:not(:disabled) { background: rgba(139, 69, 19, 0.1); transform: rotate(15deg); }
.refresh-btn:disabled { cursor: not-allowed; opacity: 0.5; }
.refresh-btn .spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* Points Detail Modal */
.points-detail-modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.points-detail-modal .modal-content { background: white; border-radius: 20px; width: 90%; max-width: 500px; max-height: 80vh; display: flex; flex-direction: column; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3); }
.points-detail-modal .modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #eee; }
.points-detail-modal h3 { margin: 0; font-size: 20px; }
.points-detail-modal .close-btn { background: none; border: none; font-size: 28px; color: #999; cursor: pointer; padding: 0; line-height: 1; }
.points-detail-modal .close-btn:hover { color: #333; }
.balance-summary { display: flex; gap: 20px; padding: 20px 24px; background: linear-gradient(135deg, #FFF9F0 0%, #FFF5E6 100%); }
.balance-summary .balance-item { flex: 1; text-align: center; }
.balance-summary .label { display: block; font-size: 12px; color: #888; margin-bottom: 4px; }
.balance-summary .value { display: block; font-size: 24px; font-weight: 700; color: #C69C6D; }
.transactions-list { flex: 1; overflow-y: auto; padding: 16px 24px; }
.transaction-item { display: flex; justify-content: space-between; align-items: flex-start; padding: 14px 0; border-bottom: 1px solid #f0f0f0; }
.transaction-left { flex: 1; }
.transaction-type { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 500; }
.transaction-type.income { background: #E8F5E9; color: #4CAF50; }
.transaction-type.expense { background: #FFF3E0; color: #FF9800; }
.transaction-desc { display: block; font-size: 13px; color: #333; margin-top: 4px; }
.transaction-time { display: block; font-size: 11px; color: #999; margin-top: 2px; }
.transaction-right { text-align: right; }
.transaction-amount { display: block; font-size: 16px; font-weight: 600; }
.transaction-amount.income { color: #4CAF50; }
.transaction-amount.expense { color: #FF9800; }
.transaction-balance { display: block; font-size: 11px; color: #999; margin-top: 2px; }
.loading-state, .empty-state { padding: 40px; text-align: center; color: #999; }

@media (max-width: 1024px) { .dashboard-split-layout { grid-template-columns: 1fr; } }
</style>
