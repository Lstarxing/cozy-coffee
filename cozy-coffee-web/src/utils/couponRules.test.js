import { describe, expect, it } from 'vitest'
import { COUPON_TYPE } from '@/constants/coupon'
import {
  isDrinkCategory,
  isBakeryCategory,
  filterCoupons,
  validateCouponForCart,
  calculateDiscountAmount,
  calculateCouponDiscount,
  filterMainCoupons,
  findBestCoupon,
  getAddonCouponDesc,
  getAddonCouponTip,
  isAddonCouponDisabled,
  allocateItemDiscounts
} from './couponRules'

// ──────────────────────────────────────────────
//  Category helpers
// ──────────────────────────────────────────────

describe('isDrinkCategory', () => {
  it('recognizes known drink categories', () => {
    expect(isDrinkCategory('espresso')).toBe(true)
    expect(isDrinkCategory('signature')).toBe(true)
    expect(isDrinkCategory('cold_brew')).toBe(true)
    expect(isDrinkCategory('tea')).toBe(true)
    expect(isDrinkCategory('drink')).toBe(true)
  })

  it('rejects bakery/snack/dessert/food categories', () => {
    expect(isDrinkCategory('bakery')).toBe(false)
    expect(isDrinkCategory('snack')).toBe(false)
    expect(isDrinkCategory('dessert')).toBe(false)
    expect(isDrinkCategory('food')).toBe(false)
  })

  it('treats unknown non-food categories as drinks', () => {
    expect(isDrinkCategory('milk')).toBe(true)
    expect(isDrinkCategory('pour-over')).toBe(true)
  })

  it('returns false for empty/undefined category', () => {
    expect(isDrinkCategory(undefined)).toBe(false)
    expect(isDrinkCategory('')).toBe(false)
    expect(isDrinkCategory(null)).toBe(false)
  })
})

describe('isBakeryCategory', () => {
  it('recognizes bakery categories case-insensitively', () => {
    expect(isBakeryCategory('bakery')).toBe(true)
    expect(isBakeryCategory('snack')).toBe(true)
    expect(isBakeryCategory('DESSERT')).toBe(true)
    expect(isBakeryCategory('cake')).toBe(true)
  })

  it('returns false for empty and drinks', () => {
    expect(isBakeryCategory('')).toBe(false)
    expect(isBakeryCategory(null)).toBe(false)
    expect(isBakeryCategory(undefined)).toBe(false)
    expect(isBakeryCategory('espresso')).toBe(false)
  })
})

// ──────────────────────────────────────────────
//  Coupon filtering
// ──────────────────────────────────────────────

describe('filterCoupons', () => {
  const coupons = [
    { couponCode: 'D1', couponType: COUPON_TYPE.DISCOUNT },
    { couponCode: 'FR1', couponType: COUPON_TYPE.FULL_REDUCE },
    { couponCode: 'DF1', couponType: COUPON_TYPE.DELIVERY_FEE },
    { couponCode: 'DF2', couponType: COUPON_TYPE.DELIVERY_FEE },
    { couponCode: 'S1', couponType: COUPON_TYPE.SHOT },
    { couponCode: 'S2', couponType: COUPON_TYPE.SHOT }
  ]

  it('splits main vs addon coupons', () => {
    const { mainCoupons, addonCoupons } = filterCoupons(coupons)
    expect(mainCoupons.map(c => c.couponCode)).toEqual(['D1', 'FR1'])
    // only the first DELIVERY_FEE is kept; all SHOT coupons are kept
    expect(addonCoupons.map(c => c.couponCode)).toEqual(['DF1', 'S1', 'S2'])
  })

  it('handles empty input', () => {
    expect(filterCoupons([])).toEqual({ mainCoupons: [], addonCoupons: [] })
  })
})

// ──────────────────────────────────────────────
//  Validation
// ──────────────────────────────────────────────

const drinkItem = { productId: 'p1', category: 'espresso', quantity: 1, cupSize: 'STANDARD', unitPrice: 25 }
const cakeItem = { productId: 'p2', category: 'cake', quantity: 1, unitPrice: 30 }

