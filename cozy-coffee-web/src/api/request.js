import axios from 'axios'

const baseURL = (import.meta.env.VITE_API_BASE_URL || '') + '/api'

const request = axios.create({
    baseURL,
    timeout: 10000
})

// 请求拦截器 — 自动注入 token
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error)
)

let isHandlingAuthFailure = false

function handleAuthFailure() {
    if (isHandlingAuthFailure) return
    isHandlingAuthFailure = true
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    if (window.location.pathname !== '/login') {
        window.location.href = '/login'
    }
    setTimeout(() => { isHandlingAuthFailure = false }, 1500)
}

// 响应拦截器 — 统一返回 backend payload, 业务异常抛 Error
request.interceptors.response.use(
    response => {
        const res = response.data
        // 兼容 success / code=1 / code=200 三种后端成功判定
        const isSuccess = res.success === true || res.code === 1 || res.code === 200
        if (!isSuccess) {
            return Promise.reject(new Error(res.message || res.msg || '请求失败'))
        }
        return res
    },
    error => {
        const status = error?.response?.status
        const url = error?.config?.url || ''
        const isLoginApi = url.includes('/auth/login')

        if (!isLoginApi && (status === 401 || status === 403)) {
            handleAuthFailure()
        }
        return Promise.reject(error)
    }
)

export default request
