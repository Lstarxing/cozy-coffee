import { describe, expect, it, vi } from 'vitest'
import { BusinessError, ValidationError } from '@/services/errors/AppError'
import { CheckoutPreviewService } from './CheckoutPreviewService'

const context = { items: [{ productId: 1, lineKey: 'v1:1', price: 20, quantity: 1 }] }

describe('CheckoutPreviewService', () => {
  it('uses an explicit local fallback only when the endpoint is unavailable', async () => {
    const orderService = { checkCart: vi.fn().mockRejectedValue(new BusinessError('missing', { code: 404, status: 404 })) }
    const result = await new CheckoutPreviewService({ orderService, logger: { warn: vi.fn() } }).preview(context)
    expect(result.source).toBe('local-fallback')
  })

  it('never turns a business rejection into a successful preview', async () => {
    const error = new BusinessError('store closed', { code: 'STORE_CLOSED' })
    const orderService = { checkCart: vi.fn().mockRejectedValue(error) }
    await expect(new CheckoutPreviewService({ orderService }).preview(context)).rejects.toBe(error)
  })

  it('rejects invalid cart items returned by the server', async () => {
    const orderService = { checkCart: vi.fn().mockResolvedValue({ invalidItems: [1], changedItems: [], preview: {} }) }
    await expect(new CheckoutPreviewService({ orderService }).preview(context)).rejects.toBeInstanceOf(ValidationError)
  })

  it('uses the server preview discount and passes through couponDetails', async () => {
    const orderService = {
      checkCart: vi.fn().mockResolvedValue({
        invalidItems: [], changedItems: [],
        preview: {
          subtotal: 20, discount: 10, payable: 10,
          couponDetails: [{ title: '5折券', discount: 10, main: true }]
        }
      })
    }
    const result = await new CheckoutPreviewService({ orderService }).preview({ ...context, diningMethod: 'DELIVERY' })
    // 金额口径由后端 preview 统一返回（payable 已含配送费/配送券抵扣），前端不再本地拼实付
    expect(result).toMatchObject({ subtotal: 20, discount: 10, payable: 10, source: 'server' })
    expect(result.couponDetails).toHaveLength(1)
    expect(result.couponDetails[0].title).toBe('5折券')
  })

  it('uses server deliveryFee and payable that already include delivery coupon deduction', async () => {
    const orderService = {
      checkCart: vi.fn().mockResolvedValue({
        invalidItems: [], changedItems: [],
        preview: {
          subtotal: 20, discount: 13, deliveryFee: 3, payable: 10,
          couponDetails: [{ title: '配送费抵扣券', discount: 3, main: false }]
        }
      })
    }
    const result = await new CheckoutPreviewService({ orderService }).preview({ ...context, diningMethod: 'DELIVERY' })
    expect(result).toMatchObject({ subtotal: 20, discount: 13, deliveryFee: 3, payable: 10, source: 'server' })
  })

  it('fallback computes DISCOUNT coupon as percentage (5折), not flat value', async () => {
    const orderService = { checkCart: vi.fn().mockRejectedValue(new BusinessError('missing', { code: 404, status: 404 })) }
    const withCoupon = { ...context, coupon: { id: 9, couponType: 'DISCOUNT', value: 50 } }
    const result = await new CheckoutPreviewService({ orderService, logger: { warn: vi.fn() } }).preview(withCoupon)
    expect(result.source).toBe('local-fallback')
    expect(result.discount).toBe(10) // 20 元 5 折
    expect(result.payable).toBe(10)
  })

  it('fallback deducts SHOT addon coupon from payable', async () => {
    const orderService = { checkCart: vi.fn().mockRejectedValue(new BusinessError('missing', { code: 404, status: 404 })) }
    const withAddon = { ...context, addonCoupons: [{ id: 20, couponType: 'SHOT', value: 5 }] }
    const result = await new CheckoutPreviewService({ orderService, logger: { warn: vi.fn() } }).preview(withAddon)
    expect(result.source).toBe('local-fallback')
    expect(result.discount).toBe(5)
    expect(result.payable).toBe(15)
  })
})