describe('validateCouponForCart', () => {
  const baseContext = { hasExtraShot: false, diningMethod: 'TAKEOUT' }

  it('accepts a valid DISCOUNT coupon on a drink cart', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: {} }
    expect(validateCouponForCart(coupon, [drinkItem], baseContext)).toEqual({ valid: true, reason: '' })
  })

  it('rejects NEW_PRODUCT coupons without a new product', () => {
    const coupon = { couponType: 'NEW_PRODUCT_HALF', parsedRule: {} }
    expect(validateCouponForCart(coupon, [drinkItem], baseContext).valid).toBe(false)
    const couponFree = { couponType: 'NEW_PRODUCT_FREE', parsedRule: {} }
    expect(validateCouponForCart(couponFree, [drinkItem], baseContext).valid).toBe(false)
    const withNew = { ...drinkItem, isNewProduct: true }
    expect(validateCouponForCart(coupon, [withNew], baseContext).valid).toBe(true)
  })

  it('enforces STANDARD_ONLY sku limit', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: { skuLimit: 'STANDARD_ONLY' } }
    const largeDrink = { ...drinkItem, cupSize: 'LARGE' }
    expect(validateCouponForCart(coupon, [largeDrink], baseContext).valid).toBe(false)
    expect(validateCouponForCart(coupon, [drinkItem], baseContext).valid).toBe(true)
    // MEDIUM counts as standard
    const medium = { ...drinkItem, cupSize: 'MEDIUM' }
    expect(validateCouponForCart(coupon, [medium], baseContext).valid).toBe(true)
  })

  it('enforces category blocklist (signature / 手冲 excluded)', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: { categoryBlocklist: ['signature'] } }
    const blocked = { ...drinkItem, category: 'signature' }
    expect(validateCouponForCart(coupon, [blocked], baseContext).valid).toBe(false)
    expect(validateCouponForCart(coupon, [drinkItem], baseContext).valid).toBe(true)
  })

  it('enforces linked product constraint', () => {
    const coupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: { linkedProductId: 'p9' } }
    expect(validateCouponForCart(coupon, [drinkItem], baseContext).valid).toBe(false)
    const linked = { ...drinkItem, productId: 'p9' }
    expect(validateCouponForCart(coupon, [linked], baseContext).valid).toBe(true)
    // linked item must be standard/medium size
    const linkedLarge = { ...linked, cupSize: 'LARGE' }
    expect(validateCouponForCart(coupon, [linkedLarge], baseContext).valid).toBe(false)
  })

  it('distinguishes generic exchange coupons for cake vs drink', () => {
    const cakeCoupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: {}, displayTitle: '蛋糕兑换券' }
    expect(validateCouponForCart(cakeCoupon, [drinkItem], baseContext).valid).toBe(false)
    expect(validateCouponForCart(cakeCoupon, [cakeItem], baseContext).valid).toBe(true)

    const drinkCoupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: {}, displayTitle: '拿铁兑换券' }
    expect(validateCouponForCart(drinkCoupon, [cakeItem], baseContext).valid).toBe(false)
    expect(validateCouponForCart(drinkCoupon, [drinkItem], baseContext).valid).toBe(true)
  })

  it('requires an extra-shot item for SHOT coupons', () => {
    const coupon = { couponType: COUPON_TYPE.SHOT, parsedRule: {} }
    expect(validateCouponForCart(coupon, [drinkItem], baseContext).valid).toBe(false)
    expect(validateCouponForCart(coupon, [drinkItem], { ...baseContext, hasExtraShot: true }).valid).toBe(true)
  })

  it('restricts DELIVERY_FEE coupons to delivery orders', () => {
    const coupon = { couponType: COUPON_TYPE.DELIVERY_FEE, parsedRule: {} }
    expect(validateCouponForCart(coupon, [drinkItem], baseContext).valid).toBe(false)
    expect(validateCouponForCart(coupon, [drinkItem], { ...baseContext, diningMethod: 'DELIVERY' }).valid).toBe(true)
  })

  it('requires >=2 drinks for BOGO coupons', () => {
    const coupon = { couponType: COUPON_TYPE.BOGO, parsedRule: {} }
    expect(validateCouponForCart(coupon, [drinkItem], baseContext).valid).toBe(false)
    expect(validateCouponForCart(coupon, [{ ...drinkItem, quantity: 2 }], baseContext).valid).toBe(true)
  })

  it('enforces DISCOUNT scope constraints', () => {
    const drinkOnly = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: { scope: 'DRINK_ONLY' } }
    expect(validateCouponForCart(drinkOnly, [cakeItem], baseContext).valid).toBe(false)
    const cakeOnly = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: { scope: 'CAKE_ONLY' } }
    expect(validateCouponForCart(cakeOnly, [drinkItem], baseContext).valid).toBe(false)
    expect(validateCouponForCart(cakeOnly, [cakeItem], baseContext).valid).toBe(true)
  })

  it('handles missing inputs without throwing', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: {} }
    expect(validateCouponForCart(coupon, null, baseContext)).toEqual({ valid: true, reason: '' })
    expect(validateCouponForCart({ couponType: COUPON_TYPE.DISCOUNT }, [drinkItem], baseContext).valid).toBe(true)
  })
})

