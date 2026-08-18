/**
 * 订单相关 API
 * 基于 openapi.json 中的 /api/order/* 接口
 */
import { get, post } from '@/api/request'

// ==================== 订单管理 ====================

// 创建订单
export const createOrder = (data, options = {}) => {
    return post('/order/create', data, options)
}

export const checkCart = (data) => {
    return post('/order/cart/check', data)
}

// 获取用户订单列表
export const getOrderList = () => {
    return get('/order/list')
}

// 获取订单详情
export const getOrderDetail = (orderId) => {
    return get(`/order/${orderId}`)
}

// 取消订单
export const cancelOrder = (orderId) => {
    return post(`/order/${orderId}/cancel`)
}

// 支付成功后自动接单
export const acceptOrder = (orderId) => {
    return post(`/order/${orderId}/accept`)
}
