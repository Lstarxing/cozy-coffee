import axios from 'axios'

const baseURL = (import.meta.env.VITE_API_BASE_URL || '') + '/api'

const api = axios.create({
    baseURL,
    timeout: 10000,
    withCredentials: true
})

let isHandlingAuthFailure = false

function handleAuthFailure() {
    if (isHandlingAuthFailure) {
        return
    }
    isHandlingAuthFailure = true

    if (window.location.pathname !== '/login') {
        window.location.href = '/login'
    }

    setTimeout(() => {
        isHandlingAuthFailure = false
    }, 1500)
}

// 请求拦截器 — 管理端走 JWT Bearer header（不依赖共享 cozy_token cookie，避免与用户端串号）
api.interceptors.request.use(
    config => {
        const token = localStorage.getItem('adminToken')
        if (token) {
            config.headers = config.headers || {}
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
    response => {
        const res = response.data
        const isSuccess = res.success === true || res.code === 1 || res.code === 200
        if (!isSuccess) {
            return Promise.reject(new Error(res.message || res.msg || '请求失败'))
        }
        return res
    },
    error => {
        console.error('API Error:', error)

        const status = error?.response?.status
        const url = error?.config?.url || ''
        const isLoginApi = url.includes('/auth/login')

        if (!isLoginApi && (status === 401 || status === 403)) {
            handleAuthFailure()
        }

        return Promise.reject(error)
    }
)

export default api

// ==================== 管理端 API ====================

// 登录认证（admin 专属端点：只返回 token，不写用户端 cookie）
export const adminLogin = (username, password) =>
    api.post('/auth/admin/login', { username, password })

// 退出登录（仅吊销 admin token，不清用户端 cookie）
export const adminLogout = () => api.post('/auth/admin/logout')

// 控制台
export const getDashboardStats = (startDate, endDate) => api.get('/admin/dashboard/stats', { params: { startDate, endDate } })
export const getAnalyticsTrend = (params) => api.get('/admin/analytics/trend', { params })
export const getAnalyticsDistribution = (params) => api.get('/admin/analytics/distribution', { params })
export const getAnalyticsRank = (params) => api.get('/admin/analytics/rank', { params })

export const getRecentOrders = (limit = 10) => api.get('/admin/orders/recent', { params: { limit } })
export const getRecentRedemptions = (limit = 10) => api.get('/admin/redemptions/recent', { params: { limit } })

// 用户管理
export const getUsers = (params) => api.get('/admin/users', { params })
export const getUserDetail = (userId) => api.get(`/admin/users/${userId}`)
export const adjustUserPoints = (userId, amount, reason) =>
    api.post(`/admin/users/${userId}/points`, null, { params: { amount, reason } })
export const updateUserStatus = (userId, status) =>
    api.put(`/admin/users/${userId}/status`, null, { params: { status } })

// 积分商品（管理端CRUD）
export const getPointsProducts = () => api.get('/admin/products/points')
export const addPointsProduct = (product) => api.post('/admin/products/points', product)
export const updatePointsProduct = (productId, product) => api.put(`/admin/products/points/${productId}`, product)
export const deletePointsProduct = (productId) => api.delete(`/admin/products/points/${productId}`)
export const togglePointsProductStatus = (productId) => api.put(`/admin/products/points/${productId}/status`)

// 咖啡商品（管理端CRUD）
export const getCoffeeProducts = () => api.get('/admin/products/coffee')
export const addCoffeeProduct = (product) => api.post('/admin/products/coffee', product)
export const updateCoffeeProduct = (productId, product) => api.put(`/admin/products/coffee/${productId}`, product)
export const deleteCoffeeProduct = (productId) => api.delete(`/admin/products/coffee/${productId}`)
export const toggleCoffeeProductStatus = (productId) => api.put(`/admin/products/coffee/${productId}/status`)
export const getAddonCatalog = () => api.get('/admin/products/addon-catalog')
export const saveAddonGroups = (productId, groups) => api.post(`/admin/products/coffee/${productId}/addon-groups`, groups)

// 内容档案（产区/单品豆/拼配豆）
export const getOrigins = () => api.get('/admin/content/origins')
export const saveOrigin = (origin) => api.post('/admin/content/origins', origin)
export const deleteOrigin = (id) => api.delete(`/admin/content/origins/${id}`)
export const getBeans = () => api.get('/admin/content/beans')
export const saveBean = (bean) => api.post('/admin/content/beans', bean)
export const deleteBean = (id) => api.delete(`/admin/content/beans/${id}`)
export const getBlends = () => api.get('/admin/content/blends')
export const saveBlend = (blend) => api.post('/admin/content/blends', blend)
export const deleteBlend = (id) => api.delete(`/admin/content/blends/${id}`)

// 咖啡订单管理
export const getOrderCounts = () => api.get('/admin/orders/counts')
export const getOrders = (params) => api.get('/admin/orders', { params })
export const getOrderDetail = (orderId) => api.get(`/admin/orders/${orderId}`)
export const acceptOrder = (orderId) => api.post(`/admin/orders/${orderId}/accept`)
export const completeOrder = (orderId) => api.post(`/admin/orders/${orderId}/complete`)
export const cancelOrder = (orderId) => api.post(`/admin/orders/${orderId}/cancel`)

// 积分兑换订单
export const getRedemptions = (params) => api.get('/admin/redemptions', { params })
export const getRedemptionDetail = (orderId) => api.get(`/admin/redemptions/${orderId}`)
export const processRedemption = (orderId) => api.post(`/admin/redemptions/${orderId}/process`)
export const shipRedemption = (orderId, company, trackingNo) =>
    api.post(`/admin/redemptions/${orderId}/ship`, null, { params: { company, trackingNo } })
export const completeRedemption = (orderId) => api.post(`/admin/redemptions/${orderId}/complete`)
export const deleteRedemption = (orderId) => api.delete(`/admin/redemptions/${orderId}`)

// 文件上传（type: coffee 咖啡商品 / points 积分兑换商品）
export const uploadImage = (file, type = 'products') => {
    const formData = new FormData()
    formData.append('file', file)
    if (type) formData.append('type', type)
    return api.post('/admin/upload/image', formData)
}

