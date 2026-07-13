import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminLogin } from '../api'
import { adminLogout } from '../api'

export const useAdminStore = defineStore('admin', () => {
    const adminInfo = ref(null)
    const isLoggedIn = ref(false)

    /**
     * 使用后端API进行登录认证
     * 只有role='admin'的用户才能登录成功
     */
    const login = async (username, password) => {
        try {
            const res = await adminLogin(username, password)
            // 兼容 success字段 或 code=1 或 code=200
            const isSuccess = res.success || res.code === 1 || res.code === 200

            if (isSuccess && res.data?.token) {
                const token = res.data.token
                // 解析JWT获取用户信息
                const payload = parseJwt(token)

                // 验证是否为管理员角色
                if (payload.role !== 'admin') {
                    console.warn('用户不是管理员，登录被拒绝')
                    return { success: false, message: '该账号没有管理员权限' }
                }

                adminInfo.value = {
                    userId: payload.sub,
                    username: payload.username,
                    role: payload.role
                }
                isLoggedIn.value = true
                return { success: true }
            }
            return { success: false, message: res.message || res.msg || '登录失败' }
        } catch (error) {
            console.error('登录失败:', error)
            return { success: false, message: error.message || '登录失败' }
        }
    }

    /**
     * 解析JWT Token
     */
    const parseJwt = (token) => {
        try {
            const base64Url = token.split('.')[1]
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            )
            return JSON.parse(jsonPayload)
        } catch (e) {
            console.error('JWT解析失败:', e)
            return {}
        }
    }

    const logout = async () => {
        try {
            await adminLogout()
        } catch (error) {
            console.warn('Admin logout API failed:', error)
        } finally {
            adminInfo.value = null
            isLoggedIn.value = false
        }
    }

    const init = async () => {
        try {
            const baseURL = (import.meta.env.VITE_API_BASE_URL || '') + '/api'
            const response = await fetch(baseURL + '/auth/me', { credentials: 'include' })
            if (response.ok) {
                const userData = await response.json()
                if (userData.data?.role === 'admin') {
                    adminInfo.value = userData.data
                    isLoggedIn.value = true
                }
            }
        } catch (e) {
            console.warn('Admin init check failed:', e)
        }
    }

    return { adminInfo, isLoggedIn, login, logout, init }
})
