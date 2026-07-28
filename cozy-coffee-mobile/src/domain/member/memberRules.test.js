import { describe, expect, it } from 'vitest'
import {
  MEMBER_LEVEL_THRESHOLDS,
  buildMonthlyChallenges,
  getDiscountedPointsCost,
  getRedeemQuantityLimit
} from './memberRules'

describe('memberRules', () => {
  it('uses the backend membership thresholds', () => {
    expect(MEMBER_LEVEL_THRESHOLDS).toEqual({
      basic: 0,
      silver: 500,
      gold: 1500,
      diamond: 4000,
      black: 9000
    })
  })

  it('calculates the same rounded-up redemption cost as the backend', () => {
    expect(getDiscountedPointsCost(101, 2, 'basic')).toBe(202)
    expect(getDiscountedPointsCost(101, 2, 'silver')).toBe(198)
    expect(getDiscountedPointsCost(101, 2, 'gold')).toBe(192)
    expect(getDiscountedPointsCost(101, 2, 'diamond')).toBe(182)
    expect(getDiscountedPointsCost(101, 2, 'black')).toBe(172)
  })

  it('limits quantity by stock and remaining monthly quota', () => {
    expect(getRedeemQuantityLimit({ stock: 8 })).toBe(8)
    expect(getRedeemQuantityLimit({ stock: 8, monthlyLimit: 3, currentUserMonthlyRedeemed: 2 })).toBe(1)
    expect(getRedeemQuantityLimit({ stock: 8, monthlyLimit: 3, currentUserMonthlyRedeemed: 3 })).toBe(0)
  })

  it('builds only the in-scope pickup challenges and clamps progress', () => {
    const tasks = buildMonthlyChallenges({
      monthlyOrderCount: 6,
      morningOrderCount: 1,
      newProductCount: 3,
      challengeOrderClaimed: true,
      currentDeliveryOrders: 2
    })

    expect(tasks.map(item => item.key)).toEqual(['order', 'morning', 'newproduct'])
    expect(tasks[0]).toMatchObject({ displayCurrent: 4, progress: 100, claimed: true })
    expect(tasks[1]).toMatchObject({ displayCurrent: 1, progress: 33 })
  })
})
