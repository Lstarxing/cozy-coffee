import { describe, expect, it, vi } from 'vitest'
import { OrderService } from './OrderService'

describe('OrderService', () => {
  it('calls cart check with normalized lines and coupon code', async () => {
    const api = { checkCart: vi.fn().mockResolvedValue({ data: { preview: {} } }), createOrder: vi.fn(), getOrderDetail: vi.fn() }
    await new OrderService(api).checkCart({
      items: [{ productId: '1', quantity: 2, milkType: 'OAT' }],
      coupon: { code: 'WELCOME' },
      storeId: 1,
      pickupTime: 'ASAP'
    })
    expect(api.checkCart).toHaveBeenCalledWith(expect.objectContaining({
      couponCode: 'WELCOME',
      items: [expect.objectContaining({ productId: 1, quantity: 2 })]
    }))
  })

  it('sends the same Idempotency-Key when creating an order', async () => {
    const api = { checkCart: vi.fn(), createOrder: vi.fn().mockResolvedValue({ data: { id: 9 } }), getOrderDetail: vi.fn() }
    await new OrderService(api).create({ items: [{ productId: 1, quantity: 1 }], preview: { previewToken: 'p1' } }, 'checkout-key')
    expect(api.createOrder.mock.calls[0][1]).toEqual({ header: { 'Idempotency-Key': 'checkout-key' } })
    expect(api.createOrder.mock.calls[0][0].previewToken).toBe('p1')
  })

  it('passes main coupon + addon coupons to both check and create', async () => {
    const api = { checkCart: vi.fn().mockResolvedValue({ data: { preview: {} } }), createOrder: vi.fn().mockResolvedValue({ data: { id: 9 } }), getOrderDetail: vi.fn() }
    const context = {
      items: [{ productId: 1, quantity: 1 }],
      coupon: { couponCode: 'MAIN_5ZHE' },
      addonCouponCodes: ['SHOT_1', 'DELIVERY_FEE_1']
    }
    const svc = new OrderService(api)
    await svc.checkCart(context)
    await svc.create(context, 'key-1')

    expect(api.checkCart).toHaveBeenCalledWith(expect.objectContaining({
      couponCode: 'MAIN_5ZHE',
      addonCouponCodes: ['SHOT_1', 'DELIVERY_FEE_1']
    }))
    expect(api.createOrder.mock.calls[0][0]).toEqual(expect.objectContaining({
      couponCode: 'MAIN_5ZHE',
      addonCouponCodes: ['SHOT_1', 'DELIVERY_FEE_1']
    }))
  })
})
