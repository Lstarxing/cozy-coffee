import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SessionService } from './SessionService'

describe('SessionService development WeChat session', () => {
  let storage

  beforeEach(() => {
    storage = new Map()
    vi.stubGlobal('uni', {
      login: ({ success }) => success({ code: 'wx-code' }),
      getStorageSync: key => storage.get(key) || '',
      setStorageSync: (key, value) => storage.set(key, value)
    })
  })

  it('reuses a stable development device id and stores the returned session', async () => {
    const setLoginInfo = vi.fn()
    const sessionStore = { isAuthenticated: false, setLoginInfo }
    const exchangeWechatSession = vi.fn().mockResolvedValue({ data: { token: 'token-1', user: { id: 7 } } })
    const service = new SessionService({
      sessionStore,
      authApi: { exchangeWechatSession, getCurrentSession: vi.fn() },
      logger: { info: vi.fn() }
    })

    await service.establishSilentSession()
    const firstDeviceId = exchangeWechatSession.mock.calls[0][1]
    await service.establishSilentSession()

    expect(firstDeviceId).toMatch(/^device_/)
    expect(exchangeWechatSession.mock.calls[1][1]).toBe(firstDeviceId)
    expect(setLoginInfo).toHaveBeenCalledWith('token-1', { id: 7 })
  })
})
