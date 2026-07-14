import {
  AuthError,
  BusinessError,
  NetworkError,
  ValidationError,
  mapResponseToResult
} from '@/services/errors/AppError'

function trimTrailingSlash(value) {
  return String(value || '').trim().replace(/\/$/, '')
}

function resolveBaseURL() {
  let baseURL = ''

  // #ifdef H5
  baseURL = import.meta.env.VITE_H5_API_BASE_URL || '/api'
  // #endif

  // #ifndef H5
  baseURL = import.meta.env.VITE_API_BASE_URL
  if (!baseURL) {
    throw new Error('VITE_API_BASE_URL is required for non-H5 builds')
  }
  // #endif

  return trimTrailingSlash(baseURL)
}

export const config = {
  baseURL: resolveBaseURL(),
  timeout: Number(import.meta.env.VITE_API_TIMEOUT || 10000)
}

function getStoredToken() {
  try {
    return uni.getStorageSync('token') || ''
  } catch (_) {
    return ''
  }
}

function normalizeRequestFailure(error) {
  if (error instanceof AuthError || error instanceof BusinessError || error instanceof ValidationError || error instanceof NetworkError) {
    return error
  }

  return new NetworkError(error?.errMsg || error?.message || '网络连接失败，请稍后重试', {
    code: error?.code || 'NETWORK_UNAVAILABLE',
    cause: error,
    retryable: true
  })
}

export const request = (options) => new Promise((resolve, reject) => {
  const token = getStoredToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.header || options.headers || {})
  }

  if (options.showLoading) {
    uni.showLoading({ title: options.loadingText || '加载中...', mask: true })
  }

  uni.request({
    url: `${config.baseURL}${options.url}`,
    method: options.method || 'GET',
    data: options.data || {},
    timeout: options.timeout || config.timeout,
    header: headers,
    success: (response) => {
      try {
        resolve(mapResponseToResult(response))
      } catch (error) {
        reject(error)
      }
    },
    fail: (error) => reject(normalizeRequestFailure(error)),
    complete: () => {
      if (options.showLoading) uni.hideLoading()
    }
  })
})

export const get = (url, data, options = {}) => request({ url, method: 'GET', data, ...options })
export const post = (url, data, options = {}) => request({ url, method: 'POST', data, ...options })
export const put = (url, data, options = {}) => request({ url, method: 'PUT', data, ...options })
export const del = (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options })

export default { request, get, post, put, del, config }
