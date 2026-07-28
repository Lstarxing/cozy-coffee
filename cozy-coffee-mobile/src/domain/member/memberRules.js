export { MEMBER_LEVELS, MEMBER_LEVEL_THRESHOLDS } from '@/constants/member'

export const POINTS_REDEEM_DISCOUNTS = Object.freeze({
  basic: 1,
  silver: 0.98,
  gold: 0.95,
  diamond: 0.9,
  black: 0.85
})

export function getDiscountedPointsCost(pointsPrice, quantity = 1, memberLevel = 'basic') {
  const price = Math.max(0, Number(pointsPrice) || 0)
  const count = Math.max(1, Math.floor(Number(quantity) || 1))
  const discount = POINTS_REDEEM_DISCOUNTS[memberLevel] || 1
  return Math.ceil(price * count * discount)
}

export function getRedeemQuantityLimit(product = {}) {
  const stock = Math.max(0, Number(product.stock) || 0)
  const monthlyLimit = Number(product.monthlyLimit) || 0
  const redeemed = Math.max(0, Number(product.currentUserMonthlyRedeemed) || 0)
  const monthlyRemaining = monthlyLimit > 0 ? Math.max(0, monthlyLimit - redeemed) : Number.POSITIVE_INFINITY
  return Math.max(0, Math.min(stock, monthlyRemaining, 99))
}

export function buildMonthlyChallenges(task = {}) {
  return [
    {
      key: 'order',
      title: '打卡达人',
      description: '当月完成 4 笔订单',
      current: Number(task.monthlyOrderCount) || 0,
      target: 4,
      reward: 40,
      claimed: Boolean(task.challengeOrderClaimed)
    },
    {
      key: 'morning',
      title: '晨间唤醒',
      description: '当月完成 3 笔上午 10 点前订单',
      current: Number(task.morningOrderCount) || 0,
      target: 3,
      reward: 60,
      claimed: Boolean(task.challengeMorningClaimed)
    },
    {
      key: 'newproduct',
      title: '新品猎人',
      description: '当月购买 3 款新品',
      current: Number(task.newProductCount) || 0,
      target: 3,
      reward: 80,
      claimed: Boolean(task.challengeNewproductClaimed)
    }
  ].map(item => ({
    ...item,
    displayCurrent: Math.min(item.current, item.target),
    progress: Math.min(100, Math.round((item.current / item.target) * 100))
  }))
}
