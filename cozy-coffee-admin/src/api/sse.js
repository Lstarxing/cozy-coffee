/**
 * SSE 服务 - 管理与服务器的实时连接
 */
import api from './index'

class SseService {
    constructor() {
        this.eventSource = null
        this.connected = false
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 5
        this.reconnectDelay = 3000
        this.listeners = new Map() // eventType -> Set<callback>
        this.isConnecting = false // 连接锁
    }

    /**
     * 连接 SSE
     */
    async connect() {
        // 锁检查：正在连接中或已有连接则跳过
        if (this.isConnecting) {
            return
        }
        if (this.eventSource) {
            return
        }

        this.isConnecting = true;
        console.log('[SSE] 准备建立新连接...')

        // ========================================
        // 预检查：验证 Token 是否存在
        // ========================================
        const token = localStorage.getItem('adminToken')
        if (!token) {
            console.warn('[SSE] 未找到 adminToken，无法建立连接')
            this.isConnecting = false
            return
        }

        // 简单验证 Token 格式（JWT应该是 3 部分）
        const parts = token.split('.')
        if (parts.length !== 3) {
            console.warn('[SSE] Token 格式无效，清除并跳过连接')
            this._handleAuthFailure()
            this.isConnecting = false
            return
        }

        try {
            // 1. 获取 Ticket
            console.log('[SSE] 开始获取 Ticket...')
            const res = await api.post('/admin/sse/ticket')
            const ticket = res.data?.ticket

            if (!ticket) {
                console.error('[SSE] 获取 Ticket 失败：服务器未返回 ticket')
                this.isConnecting = false
                return
            }

            console.log('[SSE] Ticket 获取成功，建立 EventSource 连接...')

            // 2. 建立 SSE 连接（使用 Ticket，无需 Header）
            const sseBase = (import.meta.env.VITE_API_BASE_URL || '') + '/api'
            const url = `${sseBase}/admin/sse/events?ticket=${ticket}`
            const currentSource = new EventSource(url)
            this.eventSource = currentSource

            // 连接成功
            currentSource.addEventListener('connected', (event) => {
                if (this.eventSource !== currentSource) return
                console.log('[SSE] ✅ 连接成功', event.data)
                this.connected = true
                this.reconnectAttempts = 0
                this.isConnecting = false
            })

            // 心跳
            currentSource.addEventListener('heartbeat', () => {
                if (this.eventSource !== currentSource) return
                // 静默处理心跳
            })

            // 新订单事件
            currentSource.addEventListener('new_order', (event) => {
                if (this.eventSource !== currentSource) return
                console.log('[SSE] 收到新订单事件', event.data)
                this._emit('new_order', JSON.parse(event.data))
            })

            // 新兑换订单事件
            currentSource.addEventListener('new_redemption', (event) => {
                if (this.eventSource !== currentSource) return
                console.log('[SSE] 收到新兑换订单事件', event.data)
                this._emit('new_redemption', JSON.parse(event.data))
            })

            // 业务错误事件（服务器主动推送）
            currentSource.addEventListener('error', (event) => {
                if (this.eventSource !== currentSource) return
                console.warn('[SSE] 收到业务错误推送', event.data)
                this.disconnect()
            })

            // 传输层连接错误
            currentSource.onerror = (error) => {
                if (this.eventSource !== currentSource) return
                console.error('[SSE] 传输层连接错误', error)
                this.connected = false
                this.isConnecting = false
                this._handleReconnect()
            }

        } catch (error) {
            this.isConnecting = false
            console.error('[SSE] 获取 Ticket 或建立连接失败', error)

            // ========================================
            // 认证错误处理：自动登出
            // ========================================
            if (this._isAuthError(error)) {
                console.error('[SSE] ❌ 认证失败，执行自动登出')
                this._handleAuthFailure()
                return
            }

            // 其他错误：尝试重连
            this._handleReconnect()
        }
    }

    /**
     * 判断是否为认证错误
     */
    _isAuthError(error) {
        if (!error) return false

        const message = error.message || error.toString()
        const authKeywords = ['登录', '认证', '权限', 'unauthorized', 'forbidden', '401', '403']

        return authKeywords.some(keyword =>
            message.toLowerCase().includes(keyword.toLowerCase())
        )
    }

    /**
     * 处理认证失败：停止重连（暂不自动登出）
     * 注意：为避免误判，暂时不自动清除Token
     */
    _handleAuthFailure() {
        console.warn('[SSE] 认证失败，停止重连')

        // 停止重连
        this.reconnectAttempts = this.maxReconnectAttempts
        this.disconnect()

        // ⚠️ 暂时禁用自动登出，避免误判
        // 如果Token确实过期，用户在访问其他接口时会被提示

        /* 原自动登出逻辑（暂时禁用）
        localStorage.removeItem('adminToken')
        setTimeout(() => {
            if (window.location.pathname !== '/login') {
                window.location.href = '/login'
            }
        }, 1000)
        */
    }

    /**
     * 断开连接
     */
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close()
            this.eventSource = null
            this.connected = false
            console.log('[SSE] 已断开连接')
        }
    }

    /**
     * 处理重连
     */
    _handleReconnect() {
        this.disconnect()

        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('[SSE] 达到最大重连次数，停止重连')
            return
        }

        this.reconnectAttempts++
        const delay = this.reconnectDelay * this.reconnectAttempts
        console.log(`[SSE] ${delay / 1000}秒后尝试重连（第${this.reconnectAttempts}次）`)

        setTimeout(() => {
            this.connect()
        }, delay)
    }

    /**
     * 注册事件监听
     */
    on(eventType, callback) {
        if (!this.listeners.has(eventType)) {
            this.listeners.set(eventType, new Set())
        }
        this.listeners.get(eventType).add(callback)

        // 返回取消订阅函数
        return () => {
            this.listeners.get(eventType)?.delete(callback)
        }
    }

    /**
     * 触发事件
     */
    _emit(eventType, data) {
        const callbacks = this.listeners.get(eventType)
        if (callbacks) {
            callbacks.forEach(cb => {
                try {
                    cb(data)
                } catch (e) {
                    console.error('[SSE] 事件处理器错误', e)
                }
            })
        }
    }

    /**
     * 是否已连接
     */
    isConnected() {
        return this.connected
    }
}

// 单例
const sseService = new SseService()
export default sseService
