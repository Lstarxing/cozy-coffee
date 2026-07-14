import { describe, expect, it } from 'vitest'
import {
  AuthError,
  BusinessError,
  NetworkError,
  ValidationError,
  mapResponseToResult
} from './AppError'

describe('mapResponseToResult', () => {
  it('returns a successful backend envelope', () => {
    const payload = { success: true, code: 200, data: { id: 1 } }
    expect(mapResponseToResult({ statusCode: 200, data: payload })).toBe(payload)
  })

  it('maps auth failures', () => {
    expect(() => mapResponseToResult({ statusCode: 200, data: { code: 401, message: 'expired' } }))
      .toThrow(AuthError)
  })

  it('maps validation failures', () => {
    expect(() => mapResponseToResult({ statusCode: 422, data: { code: 'ITEM_OFFLINE', message: 'offline' } }))
      .toThrow(ValidationError)
  })

  it('maps business failures without resolving them as success', () => {
    expect(() => mapResponseToResult({ statusCode: 200, data: { code: 'STORE_CLOSED', message: 'closed' } }))
      .toThrow(BusinessError)
  })

  it('maps server failures to retryable network errors', () => {
    try {
      mapResponseToResult({ statusCode: 503, data: { message: 'down' } })
    } catch (error) {
      expect(error).toBeInstanceOf(NetworkError)
      expect(error.retryable).toBe(true)
    }
  })
})