// ──────────────────────────────────────────────
//  Discount calculation
// ──────────────────────────────────────────────

describe('calculateDiscountAmount', () => {
  const pricing = { baseSubtotal: 50, cupExtraTotal: 4 }
  const items = [drinkItem, { ...drinkItem, productId: 'p3', quantity: 1, cupSize: 'LARGE', unitPrice: 28 }]

  it('computes a percentage discount across drinks', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, value: 20, parsedRule: {} }
    // baseAmount = 50 + 4 = 54 (plus LARGE +3 on the LARGE item)
    const discount = calculateDiscountAmount(coupon, items, pricing)
    expect(discount).toBeCloseTo((54 + 3) * 0.8, 5)
  })

  it('derives percent from discountRate when value missing', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: { discountRate: 0.15 } }
    expect(calculateDiscountAmount(coupon, items, pricing)).toBeCloseTo((54 + 3) * 0.85, 5)
    const coupon2 = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: { discountRate: 5 } }
    expect(calculateDiscountAmount(coupon2, items, pricing)).toBeCloseTo((54 + 3) * 0.5, 5)
  })

  it('caps the discount at maxDiscountAmount', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, value: 50, parsedRule: { maxDiscountAmount: 10 } }
    expect(calculateDiscountAmount(coupon, items, pricing)).toBe(10)
  })

  it('returns 0 for invalid percent', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, value: 0, parsedRule: {} }
    expect(calculateDiscountAmount(coupon, items, pricing)).toBe(0)
  })

  it('uses only the most expensive bakery item for CAKE_ONLY', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, value: 10, parsedRule: { scope: 'CAKE_ONLY' } }
    const bakeryItems = [{ ...cakeItem, unitPrice: 20 }, { ...cakeItem, productId: 'p4', unitPrice: 40 }]
    expect(calculateDiscountAmount(coupon, bakeryItems, pricing)).toBeCloseTo(40 * 0.9, 5)
  })

  it('handles SINGLE_ITEM limit on the most expensive drink', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, value: 50, parsedRule: { limit: 'SINGLE_ITEM' } }
    // max drink = LARGE 28 + 3 = 31
    expect(calculateDiscountAmount(coupon, items, pricing)).toBeCloseTo(31 * 0.5, 5)
  })
})

describe('calculateCouponDiscount', () => {
  const pricing = { baseSubtotal: 40, cupExtraTotal: 0, subtotal: 45 }

  it('returns 0 for null coupon', () => {
    expect(calculateCouponDiscount(null, [], pricing)).toBe(0)
  })

  it('handles DISCOUNT via calculateDiscountAmount', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, value: 50, parsedRule: {} }
    expect(calculateCouponDiscount(coupon, [{ ...drinkItem, quantity: 2 }], pricing)).toBe(25)
  })

  it('computes BOGO as the cheapest drink price', () => {
    const coupon = { couponType: COUPON_TYPE.BOGO, parsedRule: {} }
    const items = [
      { ...drinkItem, quantity: 1, unitPrice: 30 },
      { ...drinkItem, productId: 'p5', quantity: 1, unitPrice: 18 }
    ]
    expect(calculateCouponDiscount(coupon, items, pricing)).toBe(18)
  })

  it('caps BOGO discount per cup with maxDiscount', () => {
    const coupon = { couponType: COUPON_TYPE.BOGO, parsedRule: { maxDiscount: 15 } }
    const items = [{ ...drinkItem, quantity: 2, unitPrice: 30 }]
    expect(calculateCouponDiscount(coupon, items, pricing)).toBe(15)
  })

  it('FULL_REDUCE applies when subtotal meets minAmount', () => {
    const coupon = { couponType: COUPON_TYPE.FULL_REDUCE, value: 10, minAmount: 40, parsedRule: {} }
    expect(calculateCouponDiscount(coupon, [], pricing)).toBe(10)
    const below = { ...coupon, minAmount: 50 }
    expect(calculateCouponDiscount(below, [], pricing)).toBe(0)
  })

  it('EXCHANGE returns the highest eligible drink price', () => {
    const coupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: {} }
    const items = [
      { ...drinkItem, unitPrice: 20 },
      { ...drinkItem, productId: 'p6', unitPrice: 35 }
    ]
    expect(calculateCouponDiscount(coupon, items, pricing)).toBe(35)
  })

  it('EXCHANGE cake coupon returns the highest bakery price', () => {
    const coupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: { scope: 'CAKE_ONLY' } }
    const items = [{ ...cakeItem, unitPrice: 20 }, { ...cakeItem, productId: 'p7', unitPrice: 45 }]
    expect(calculateCouponDiscount(coupon, items, pricing)).toBe(45)
  })

  it('EXCHANGE linked product uses that product price', () => {
    const coupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: { linkedProductId: 'p1' } }
    expect(calculateCouponDiscount(coupon, [drinkItem], pricing)).toBe(25)
  })

  it('returns 0 for an empty cart', () => {
    const coupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: {} }
    expect(calculateCouponDiscount(coupon, [], pricing)).toBe(0)
  })
})

