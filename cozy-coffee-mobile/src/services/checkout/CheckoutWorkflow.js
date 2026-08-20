import { useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { useSessionStore } from '@/stores/session'
import { AuthError, NetworkError } from '@/services/errors/AppError'
import { Logger } from '@/services/logging/Logger'
import { SessionService } from '@/services/session/SessionService'
import { NetworkService } from '@/services/network/NetworkService'
import { OrderService } from '@/services/order/OrderService'
import { PaymentService } from '@/services/payment/PaymentService'
import { MockPaymentAdapter } from '@/services/payment/adapters/MockPaymentAdapter'
import { CheckoutPreviewService } from './CheckoutPreviewService'

function createTraceId() {
  return `trace-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`
}

function detectPlatform() {
  try {
    const info = uni.getSystemInfoSync()
    return info.uniPlatform || info.platform || 'unknown'
  } catch (_) {
    return 'unknown'
  }
}

export class CheckoutWorkflow {
  constructor({ cartStore, checkoutStore, sessionService, networkService, previewService, orderService, paymentService, logger = Logger, platform = detectPlatform }) {
    this.cartStore = cartStore
    this.checkoutStore = checkoutStore
    this.sessionService = sessionService
    this.networkService = networkService
    this.previewService = previewService
    this.orderService = orderService
    this.paymentService = paymentService
    this.logger = logger
    this.platform = platform
    this.submitPromise = null
    this.previewPromise = null
    this.lastResult = null
    this.lastOverrides = {}
  }

  context(overrides = {}) {
    return {
      items: this.cartStore.items,
      storeId: this.checkoutStore.storeId,
      pickupTime: this.checkoutStore.pickupTime,
      diningMethod: this.checkoutStore.diningMethod,
      deliveryAddressId: this.checkoutStore.deliveryAddressId,
      deliveryAddress: this.checkoutStore.deliveryAddress || null,
      selectedCouponId: this.checkoutStore.selectedCouponId,
      remark: this.checkoutStore.remark,
      phone: this.checkoutStore.phone,
      ...overrides
    }
  }

  log(level, event, startedAt, extra = {}) {
    this.logger[level](event, {
      traceId: extra.traceId,
      idempotencyKey: this.checkoutStore.idempotencyKey,
      previewVersion: this.checkoutStore.latestPreview?.previewVersion,
      stage: this.checkoutStore.status,
      duration: Date.now() - startedAt,
      platform: this.platform(),
      ...extra
    })
  }

  async ensureOnline(traceId, startedAt) {
    try {
      await this.networkService.ensureOnline()
      if (this.checkoutStore.status === 'offline') this.checkoutStore.transition('NETWORK_RESTORED')
    } catch (error) {
      if (error instanceof NetworkError && this.checkoutStore.status !== 'offline') this.checkoutStore.transition('OFFLINE')
      this.log('warn', 'Checkout Offline', startedAt, { traceId, errorCode: error.code })
      throw error
    }
  }

  async preview(overrides = {}) {
    if (this.previewPromise) return this.previewPromise
    this.lastOverrides = { ...this.lastOverrides, ...overrides }
    const traceId = createTraceId()
    const startedAt = Date.now()

    this.previewPromise = (async () => {
      await this.ensureOnline(traceId, startedAt)
      if (this.checkoutStore.status === 'success') this.checkoutStore.reset({ preserveIntent: true })
      if (this.checkoutStore.status === 'awaiting_auth') {
        await this.sessionService.ensureCheckoutIdentity()
        this.checkoutStore.transition('AUTH_RESTORED')
      } else {
        this.checkoutStore.transition('START_PREVIEW')
      }
      this.log('info', 'Checkout Start', startedAt, { traceId })
      try {
        const result = await this.previewService.preview(this.context(this.lastOverrides))
        this.checkoutStore.applyPreview(result)
        this.log('info', 'Preview Success', startedAt, { traceId, source: result.source })
        return result
      } catch (error) {
        if (error instanceof AuthError) this.checkoutStore.transition('AUTH_REQUIRED')
        else if (error instanceof NetworkError && error.code === 'OFFLINE') this.checkoutStore.transition('OFFLINE')
        else this.checkoutStore.transition('PREVIEW_FAILED')
        this.log('error', 'Preview Failed', startedAt, { traceId, errorCode: error.code })
        throw error
      }
    })()

    try {
      return await this.previewPromise
    } finally {
      this.previewPromise = null
    }
  }

  submit(overrides = {}) {
    if (this.submitPromise) return this.submitPromise
    if (this.checkoutStore.status === 'success' && this.lastResult) return Promise.resolve(this.lastResult)
    this.lastOverrides = { ...this.lastOverrides, ...overrides }
    this.submitPromise = this.performSubmit()
    this.submitPromise.then(
      () => { this.submitPromise = null },
      () => { this.submitPromise = null }
    )
    return this.submitPromise
  }

  async performSubmit() {
    const traceId = createTraceId()
    const startedAt = Date.now()
    this.checkoutStore.start()
    await this.ensureOnline(traceId, startedAt)

    if (!this.checkoutStore.latestPreview || this.checkoutStore.isPreviewStale || this.checkoutStore.status !== 'ready') {
      await this.preview(this.lastOverrides)
    }

    try {
      await this.sessionService.ensureCheckoutIdentity()
    } catch (error) {
      if (error instanceof AuthError && this.checkoutStore.status === 'ready') this.checkoutStore.transition('AUTH_REQUIRED')
      this.log('warn', 'Checkout Auth Required', startedAt, { traceId, errorCode: error.code })
      throw error
    }

    if (this.checkoutStore.status === 'awaiting_auth') this.checkoutStore.transition('AUTH_RESTORED')
    if (this.checkoutStore.status === 'previewing') {
      const refreshed = await this.previewService.preview(this.context(this.lastOverrides))
      this.checkoutStore.applyPreview(refreshed)
    } else {
      await this.preview(this.lastOverrides)
    }

    this.checkoutStore.transition('SUBMIT')
    const context = { ...this.context(this.lastOverrides), preview: this.checkoutStore.latestPreview }

    try {
      this.log('info', 'Create Order', startedAt, { traceId })
      const order = await this.orderService.create(context, this.checkoutStore.idempotencyKey)
      this.checkoutStore.transition('ORDER_CREATED')
      // 下单即视为购物车已提交：无论支付成功或取消，购物车都应清空（商品已进入订单）
      this.cartStore.clearCart()
      const payment = await this.paymentService.pay({ order, orderId: order?.id, amount: order?.payAmount ?? this.checkoutStore.latestPreview.payable })

      if (payment.status === 'cancelled') {
        this.checkoutStore.transition('PAYMENT_CANCELLED')
        this.log('warn', 'Payment Cancel', startedAt, { traceId, orderId: order?.id })
        return { status: 'cancelled', order, payment }
      }

      this.checkoutStore.transition('PAYMENT_SUCCEEDED')
      // 支付成功后自动接单：待支付 pending → 制作中 preparing
      if (order?.id) {
        this.orderService.accept(order.id).catch(error => {
          this.log('warn', 'Auto Accept Failed', startedAt, { traceId, orderId: order?.id, error: error?.message })
        })
      }
      this.lastResult = { status: 'success', order, payment }
      this.log('info', 'Payment Success', startedAt, { traceId, orderId: order?.id })
      return this.lastResult
    } catch (error) {
      if (error instanceof AuthError) {
        this.sessionService.clearOnAuthFailure?.()
        if (this.checkoutStore.status === 'submitting') this.checkoutStore.transition('AUTH_REQUIRED')
      } else if (error instanceof NetworkError) {
        if (this.checkoutStore.status !== 'offline') this.checkoutStore.transition('OFFLINE')
      } else if (this.checkoutStore.status === 'submitting') {
        this.checkoutStore.transition('SUBMIT_FAILED')
      } else if (this.checkoutStore.status === 'paying') {
        this.checkoutStore.transition('PAYMENT_FAILED')
      }
      this.log('error', 'Checkout Failed', startedAt, { traceId, errorCode: error.code })
      throw error
    }
  }

  async recover(overrides = {}) {
    this.lastOverrides = { ...this.lastOverrides, ...overrides }
    if (this.checkoutStore.status === 'offline') {
      await this.networkService.ensureOnline()
      this.checkoutStore.transition('NETWORK_RESTORED')
    }
    if (this.checkoutStore.status === 'awaiting_auth') {
      await this.sessionService.ensureCheckoutIdentity()
      this.checkoutStore.transition('AUTH_RESTORED')
      const refreshed = await this.previewService.preview(this.context(this.lastOverrides))
      this.checkoutStore.applyPreview(refreshed)
    }
    if (['failed', 'cancelled'].includes(this.checkoutStore.status)) this.checkoutStore.transition('RETRY')
    return this.submit(this.lastOverrides)
  }
}

export function createDefaultCheckoutWorkflow() {
  const cartStore = useCartStore()
  const checkoutStore = useCheckoutStore()
  const sessionStore = useSessionStore()
  const orderService = new OrderService()
  return new CheckoutWorkflow({
    cartStore,
    checkoutStore,
    sessionService: new SessionService({ sessionStore }),
    networkService: new NetworkService(globalThis.uni),
    previewService: new CheckoutPreviewService({ orderService }),
    orderService,
    paymentService: new PaymentService(new MockPaymentAdapter())
  })
}
