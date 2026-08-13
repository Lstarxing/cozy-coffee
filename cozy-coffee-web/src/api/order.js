import request from './request'

// 获取咖啡产品列表
export function getCoffeeProducts() {
    return request.get('/order/products')
}

// 获取咖啡产品详情
export function getCoffeeProductDetail(id) {
    return request.get(`/order/products/${id}`)
}

// 创建订单
// data: { items: [], couponCode: string, remark: string }
// idempotencyKey: 幂等键（防重复下单），未传则自动生成
export function createOrder(data, idempotencyKey) {
    const key = idempotencyKey || `web-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
    return request.post('/order/create', data, { headers: { 'Idempotency-Key': key } })
}

// 获取订单列表
// params: { status: 'pending' | 'completed' | ... }
export function listOrders(params) {
    return request.get('/order/list', { params })
}

// 获取订单详情
export function getOrderDetail(orderId) {
    return request.get(`/order/${orderId}`)
}

// 取消订单
export function cancelOrder(orderId) {
    return request.post(`/order/${orderId}/cancel`)
}
