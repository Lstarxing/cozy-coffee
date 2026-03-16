/**
 * 订单相关 API
 * 基于 openapi.json 中的 /api/order/* 接口
 */
import { get, post } from '@/api/request'

// ==================== 订单管理 ====================

// 创建订单
export const createOrder = (data) => {
    return post('/order/create', data)
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
