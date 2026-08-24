import { computed, ref } from 'vue'
import { getMemberInfo, getMonthlyTask } from '@/api/member'
import {
  calculateEarnedPoints,
  calculateProgress,
  calculateRedemptionCost
} from '@/utils/homepageMembership'

const membershipState = ref('auth-resolving')
const memberInfo = ref(null)
const monthlyTask = ref(null)

// 等级营销卡：权益/折扣/名称为静态文案；rate/cozyDay 倍率列需与后端 PointsRateConfig 保持同步
const levels = [
  { key: 'basic', name: '基础 Classic', threshold: '0 EXP', rate: '1.0×', cozyDay: '1.5×', discount: '—', benefit: '免费加浓缩券×1' },
  { key: 'silver', name: '白银 Silver', threshold: '500 EXP', rate: '1.1×', cozyDay: '1.6×', discount: '9.8 折', benefit: '配送券×1+加浓缩券×2 · 生日BOGO' },
  { key: 'gold', name: '黄金 Gold', threshold: '1,500 EXP', rate: '1.2×', cozyDay: '1.7×', discount: '9.5 折', benefit: 'BOGO×1+8.8折券×2+配送券×2 · 生日免单券' },
  { key: 'diamond', name: '钻石 Diamond', threshold: '4,000 EXP', rate: '1.3×', cozyDay: '1.8×', discount: '9.0 折', benefit: '免单券×1 + BOGO×2 + 配送券×5 + 新品5折券 · 生日蛋糕5折' },
  { key: 'black', name: '黑金 Black Gold', threshold: '9,000 EXP', rate: '1.5×', cozyDay: '2.0×', discount: '8.5 折', benefit: '1.7x加速包 · 免单券×2 + BOGO×5 · 无限免配送 · 新品试饮券 · 生日免单+蛋糕+888积分' }
]

const EXP_THRESHOLDS = { basic: 0, silver: 500, gold: 1500, diamond: 4000, black: 9000 }
const NEXT_LEVEL_MAP = { basic: 'silver', silver: 'gold', gold: 'diamond', diamond: 'black' }

let disposed = false

export function useHomeMembership(userStore) {
  const normalizedLevel = computed(() => {
    if (membershipState.value === 'anonymous') return 'silver'
    const level = memberInfo.value?.memberLevel || memberInfo.value?.level || 'basic'
    return ['basic', 'silver', 'gold', 'diamond', 'black'].includes(level) ? level : 'basic'
  })
  const currentLevel = computed(() => levels.find(level => level.key === normalizedLevel.value) || levels[0])
  const currentLevelLabel = computed(() => currentLevel.value.name.split(' ')[0] + '会员')
  // 倍率走后端 MemberDTO.pointsRate（单一事实源）；匿名展示兜底 1.1（白银示例）
  const currentRate = computed(() => Number(memberInfo.value?.pointsRate) || (membershipState.value === 'anonymous' ? 1.1 : 1))
  const currentPoints = computed(() => membershipState.value === 'anonymous' ? 131 : Number(memberInfo.value?.currentPoints || 0))
  const rewardTarget = computed(() => calculateRedemptionCost(150, normalizedLevel.value))
  const earnedPoints = computed(() => calculateEarnedPoints(35, currentRate.value, false))
  const levelLabel = computed(() => currentLevel.value.name.split(' ')[0])
  const currentDiscount = computed(() => currentLevel.value.discount)
  const remainingPoints = computed(() => Math.max(0, rewardTarget.value - currentPoints.value))
  const progressPercent = computed(() => calculateProgress(currentPoints.value, rewardTarget.value))

  const levelProgress = computed(() => {
    const exp = membershipState.value === 'anonymous' ? 850 : (memberInfo.value?.expTotal || 0)
    const currentLvl = normalizedLevel.value
    const nextKey = NEXT_LEVEL_MAP[currentLvl]
    const target = nextKey ? (EXP_THRESHOLDS[nextKey] || 500) : 99999
    const currentThreshold = EXP_THRESHOLDS[currentLvl] || 0
    const relativeExp = exp - currentThreshold
    const relativeTarget = target - currentThreshold
    const pct = relativeTarget <= 0 ? 100 : Math.max(0, Math.min(100, Math.round((relativeExp / relativeTarget) * 100)))
    const nextLevel = levels.find(l => l.key === nextKey)
    return {
      current: exp, target, percentage: pct,
      remaining: Math.max(0, target - exp),
      isMax: !nextKey,
      currentLevel: currentLevel.value,
      nextLevel,
      nextLevelName: nextLevel?.name?.split(' ')[0] || ''
    }
  })

  function isCurrentLevel(level) {
    return membershipState.value !== 'anonymous' && normalizedLevel.value === level
  }

  async function loadMemberData() {
    membershipState.value = 'member-loading'
    memberInfo.value = null
    monthlyTask.value = null

    const [memberResult, taskResult] = await Promise.allSettled([
      getMemberInfo(),
      getMonthlyTask()
    ])
    if (disposed) return

    if (memberResult.status === 'rejected' || !memberResult.value?.data) {
      membershipState.value = 'member-failed'
      return
    }

    memberInfo.value = memberResult.value.data
    if (userStore?.userInfo) Object.assign(userStore.userInfo, memberResult.value.data)

    if (taskResult.status === 'fulfilled' && taskResult.value?.data) {
      monthlyTask.value = taskResult.value.data
      membershipState.value = 'member-success'
    } else {
      membershipState.value = 'member-partial'
    }
  }

  async function initMembership() {
    if (!userStore?.isLoggedIn) {
      membershipState.value = 'anonymous'
      return
    }
    await loadMemberData()
  }

  function resetMembership() {
    disposed = true
  }

  function resetDisposed() {
    disposed = false
  }

  return {
    membershipState,
    memberInfo,
    monthlyTask,
    levels,
    normalizedLevel,
    currentLevel,
    currentLevelLabel,
    currentRate,
    currentPoints,
    rewardTarget,
    earnedPoints,
    levelLabel,
    currentDiscount,
    remainingPoints,
    progressPercent,
    levelProgress,
    isCurrentLevel,
    loadMemberData,
    initMembership,
    resetMembership,
    resetDisposed
  }
}
