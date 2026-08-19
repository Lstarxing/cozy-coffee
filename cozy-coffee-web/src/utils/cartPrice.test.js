import { describe, expect, it } from 'vitest'
import {
  computeBaseSubtotal,
  computeCupExtraTotal,
  computeStrengthExtraTotal,
  computeMilkExtraTotal,
  computeSubtotal,
  detectHasExtraShot,
  computeExtraShotCount,
  computeMemberDiscount,
  computeDeliveryFeeDiscount,
  computeShotDiscount,
  computeFinalTotal,
  computeEstimatedPoints
} from './cartPrice'

describe('cart subtotal helpers', () => {
  const items = [
    { productId: 'p1', unitPrice: 20, quantity: 2, extraPrices: { cup: 3, strength: 2, milk: 1 } },
    { productId: 'p2', basePrice: 25, unitPrice: 30, quantity: 1, extraPrices: { cup: 0, strength: 5, milk: 0 } }
  ]

  it('computeBaseSubtotal prefers basePrice and multiplies by quantity', () => {
    expect(computeBaseSubtotal(items)).toBe(20 * 2 + 25 * 1)
  })

  it('computeCupExtraTotal / strength / milk sum extra prices per quantity', () => {
    expect(computeCupExtraTotal(items)).toBe(3 * 2 + 0)
    expect(computeStrengthExtraTotal(items)).toBe(2 * 2 + 5)
    expect(computeMilkExtraTotal(items)).toBe(1 * 2 + 0)
  })

  it('computeSubtotal aggregates all parts', () => {
    expect(computeSubtotal(items)).toBe(40 + 25 + 6 + 9 + 2)
  })

  it('returns 0 for missing / non-array input', () => {
    expect(computeBaseSubtotal(null)).toBe(0)
    expect(computeSubtotal(undefined)).toBe(0)
    expect(computeStrengthExtraTotal('nope')).toBe(0)
  })
})

describe('extra shot detection', () => {
  it('detects STRONG strength', () => {
    expect(detectHasExtraShot([{ coffeeStrength: 'STRONG', quantity: 1 }])).toBe(true)
    expect(detectHasExtraShot([{ coffeeStrength: 'NORMAL', quantity: 1 }])).toBe(false)
  })

  it('detects extra shot via optionsJson', () => {
    expect(detectHasExtraShot([{ optionsJson: 'add_extra_shot' }])).toBe(true)
    expect(detectHasExtraShot([{ optionsJson: '加浓' }])).toBe(true)
    expect(detectHasExtraShot([{ optionsJson: 'none' }])).toBe(false)
  })

  it('computeExtraShotCount weights by quantity', () => {
    const items = [
      { coffeeStrength: 'STRONG', quantity: 2 },
      { coffeeStrength: 'NORMAL', quantity: 3 }
    ]
    expect(computeExtraShotCount(items)).toBe(2)
    expect(computeExtraShotCount([])).toBe(0)
  })
})

describe('computeMemberDiscount', () => {
  it('returns 0 for non-black levels', () => {
    expect(computeMemberDiscount([{ category: 'soe', unitPrice: 20, quantity: 1 }], 'basic')).toBe(0)
    expect(computeMemberDiscount([{ category: 'soe', unitPrice: 20, quantity: 1 }], 'gold')).toBe(0)
  })

  it('discounts only SOE category items at 15%', () => {
    const items = [
      { category: 'soe', unitPrice: 20, quantity: 2, cupSize: 'STANDARD' },
      { category: 'espresso', unitPrice: 20, quantity: 1 }
    ]
    // 20 * 2 * 0.15 = 6
    expect(computeMemberDiscount(items, 'black')).toBeCloseTo(6, 5)
  })

  it('adds ¥3 for LARGE cups before discounting', () => {
    const items = [{ category: 'soe', unitPrice: 20, quantity: 2, cupSize: 'LARGE' }]
    // (20 + 3) * 2 * 0.15 = 6.9
    expect(computeMemberDiscount(items, 'black')).toBeCloseTo(6.9, 5)
  })
})