// ──────────────────────────────────────────────
//  Main coupon enrichment & best coupon
// ──────────────────────────────────────────────

describe('filterMainCoupons & findBestCoupon', () => {
  const pricing = { baseSubtotal: 50, cupExtraTotal: 0, subtotal: 50 }
  const context = { hasExtraShot: false, diningMethod: 'TAKEOUT' }
  const items = [{ ...drinkItem, quantity: 1, unitPrice: 50 }]

  it('marks usable coupons as meetsThreshold and sorts by discount desc', () => {
    const coupons = [
      { couponCode: 'FR10', couponType: COUPON_TYPE.FULL_REDUCE, value: 10, minAmount: 30, parsedRule: { minOrderAmount: 30 } },
      { couponCode: 'D5', couponType: COUPON_TYPE.DISCOUNT, value: 20, parsedRule: {} }
    ]
    const result = filterMainCoupons(coupons, items, pricing, context)
    expect(result[0].couponCode).toBe('D5')
    expect(result[0].meetsThreshold).toBe(true)
    expect(result[1].couponCode).toBe('FR10')
    expect(result[1].meetsThreshold).toBe(true)
    expect(findBestCoupon(result)).toBe('D5')
  })

  it('sets amountNeeded when FULL_REDUCE threshold not met', () => {
    const coupons = [{ couponCode: 'FR20', couponType: COUPON_TYPE.FULL_REDUCE, value: 10, minAmount: 80, parsedRule: { minOrderAmount: 80 } }]
    const result = filterMainCoupons(coupons, items, pricing, context)
    expect(result[0].meetsThreshold).toBe(false)
    expect(result[0].amountNeeded).toBe('30')
    expect(result[0].unavailableReason).toContain('80')
  })

  it('invalid coupons are marked unavailable with reason', () => {
    const coupons = [{ couponCode: 'SHOT1', couponType: COUPON_TYPE.SHOT, parsedRule: {} }]
    const result = filterMainCoupons(coupons, items, pricing, context)
    expect(result[0].meetsThreshold).toBe(false)
    expect(result[0].estimatedDiscount).toBe(0)
  })

  it('findBestCoupon returns null when nothing is usable', () => {
    expect(findBestCoupon([])).toBe(null)
    const unusable = [{ couponCode: 'X', meetsThreshold: false }]
    expect(findBestCoupon(unusable)).toBe(null)
  })
})

// ──────────────────────────────────────────────
//  Addon coupon helpers
// ──────────────────────────────────────────────

describe('addon coupon helpers', () => {
  it('getAddonCouponDesc', () => {
    expect(getAddonCouponDesc({ couponType: COUPON_TYPE.DELIVERY_FEE }, 0)).toBe('抵扣配送费（限用1张）')
    expect(getAddonCouponDesc({ couponType: COUPON_TYPE.SHOT }, 2)).toBe('每张抵¥5（最多2张）')
    expect(getAddonCouponDesc({ couponType: COUPON_TYPE.SHOT }, 0)).toBe('免费加浓缩')
    expect(getAddonCouponDesc({ couponType: COUPON_TYPE.DISCOUNT }, 0)).toBe('')
  })

  it('getAddonCouponTip returns validation reason when invalid', () => {
    const coupon = { couponCode: 'S1', couponType: COUPON_TYPE.SHOT }
    const tip = getAddonCouponTip(coupon, [drinkItem], { hasExtraShot: false, extraShotCount: 0, selectedShotCouponCount: 0, selectedAddonCoupons: [], diningMethod: 'TAKEOUT' })
    expect(tip).toBe('需先添加加浓缩饮品')
  })

  it('getAddonCouponTip flags shot selection cap', () => {
    const coupon = { couponCode: 'S1', couponType: COUPON_TYPE.SHOT }
    const tip = getAddonCouponTip(coupon, [drinkItem], { hasExtraShot: true, extraShotCount: 1, selectedShotCouponCount: 1, selectedAddonCoupons: ['S2'], diningMethod: 'TAKEOUT' })
    expect(tip).toBe('已选1/1张')
  })

  it('isAddonCouponDisabled disables DELIVERY_FEE outside delivery', () => {
    const coupon = { couponCode: 'DF', couponType: COUPON_TYPE.DELIVERY_FEE }
    const ctx = { hasExtraShot: false, extraShotCount: 0, selectedShotCouponCount: 0, selectedAddonCoupons: [], diningMethod: 'TAKEOUT' }
    expect(isAddonCouponDisabled(coupon, [drinkItem], ctx)).toBe(true)
    expect(isAddonCouponDisabled(coupon, [drinkItem], { ...ctx, diningMethod: 'DELIVERY' })).toBe(false)
  })

  it('isAddonCouponDisabled disables SHOT without extra shot', () => {
    const coupon = { couponCode: 'S1', couponType: COUPON_TYPE.SHOT }
    const ctx = { hasExtraShot: false, extraShotCount: 0, selectedShotCouponCount: 0, selectedAddonCoupons: [], diningMethod: 'TAKEOUT' }
    expect(isAddonCouponDisabled(coupon, [drinkItem], ctx)).toBe(true)
    expect(isAddonCouponDisabled(coupon, [drinkItem], { ...ctx, hasExtraShot: true, extraShotCount: 1 })).toBe(false)
  })
})

