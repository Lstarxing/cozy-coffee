function defaultConfirmation(context) {
  return new Promise((resolve, reject) => {
    uni.showModal({
      title: '模拟支付',
      content: `开发模式：确认模拟支付 ¥${Number(context.amount || 0).toFixed(2)}？`,
      confirmText: '确认支付',
      cancelText: '取消',
      success: result => resolve(Boolean(result.confirm)),
      fail: reject
    })
  })
}

export class MockPaymentAdapter {
  constructor({ confirm = defaultConfirmation } = {}) {
    this.confirm = confirm
  }

  async prepare(context) {
    return { ...context, paymentMode: 'mock' }
  }

  async pay(context) {
    try {
      const confirmed = await this.confirm(context)
      if (!confirmed) return { status: 'cancelled' }
      return {
        status: 'success',
        transactionId: `mock-${Date.now()}`,
        adapterDebug: 'mock-payment'
      }
    } catch (error) {
      return { status: 'failed', error }
    }
  }
}
