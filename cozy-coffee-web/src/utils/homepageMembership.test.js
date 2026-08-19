import { describe, expect, it } from 'vitest'
import {
  calculateEarnedPoints,
  calculateRedemptionCost,
  calculateProgress,
  POINTS_RATES_BY_LEVEL,
  REDEMPTION_DISCOUNTS_BY_LEVEL
} from './homepageMembership'

describe('calculateEarnedPoints', () => {
  it('applies the per-level points rate', () => {
    expect(calculateEarnedPoints(100, 'basic')).toBe(100)
    expect(calculateEarnedPoints(100, 'silver')).toBe(110)
    expect(calculateEarnedPoints(100, 'gold')).toBe(120)
    expect(calculateEarnedPoints(100, 'black')).toBe(150)
  })

  it('adds the CozyDay boost', () => {
    expect(calculateEarnedPoints(100, 'basic', true)).toBe(150)
    expect(calculateEarnedPoints(100, 'gold', true)).toBe(170)
  })

  it('rounds fractional results', () => {
    expect(calculateEarnedPoints(100.4, 'gold')).toBe(Math.round(120.48))
    expect(calculateEarnedPoints(0, 'gold')).toBe(0)
  })

  it('falls back to basic rate for unknown levels', () => {
    expect(calculateEarnedPoints(100, 'platinum')).toBe(100)
  })
})

describe('calculateRedemptionCost', () => {
  it('applies per-level redemption discount and ceil', () => {
    expect(calculateRedemptionCost(100, 'basic')).toBe(100)
    expect(calculateRedemptionCost(100, 'gold')).toBe(95)
    expect(calculateRedemptionCost(100, 'black')).toBe(85)
  })

  it('scales by quantity', () => {
    expect(calculateRedemptionCost(100, 'gold', 2)).toBe(190)
  })

  it('handles zero and unknown levels', () => {
    expect(calculateRedemptionCost(0, 'basic')).toBe(0)
    expect(calculateRedemptionCost(100, 'vip')).toBe(100)
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

describe('exported rate tables', () => {
  it('exposes every level in both tables', () => {
    expect(Object.keys(POINTS_RATES_BY_LEVEL)).toEqual(['basic', 'silver', 'gold', 'diamond', 'black'])
    expect(REDEMPTION_DISCOUNTS_BY_LEVEL.black).toBe(0.85)
  })
})