// ──────────────────────────────────────────────
//  Per-item discount allocation
// ──────────────────────────────────────────────

describe('allocateItemDiscounts', () => {
  const pricing = { baseSubtotal: 40, cupExtraTotal: 0, subtotal: 40 }
  const items = [
    { ...drinkItem, quantity: 1, unitPrice: 30 },
    { ...drinkItem, productId: 'p8', quantity: 1, unitPrice: 20 }
  ]

  it('returns items unchanged without a coupon or zero discount', () => {
    const out = allocateItemDiscounts(items, null, 0, pricing)
    expect(out[0].discountAmount).toBe(0)
    expect(out[0].discountedPrice).toBe(30)
    const out2 = allocateItemDiscounts(items, { couponType: COUPON_TYPE.DISCOUNT, parsedRule: {} }, 0, pricing)
    expect(out2[0].discountAmount).toBe(0)
  })

  it('does not allocate SHOT / DELIVERY_FEE discounts to items', () => {
    const out = allocateItemDiscounts(items, { couponType: COUPON_TYPE.SHOT, parsedRule: {} }, 10, pricing)
    expect(out[0].discountAmount).toBe(0)
    const out2 = allocateItemDiscounts(items, { couponType: COUPON_TYPE.DELIVERY_FEE, parsedRule: {} }, 10, pricing)
    expect(out2[0].discountAmount).toBe(0)
  })

  it('EXCHANGE discounts the single most expensive eligible item', () => {
    const coupon = { couponType: COUPON_TYPE.EXCHANGE, parsedRule: {} }
    const out = allocateItemDiscounts(items, coupon, 10, pricing)
    const target = out.find(i => i.unitPrice === 30)
    expect(target.discountAmount).toBe(10)
    expect(target.discountedPrice).toBe(20)
    const cheap = out.find(i => i.unitPrice === 20)
    expect(cheap.discountAmount).toBe(0)
  })

  it('BOGO discounts the cheapest drink item', () => {
    const coupon = { couponType: COUPON_TYPE.BOGO, parsedRule: {} }
    const out = allocateItemDiscounts(items, coupon, 20, pricing)
    const target = out.find(i => i.unitPrice === 20)
    expect(target.discountAmount).toBe(20)
    expect(target.discountedPrice).toBe(0)
  })

  it('proportionally allocates a default discount across all items', () => {
    const coupon = { couponType: COUPON_TYPE.FULL_REDUCE, parsedRule: {} }
    const out = allocateItemDiscounts(items, coupon, 15, pricing)
    const total = out.reduce((s, i) => s + i.discountAmount * i.quantity, 0)
    expect(total).toBeCloseTo(15, 5)
    expect(out.every(i => i.discountAmount >= 0)).toBe(true)
  })

  it('DRINK_ONLY discount allocates across drinks only', () => {
    const coupon = { couponType: COUPON_TYPE.DISCOUNT, parsedRule: { scope: 'DRINK_ONLY' } }
    const mixed = [...items, { ...cakeItem, quantity: 1, unitPrice: 50 }]
    const out = allocateItemDiscounts(mixed, coupon, 15, pricing)
    const cake = out.find(i => i.category === 'cake')
    expect(cake.discountAmount).toBe(0)
    const drinkTotal = out.filter(i => i.category !== 'cake').reduce((s, i) => s + i.discountAmount * i.quantity, 0)
    expect(drinkTotal).toBeCloseTo(15, 5)
  })
})
