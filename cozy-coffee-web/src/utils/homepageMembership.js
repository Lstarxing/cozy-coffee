const POINTS_RATES = {
  basic: 1,
  silver: 1.1,
  gold: 1.2,
  diamond: 1.3,
  black: 1.5
}

const REDEMPTION_DISCOUNTS = {
  basic: 1,
  silver: 0.98,
  gold: 0.95,
  diamond: 0.9,
  black: 0.85
}

export function calculateEarnedPoints(amount, level = 'basic', isCozyDay = false) {
  const rate = (POINTS_RATES[level] || POINTS_RATES.basic) + (isCozyDay ? 0.5 : 0)
  return Math.round(Number(amount || 0) * rate)
}

export function calculateRedemptionCost(basePrice, level = 'basic', quantity = 1) {
  const discount = REDEMPTION_DISCOUNTS[level] || REDEMPTION_DISCOUNTS.basic
  return Math.ceil(Number(basePrice || 0) * Math.max(1, quantity) * discount)
}

export function calculateProgress(currentPoints, targetPoints) {
  if (!targetPoints || targetPoints <= 0) return 0
  return Math.min(100, Math.round((Number(currentPoints || 0) / targetPoints) * 100))
}

export const POINTS_RATES_BY_LEVEL = POINTS_RATES
export const REDEMPTION_DISCOUNTS_BY_LEVEL = REDEMPTION_DISCOUNTS
