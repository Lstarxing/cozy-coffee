import { BusinessError, ValidationError } from '@/services/errors/AppError'

export class PaymentService {
  constructor(adapter) {
    this.adapter = adapter
  }

  async pay(context) {
    if (!this.adapter?.pay) {
      throw new ValidationError('支付适配器未配置', { code: 'PAYMENT_ADAPTER_MISSING' })
    }
    const prepared = this.adapter.prepare ? await this.adapter.prepare(context) : context
    const result = await this.adapter.pay(prepared)
    if (result?.status === 'failed') {
      throw new BusinessError('模拟支付失败，请重试', {
        code: 'PAYMENT_FAILED',
        retryable: true,
        cause: result.error
      })
    }
    if (!['success', 'cancelled'].includes(result?.status)) {
      throw new ValidationError('支付适配器返回了未知状态', { code: 'INVALID_PAYMENT_RESULT' })
    }
    return {
      status: result.status,
      transactionId: result.status === 'success' ? (result.transactionId || null) : null
    }
  }
}
