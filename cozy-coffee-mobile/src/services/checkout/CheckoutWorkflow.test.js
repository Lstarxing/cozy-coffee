import { describe, expect, it, vi } from 'vitest'
import { transitionCheckout } from '@/domain/checkout/checkoutMachine'
import { AuthError, NetworkError } from '@/services/errors/AppError'
import { CheckoutWorkflow } from './CheckoutWorkflow'

function fixture(overrides = {}) {
  const checkoutStore = {
    storeId: 1, pickupTime: 'ASAP', selectedCouponId: null, remark: '', status: 'idle', idempotencyKey: '', latestPreview: null,
    get isPreviewStale() { return !this.latestPreview },
    start() { if (!this.idempotencyKey) this.idempotencyKey = 'same-key' },
    transition(event) { this.status = transitionCheckout(this.status, event) },
    applyPreview(preview) { this.latestPreview = preview; if (this.status === 'previewing') this.transition('PREVIEW_SUCCEEDED') },
    reset() { this.status = 'idle'; this.latestPreview = null; this.idempotencyKey = '' }
  }
  const cartStore = { items: [{ productId: 1, lineKey: 'v1:1', price: 20, quantity: 1 }], clearCart: vi.fn() }
  const networkService = { ensureOnline: vi.fn().mockResolvedValue(true) }
  const sessionService = { ensureCheckoutIdentity: vi.fn().mockResolvedValue(true), clearOnAuthFailure: vi.fn() }
  const previewService = { preview: vi.fn().mockResolvedValue({ subtotal: 20, discount: 0, payable: 20, previewVersion: 'p1' }) }
  const orderService = { create: vi.fn().mockResolvedValue({ id: 9, payAmount: 20 }), accept: vi.fn().mockResolvedValue({ id: 9, status: 'preparing' }) }
  const paymentService = { pay: vi.fn().mockResolvedValue({ status: 'success', transactionId: 't1' }) }
  const logger = { info: vi.fn(), warn: vi.fn(), error: vi.fn() }
  const dependencies = { cartStore, checkoutStore, networkService, sessionService, previewService, orderService, paymentService, logger, platform: () => 'test', ...overrides }
  return { workflow: new CheckoutWorkflow(dependencies), ...dependencies }
}

describe('CheckoutWorkflow', () => {
  it('completes preview, order creation and payment', async () => {
    const { workflow, checkoutStore, cartStore } = fixture()
    const result = await workflow.submit()
    expect(result.status).toBe('success')
    expect(checkoutStore.status).toBe('success')
    expect(cartStore.clearCart).toHaveBeenCalledOnce()
  })

  it('deduplicates ten rapid submits with one idempotency key', async () => {
    const { workflow, orderService } = fixture()
    const results = await Promise.all(Array.from({ length: 10 }, () => workflow.submit()))
    expect(orderService.create).toHaveBeenCalledOnce()
    expect(orderService.create.mock.calls[0][1]).toBe('same-key')
    expect(results.every(result => result.status === 'success')).toBe(true)
  })

  it('clears the cart when mock payment is cancelled (order already created)', async () => {
    const paymentService = { pay: vi.fn().mockResolvedValue({ status: 'cancelled', transactionId: null }) }
    const { workflow, checkoutStore, cartStore } = fixture({ paymentService })
    expect((await workflow.submit()).status).toBe('cancelled')
    expect(checkoutStore.status).toBe('cancelled')
    expect(cartStore.clearCart).toHaveBeenCalledOnce()
  })

  it('moves to offline when the network is unavailable', async () => {
    const networkService = { ensureOnline: vi.fn().mockRejectedValue(new NetworkError('offline', { code: 'OFFLINE' })) }
    const { workflow, checkoutStore } = fixture({ networkService })
    await expect(workflow.submit()).rejects.toBeInstanceOf(NetworkError)
    expect(checkoutStore.status).toBe('offline')
  })

  it('preserves intent and recovers after authentication', async () => {
    const sessionService = {
      ensureCheckoutIdentity: vi.fn().mockRejectedValueOnce(new AuthError('login', { code: 'AUTH_REQUIRED' })).mockResolvedValue(true),
      clearOnAuthFailure: vi.fn()
    }
    const { workflow, checkoutStore } = fixture({ sessionService })
    await expect(workflow.submit()).rejects.toBeInstanceOf(AuthError)
    expect(checkoutStore.status).toBe('awaiting_auth')
    const key = checkoutStore.idempotencyKey
    expect((await workflow.recover()).status).toBe('success')
    expect(checkoutStore.idempotencyKey).toBe(key)
  })
})
