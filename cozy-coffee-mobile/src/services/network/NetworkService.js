import { NetworkError } from '@/services/errors/AppError'

export class NetworkService {
  constructor(uniApi = globalThis.uni) {
    this.uniApi = uniApi
    this.online = true
    this.networkType = 'unknown'
    this.listeners = new Set()
    this.boundHandler = result => this.update(result)
  }

  update(result = {}) {
    this.networkType = result.networkType || this.networkType
    this.online = typeof result.isConnected === 'boolean'
      ? result.isConnected
      : this.networkType !== 'none'
    this.listeners.forEach(listener => listener(this.snapshot()))
    return this.snapshot()
  }

  snapshot() {
    return { online: this.online, networkType: this.networkType }
  }

  refresh() {
    if (!this.uniApi?.getNetworkType) return Promise.resolve(this.snapshot())
    return new Promise(resolve => {
      this.uniApi.getNetworkType({
        success: result => resolve(this.update(result)),
        fail: () => resolve(this.snapshot())
      })
    })
  }

  async start() {
    await this.refresh()
    this.uniApi?.onNetworkStatusChange?.(this.boundHandler)
    return this.snapshot()
  }

  stop() {
    this.uniApi?.offNetworkStatusChange?.(this.boundHandler)
  }

  subscribe(listener) {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  async ensureOnline() {
    await this.refresh()
    if (!this.online) {
      throw new NetworkError('当前网络不可用，重新连接后可继续提交', {
        code: 'OFFLINE',
        retryable: true
      })
    }
    return true
  }
}
