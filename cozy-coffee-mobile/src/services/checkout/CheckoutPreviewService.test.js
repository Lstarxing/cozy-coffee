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
})
