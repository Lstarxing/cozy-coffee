// 积分计算不再维护倍率表：倍率/折扣由后端 MemberDTO.pointsRate / redeemDiscount 传入（PointsRateConfig / RedemptionDiscountConfig 单一事实源）
export function calculateEarnedPoints(amount, rate = 1, isCozyDay = false) {
  return Math.round(Number(amount || 0) * (Number(rate || 0) + (isCozyDay ? 0.5 : 0)))
}

export function calculateRedemptionCost(basePrice, discount = 1, quantity = 1) {
  return Math.ceil(Number(basePrice || 0) * Math.max(1, quantity) * Number(discount || 1))
}

export function calculateProgress(currentPoints, targetPoints) {
  if (!targetPoints || targetPoints <= 0) return 0
  return Math.min(100, Math.round((Number(currentPoints || 0) / targetPoints) * 100))
}
