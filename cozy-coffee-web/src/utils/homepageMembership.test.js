import { describe, expect, it } from 'vitest'
import {
  calculateEarnedPoints,
  calculateRedemptionCost,
  calculateProgress
} from './homepageMembership'

// 倍率/折扣由后端 MemberDTO.pointsRate / redeemDiscount 传入（PointsRateConfig / RedemptionDiscountConfig），
// 前端只做纯计算；rate/discount 是数字，非会员等级字符串。

describe('calculateEarnedPoints', () => {
  it('multiplies amount by rate', () => {
    expect(calculateEarnedPoints(100, 1)).toBe(100)
    expect(calculateEarnedPoints(100, 1.1)).toBe(110)
    expect(calculateEarnedPoints(100, 1.5)).toBe(150)
  })

  it('adds the CozyDay boost (+0.5)', () => {
    expect(calculateEarnedPoints(100, 1, true)).toBe(150)
    expect(calculateEarnedPoints(100, 1.2, true)).toBe(170)
  })

  it('rounds fractional results', () => {
    expect(calculateEarnedPoints(100.4, 1.2)).toBe(Math.round(120.48))
    expect(calculateEarnedPoints(0, 1.2)).toBe(0)
  })

  it('defaults to rate 1 and non-cozy-day', () => {
    expect(calculateEarnedPoints(100)).toBe(100)
    expect(calculateEarnedPoints(0)).toBe(0)
  })
})

describe('calculateRedemptionCost', () => {
  it('applies discount and ceil', () => {
    expect(calculateRedemptionCost(100, 1)).toBe(100)
    expect(calculateRedemptionCost(100, 0.95)).toBe(95)
    expect(calculateRedemptionCost(100, 0.85)).toBe(85)
  })

  it('scales by quantity', () => {
    expect(calculateRedemptionCost(100, 0.85, 2)).toBe(170)
  })

  it('handles zero and missing discount', () => {
    expect(calculateRedemptionCost(0, 0.85)).toBe(0)
    expect(calculateRedemptionCost(100)).toBe(100)
  })
})

describe('calculateProgress', () => {
  it('computes a capped percentage', () => {
    expect(calculateProgress(50, 200)).toBe(25)
    expect(calculateProgress(300, 200)).toBe(100)
  })

  it('returns 0 for missing or zero target', () => {
    expect(calculateProgress(100, 0)).toBe(0)
    expect(calculateProgress(100, null)).toBe(0)
    expect(calculateProgress(0, 200)).toBe(0)
  })
})
