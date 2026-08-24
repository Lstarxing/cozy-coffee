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
    expect(getDiscountedPointsCost(101, 2, 1)).toBe(202)
    expect(getDiscountedPointsCost(101, 2, 0.98)).toBe(198)
    expect(getDiscountedPointsCost(101, 2, 0.95)).toBe(192)
    expect(getDiscountedPointsCost(101, 2, 0.90)).toBe(182)
    expect(getDiscountedPointsCost(101, 2, 0.85)).toBe(172)
  })

  it('limits quantity by stock and remaining monthly quota', () => {
    expect(getRedeemQuantityLimit({ stock: 8 })).toBe(8)
    expect(getRedeemQuantityLimit({ stock: 8, monthlyLimit: 3, currentUserMonthlyRedeemed: 2 })).toBe(1)
    expect(getRedeemQuantityLimit({ stock: 8, monthlyLimit: 3, currentUserMonthlyRedeemed: 3 })).toBe(0)
  })

  it('builds challenges from backend task.challenges and clamps progress', () => {
    const tasks = buildMonthlyChallenges({
      challenges: [
        { key: 'order', title: '打卡达人', description: '当月完成 4 笔订单', current: 6, target: 4, reward: 40, claimed: true },
        { key: 'morning', title: '晨间唤醒', description: '当月完成 3 笔上午 10 点前订单', current: 1, target: 3, reward: 60, claimed: false }
      ]
    })

    expect(tasks.map(item => item.key)).toEqual(['order', 'morning'])
    expect(tasks[0]).toMatchObject({ displayCurrent: 4, progress: 100, claimed: true })
    expect(tasks[1]).toMatchObject({ displayCurrent: 1, progress: 33 })
  })
})
