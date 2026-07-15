import { exchangeWechatSession, getCurrentSession } from '@/api/auth'
import { AuthError } from '@/services/errors/AppError'
import { Logger } from '@/services/logging/Logger'

function requestWechatCode() {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: result => result.code ? resolve(result.code) : reject(new AuthError('微信登录未返回有效凭证', { code: 'WECHAT_CODE_MISSING' })),
      fail: error => reject(new AuthError('无法建立微信会话', { code: 'WECHAT_LOGIN_FAILED', cause: error }))
    })
  })
}

function getDevDeviceId() {
  let deviceId = uni.getStorageSync('cozy_wechat_dev_device_id')
  if (!deviceId) {
    deviceId = `device_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`
    uni.setStorageSync('cozy_wechat_dev_device_id', deviceId)
  }
  return deviceId
}

export class SessionService {
  constructor({ sessionStore, authApi = { exchangeWechatSession, getCurrentSession }, logger = Logger } = {}) {
    this.sessionStore = sessionStore
    this.authApi = authApi
    this.logger = logger
  }

  restore() {
    return this.sessionStore.restore()
  }

  async validate() {
    if (!this.sessionStore.token) return false
    try {
      await this.authApi.getCurrentSession()
      return true
    } catch (error) {
      if (error instanceof AuthError) this.sessionStore.clearSession()
      throw error
    }
  }

  async establishSilentSession() {
    if (this.sessionStore.isAuthenticated) return true

    let code = null
    // #ifdef MP-WEIXIN
    code = await requestWechatCode()
    // #endif

    if (!code) return false
    const response = await this.authApi.exchangeWechatSession(code, getDevDeviceId())
    const data = response?.data || response
    if (!data?.token) throw new AuthError('后端未返回登录凭证', { code: 'SESSION_TOKEN_MISSING' })
    this.sessionStore.setLoginInfo(data.token, data.user || data.userInfo || {})
    this.logger.info('Session Restored', { userId: data.user?.id || data.userInfo?.id, platform: 'mp-weixin' })
    return true
  }

  async ensureCheckoutIdentity() {
    this.restore()
    if (this.sessionStore.isAuthenticated) return true
    if (await this.establishSilentSession()) return true
    throw new AuthError('结算前请先登录', { code: 'AUTH_REQUIRED', retryable: true })
  }

  clearOnAuthFailure() {
    this.sessionStore.clearSession()
  }
}
