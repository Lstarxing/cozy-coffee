import { describe, expect, it } from 'vitest'
import { computeCheckoutPreview } from './computeCheckoutPreview'

describe('computeCheckoutPreview', () => {
  const items = [
    { lineKey: 'v1:a', productId: 1, price: 20, quantity: 2 },
    { lineKey: 'v1:b', productId: 2, price: 15, quantity: 1 }
  ]

  it('calculates subtotal, fixed discount and non-negative payable', () => {
    const result = computeCheckoutPreview({ items, coupon: { id: 8, value: 10 } })
    expect(result).toMatchObject({ subtotal: 55, discount: 10, payable: 45 })
    expect(computeCheckoutPreview({ items, coupon: { value: 999 } }).payable).toBe(0)
  })

  it('treats DISCOUNT coupon value as percentage (5折 = half off), not flat amount', () => {
    // 小计 55，5 折 → 折扣 27.5，实付 27.5（不得按 value=50 抵 50）
    const result = computeCheckoutPreview({ items, coupon: { id: 9, couponType: 'DISCOUNT', value: 50 } })
    expect(result.discount).toBe(27.5)
    expect(result.payable).toBe(27.5)
  })

  it('deducts addon coupons (SHOT 加浓缩券) from payable', () => {
    // 小计 55 + SHOT 5 → 折扣 5，实付 50；两张 SHOT → 10
    const shot = { id: 20, couponType: 'SHOT', value: 5 }
    expect(computeCheckoutPreview({ items, addonCoupons: [shot] }).payable).toBe(50)
    expect(computeCheckoutPreview({ items, addonCoupons: [shot, { ...shot, id: 21 }] }).payable).toBe(45)
  })

  it('adds delivery fee into payable and invalidates the version', () => {
    const base = computeCheckoutPreview({ items })
    const withFee = computeCheckoutPreview({ items, deliveryFee: 3 })
    expect(withFee).toMatchObject({ subtotal: 55, deliveryFee: 3, payable: 58 })
    expect(base.previewVersion).not.toBe(withFee.previewVersion)
  })

  it('does not add a second large-size surcharge', () => {
    expect(computeCheckoutPreview({ items: [{ price: 23, quantity: 1, cupSize: 'LARGE' }] }).subtotal).toBe(23)
  })

  it('keeps a stable version and invalidates it on quantity or coupon changes', () => {
    const first = computeCheckoutPreview({ items, coupon: { id: 8, value: 10 } })
    const same = computeCheckoutPreview({ items: [...items].reverse(), coupon: { id: 8, value: 10 } })
    const quantityChanged = computeCheckoutPreview({ items: [{ ...items[0], quantity: 3 }, items[1]], coupon: { id: 8, value: 10 } })
    const couponChanged = computeCheckoutPreview({ items, coupon: { id: 9, value: 10 } })
    expect(first.previewVersion).toBe(same.previewVersion)
    expect(first.previewVersion).not.toBe(quantityChanged.previewVersion)
    expect(first.previewVersion).not.toBe(couponChanged.previewVersion)
  })
})
