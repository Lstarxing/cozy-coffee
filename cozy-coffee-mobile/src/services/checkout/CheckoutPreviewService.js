import { computeCheckoutPreview } from '@/domain/checkout/computeCheckoutPreview'
import { BusinessError, ValidationError } from '@/services/errors/AppError'
import { Logger } from '@/services/logging/Logger'

const FALLBACK_CODES = new Set([404, '404', 'NOT_FOUND', 'PREVIEW_NOT_IMPLEMENTED', 'CHECKOUT_PREVIEW_UNAVAILABLE'])

function canUseLocalFallback(error) {
  return error instanceof BusinessError && (error.status === 404 || FALLBACK_CODES.has(error.code))
}

export class CheckoutPreviewService {
  constructor({ orderService, logger = Logger } = {}) {
    this.orderService = orderService
    this.logger = logger
  }

  async preview(context) {
    const local = computeCheckoutPreview(context)
    try {
      const result = await this.orderService.checkCart(context)
      if (result?.invalidItems?.length) {
        throw new ValidationError('购物车中有已失效商品，请重新选择', {
          code: 'CART_INVALID',
          details: result
        })
      }
      if (result?.changedItems?.length) {
        throw new ValidationError('商品价格或规格已变化，请确认后重新提交', {
          code: 'CART_CHANGED',
          details: result
        })
      }
      if (!result?.preview) {
        throw new BusinessError('结算预览响应不完整', { code: 'INVALID_PREVIEW_RESPONSE' })
      }

      return Object.freeze({
        subtotal: Number(result.preview.subtotal ?? local.subtotal),
        discount: Number(result.preview.discount ?? local.discount),
        payable: Number(result.preview.payable ?? local.payable),
        previewToken: result.preview.previewToken,
        previewVersion: result.preview.previewToken || local.previewVersion,
        expiresAt: result.preview.expiresAt || null,
        source: 'server'
      })
    } catch (error) {
      if (!canUseLocalFallback(error)) throw error
      this.logger.warn('Checkout Preview Local Fallback', { code: error.code, status: error.status })
      return Object.freeze({ ...local, source: 'local-fallback' })
    }
  }
}
