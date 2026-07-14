import { describe, expect, it } from 'vitest'
import { BusinessError } from '@/services/errors/AppError'
import { PaymentService } from './PaymentService'
import { MockPaymentAdapter } from './adapters/MockPaymentAdapter'

describe('PaymentService', () => {
  it('returns a normalized success without adapter-specific fields', async () => {
    const service = new PaymentService(new MockPaymentAdapter({ confirm: async () => true }))
    const result = await service.pay({ amount: 20 })
    expect(result.status).toBe('success')
    expect(result).not.toHaveProperty('adapterDebug')
  })

  it('keeps cancellation as a normal result', async () => {
    const service = new PaymentService(new MockPaymentAdapter({ confirm: async () => false }))
    await expect(service.pay({ amount: 20 })).resolves.toEqual({ status: 'cancelled', transactionId: null })
  })

  it('maps adapter failures to a retryable business error', async () => {
    const service = new PaymentService(new MockPaymentAdapter({ confirm: async () => { throw new Error('boom') } }))
    await expect(service.pay({ amount: 20 })).rejects.toBeInstanceOf(BusinessError)
  })
})
