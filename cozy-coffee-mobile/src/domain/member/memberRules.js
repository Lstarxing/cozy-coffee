export { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS } from '@/constants/member'

// 兑换折扣走后端 MemberDTO.redeemDiscount（RedemptionDiscountConfig 单一事实源），本地不再维护折扣表
export function getDiscountedPointsCost(pointsPrice, quantity = 1, discount = 1) {
  const price = Math.max(0, Number(pointsPrice) || 0)
  const count = Math.max(1, Math.floor(Number(quantity) || 1))
  return Math.ceil(price * count * Number(discount || 1))
}

export function getRedeemQuantityLimit(product = {}) {
  const stock = Math.max(0, Number(product.stock) || 0)
  const monthlyLimit = Number(product.monthlyLimit) || 0
  const redeemed = Math.max(0, Number(product.currentUserMonthlyRedeemed) || 0)
  const monthlyRemaining = monthlyLimit > 0 ? Math.max(0, monthlyLimit - redeemed) : Number.POSITIVE_INFINITY
  return Math.max(0, Math.min(stock, monthlyRemaining, 99))
}

// 挑战任务配置由后端 MonthlyTaskDTO.challenges 返回（单一事实源，含外送挑战），本地不再硬编码
export function buildMonthlyChallenges(task = {}) {
  const list = Array.isArray(task.challenges) ? task.challenges : []
  return list.map(item => ({
    key: item.key,
    title: item.title,
    description: item.description,
    current: Number(item.current) || 0,
    target: Number(item.target) || 0,
    reward: Number(item.reward) || 0,
    claimed: Boolean(item.claimed),
    displayCurrent: Math.min(Number(item.current) || 0, Number(item.target) || 0),
    progress: Math.min(100, Math.round(((Number(item.current) || 0) / (Number(item.target) || 1)) * 100))
  }))
}