describe('computeDeliveryFeeDiscount', () => {
  it('waives delivery for black-gold members on delivery orders', () => {
    expect(computeDeliveryFeeDiscount({ isBlackGoldMember: true, diningMethod: 'DELIVERY', deliveryFee: 3, hasDeliveryCoupon: false })).toBe(3)
  })

  it('applies delivery coupon capped at ¥10', () => {
    expect(computeDeliveryFeeDiscount({ isBlackGoldMember: false, diningMethod: 'DELIVERY', deliveryFee: 8, hasDeliveryCoupon: true })).toBe(8)
    expect(computeDeliveryFeeDiscount({ isBlackGoldMember: false, diningMethod: 'DELIVERY', deliveryFee: 12, hasDeliveryCoupon: true })).toBe(10)
  })

  it('returns 0 outside delivery mode', () => {
    expect(computeDeliveryFeeDiscount({ isBlackGoldMember: true, diningMethod: 'TAKEOUT', deliveryFee: 3, hasDeliveryCoupon: false })).toBe(0)
    expect(computeDeliveryFeeDiscount({ isBlackGoldMember: false, diningMethod: 'TAKEOUT', deliveryFee: 3, hasDeliveryCoupon: true })).toBe(0)
  })
})

describe('computeShotDiscount', () => {
  it('gives ¥5 per shot coupon, capped by strength extra total', () => {
    expect(computeShotDiscount({ shotCouponCount: 2, hasExtraShot: true, strengthExtraTotal: 12 })).toBe(10)
    expect(computeShotDiscount({ shotCouponCount: 1, hasExtraShot: true, strengthExtraTotal: 3 })).toBe(3)
  })

  it('returns 0 without extra shot or coupons', () => {
    expect(computeShotDiscount({ shotCouponCount: 1, hasExtraShot: false, strengthExtraTotal: 12 })).toBe(0)
    expect(computeShotDiscount({ shotCouponCount: 0, hasExtraShot: true, strengthExtraTotal: 12 })).toBe(0)
  })
})

describe('computeFinalTotal', () => {
  const base = {
    baseSubtotal: 40,
    cupExtraTotal: 4,
    milkExtraTotal: 3,
    strengthExtraTotal: 2,
    memberDiscount: 0,
    mainDiscount: 5,
    shotDiscount: 2,
    diningMethod: 'TAKEOUT',
    deliveryFee: 3,
    deliveryFeeDiscount: 0
  }

  it('computes takeout total as amount after discounts', () => {
    // total = 40+4+3+2 = 49; after 5+2 discounts = 42
    expect(computeFinalTotal(base)).toBe(42)
  })

  it('adds net delivery fee for delivery orders', () => {
    expect(computeFinalTotal({ ...base, diningMethod: 'DELIVERY', deliveryFeeDiscount: 3 })).toBe(42)
    expect(computeFinalTotal({ ...base, diningMethod: 'DELIVERY', deliveryFeeDiscount: 0 })).toBe(45)
  })

  it('never goes below zero', () => {
    expect(computeFinalTotal({ ...base, baseSubtotal: 5, cupExtraTotal: 0, milkExtraTotal: 0, strengthExtraTotal: 0, mainDiscount: 100 })).toBe(0)
  })

  it('applies member discount before main discount', () => {
    // 49 - 4 (member) - 5 - 2 = 38
    expect(computeFinalTotal({ ...base, memberDiscount: 4 })).toBe(38)
  })
})

describe('computeEstimatedPoints', () => {
  it('applies per-level base multiplier', () => {
    expect(computeEstimatedPoints({ amount: 50, level: 'basic', accelerateRemaining: 0 })).toBe(50)
    expect(computeEstimatedPoints({ amount: 50, level: 'gold', accelerateRemaining: 0 })).toBe(60)
    expect(computeEstimatedPoints({ amount: 50, level: 'black', accelerateRemaining: 0 })).toBe(75)
  })

  it('returns 0 for zero amount', () => {
    expect(computeEstimatedPoints({ amount: 0, level: 'basic', accelerateRemaining: 0 })).toBe(0)
  })

  it('uses 1.7x acceleration when amount fits in the black window', () => {
    expect(computeEstimatedPoints({ amount: 50, level: 'black', accelerateRemaining: 100 })).toBe(Math.floor(50 * 1.7))
  })

  it('splits acceleration and normal parts beyond the window', () => {
    // 30 * 1.7 + 20 * 1.5 = 51 + 30 = 81
    expect(computeEstimatedPoints({ amount: 50, level: 'black', accelerateRemaining: 30 })).toBe(81)
  })
})
