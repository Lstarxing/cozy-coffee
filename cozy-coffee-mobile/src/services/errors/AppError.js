const VALIDATION_CODES = new Set([
  400,
  422,
  'VALIDATION_ERROR',
  'INVALID_ARGUMENT',
  'CART_INVALID',
  'ITEM_OFFLINE',
  'ITEM_CHANGED'
])

export class AppError extends Error {
  constructor(message, options = {}) {
    super(message || '操作失败')
    this.name = new.target.name
    this.code = options.code ?? 'UNKNOWN_ERROR'
    this.status = options.status ?? null
    this.retryable = Boolean(options.retryable)
    this.details = options.details ?? null
    this.cause = options.cause
  }
}

export class NetworkError extends AppError {}
export class BusinessError extends AppError {}
export class AuthError extends AppError {}
export class ValidationError extends AppError {}

function errorOptions(payload, status) {
  return {
    code: payload?.code ?? status ?? 'BUSINESS_ERROR',
    status,
    retryable: Boolean(payload?.retryable),
    details: payload?.data ?? payload?.details ?? null
  }
}

export function mapResponseToResult(response) {
  const status = Number(response?.statusCode ?? response?.status ?? 0)
  const payload = response?.data

  if (status === 401 || status === 403 || payload?.code === 401 || payload?.code === 403) {
    throw new AuthError(payload?.message || payload?.msg || '登录状态已失效', errorOptions(payload, status))
  }

  if (status >= 500 || status === 0) {
    throw new NetworkError(payload?.message || payload?.msg || '服务暂时不可用，请稍后重试', {
      ...errorOptions(payload, status),
      code: payload?.code || `HTTP_${status || 'ERROR'}`,
      retryable: true
    })
  }

  const isEnvelope = payload && typeof payload === 'object' && (
    Object.prototype.hasOwnProperty.call(payload, 'success') ||
    Object.prototype.hasOwnProperty.call(payload, 'code')
  )

  if (!isEnvelope && status >= 200 && status < 300) return payload

  const success = status >= 200 && status < 300 && (
    payload?.success === true || payload?.code === 200 || payload?.code === 1
  )

  if (success) return payload

  const message = payload?.message || payload?.msg || '请求失败'
  const options = errorOptions(payload, status)
  if (status === 422 || VALIDATION_CODES.has(payload?.code)) {
    throw new ValidationError(message, options)
  }

  throw new BusinessError(message, options)
}
