import { describe, expect, it } from 'vitest'
import {
  AuthError,
  BusinessError,
  NetworkError,
  ValidationError,
  mapResponseToResult
} from './AppError'

describe('mapResponseToResult', () => {
  function capture(fn) {
    try {
      fn()
    } catch (error) {
      return error
    }
    throw new Error('expected fn to throw')
  }

  it('returns a successful backend envelope', () => {
    const payload = { success: true, code: 200, data: { id: 1 } }
    expect(mapResponseToResult({ statusCode: 200, data: payload })).toBe(payload)
  })

  it('maps auth failures', () => {
    expect(() => mapResponseToResult({ statusCode: 200, data: { code: 401, message: 'expired' } }))
      .toThrow(AuthError)
  })

  // 真实后端结构：业务失败恒为 code: 400（Result.fail），机器可读码放在 errorCode
  it('uses errorCode from the real envelope as the business code', () => {
    const error = capture(() => mapResponseToResult({
      statusCode: 200,
      data: { success: false, code: 400, errorCode: 'PREVIEW_EXPIRED', retryable: true, message: '预览已过期' }
    }))

    expect(error).toBeInstanceOf(BusinessError)
    expect(error.code).toBe('PREVIEW_EXPIRED')
    expect(error.retryable).toBe(true)
  })

  it('classifies as validation by errorCode, not by the numeric code', () => {
    const error = capture(() => mapResponseToResult({
      statusCode: 200,
      data: { success: false, code: 400, errorCode: 'ITEM_OFFLINE', retryable: false, message: '商品已下架' }
    }))

    expect(error).toBeInstanceOf(ValidationError)
    expect(error.code).toBe('ITEM_OFFLINE')
  })

  it('maps business failures without resolving them as success', () => {
    const error = capture(() => mapResponseToResult({
      statusCode: 200,
      data: { success: false, code: 400, errorCode: 'STORE_CLOSED', retryable: true, message: '门店已打烊' }
    }))

    expect(error).toBeInstanceOf(BusinessError)
    expect(error.code).toBe('STORE_CLOSED')
  })

  it('maps server failures to retryable network errors', () => {
    const error = capture(() => mapResponseToResult({ statusCode: 503, data: { message: 'down' } }))

    expect(error).toBeInstanceOf(NetworkError)
    expect(error.retryable).toBe(true)
  })
})
