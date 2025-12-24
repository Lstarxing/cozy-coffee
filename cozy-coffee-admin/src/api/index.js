import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
    config => {
        const token = localStorage.getItem('adminToken')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
    response => {
        const res = response.data
        if (res.success === false) {
            return Promise.reject(new Error(res.message || '请求失败'))
        }
        return res
    },
    error => {
        console.error('API Error:', error)
        return Promise.reject(error)
    }
)

export default api

// ==================== 管理端 API ====================

// 控制台
export const getDashboardStats = () => api.get('/admin/dashboard/stats')

// 用户管理
export const getUsers = (params) => api.get('/admin/users', { params })
export const adjustUserPoints = (userId, amount, reason) =>
    api.post(`/admin/users/${userId}/points`, null, { params: { amount, reason } })

// 积分商品
export const getPointsProducts = () => api.get('/admin/products/points')

// 咖啡商品（管理端CRUD）
export const getCoffeeProducts = () => api.get('/admin/products/coffee')
export const addCoffeeProduct = (product) => api.post('/admin/products/coffee', product)
export const updateCoffeeProduct = (productId, product) => api.put(`/admin/products/coffee/${productId}`, product)
export const deleteCoffeeProduct = (productId) => api.delete(`/admin/products/coffee/${productId}`)
export const toggleCoffeeProductStatus = (productId) => api.put(`/admin/products/coffee/${productId}/status`)

// 咖啡订单管理
export const getOrders = (status, orderNo, startDate, endDate) =>
    api.get('/admin/orders', { params: { status, orderNo, startDate, endDate } })
export const acceptOrder = (orderId) => api.post(`/admin/orders/${orderId}/accept`)
export const completeOrder = (orderId) => api.post(`/admin/orders/${orderId}/complete`)
export const cancelOrder = (orderId) => api.post(`/admin/orders/${orderId}/cancel`)

// 积分兑换订单
export const getRedemptions = (status) => api.get('/admin/redemptions', { params: { status } })
export const processRedemption = (orderId) => api.post(`/admin/redemptions/${orderId}/process`)
export const shipRedemption = (orderId, company, trackingNo) =>
    api.post(`/admin/redemptions/${orderId}/ship`, null, { params: { company, trackingNo } })
export const completeRedemption = (orderId) => api.post(`/admin/redemptions/${orderId}/complete`)

