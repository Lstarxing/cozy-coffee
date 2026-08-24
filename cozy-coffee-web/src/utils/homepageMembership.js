const REDEMPTION_DISCOUNTS = {
  basic: 1,
  silver: 0.98,
  gold: 0.95,
  diamond: 0.9,
  black: 0.85
}

// 积分计算不再维护倍率表：倍率由后端 MemberDTO.pointsRate 传入（PointsRateConfig 单一事实源）
export function calculateEarnedPoints(amount, rate = 1, isCozyDay = false) {
  return Math.round(Number(amount || 0) * (Number(rate || 0) + (isCozyDay ? 0.5 : 0)))
}

export function calculateRedemptionCost(basePrice, level = 'basic', quantity = 1) {
  const discount = REDEMPTION_DISCOUNTS[level] || REDEMPTION_DISCOUNTS.basic
  return Math.ceil(Number(basePrice || 0) * Math.max(1, quantity) * discount)
}

export function calculateProgress(currentPoints, targetPoints) {
  if (!targetPoints || targetPoints <= 0) return 0
  return Math.min(100, Math.round((Number(currentPoints || 0) / targetPoints) * 100))
}

export const REDEMPTION_DISCOUNTS_BY_LEVEL = REDEMPTION_DISCOUNTS
