/**
 * 会员相关 API
 * 基于 openapi.json 的实际接口路径
 */
import { get, post, put, del } from '@/api/request'

// ==================== 会员信息 ====================

// 获取会员详细信息 (等级、积分、签到天数等)
// 正确路径: /api/member/info
export const getMemberInfo = () => {
    return get('/member/info')
}

// ==================== 签到 ====================

// 每日签到
// 正确路径: /api/member/signin (POST)
export const signIn = () => {
    return post('/member/signin')
}

// ==================== 积分 ====================

// 获取积分流水记录
// 正确路径: /api/member/points/transactions
export const getPointsTransactions = (limit = 20) => {
    return get('/member/points/transactions', { limit })
}

// 获取即将到期的积分
// 正确路径: /api/member/points/expiring
export const getExpiringPoints = (days = 30) => {
    return get('/member/points/expiring', { days })
}

// 添加积分（模拟消费场景，测试用）
// 正确路径: /api/member/points/add (POST)
export const addPoints = (data) => {
    return post('/member/points/add', data)
}

// ==================== 月度任务 ====================

// 获取当月任务进度
// 正确路径: /api/member/monthly-task
export const getMonthlyTask = () => {
    return get('/member/monthly-task')
}

// ==================== 收货地址 ====================

// 获取默认地址
// 正确路径: /api/member/addresses/default
export const getDefaultAddress = () => {
    return get('/member/addresses/default')
}

// 创建新地址
// 正确路径: /api/member/addresses (POST)
export const createAddress = (data) => {
    return post('/member/addresses', data)
}

// 删除地址
// 正确路径: /api/member/addresses/{id} (DELETE)
export const deleteAddress = (id) => {
    return del(`/member/addresses/${id}`)
}

// 设置默认地址
// 正确路径: /api/member/addresses/{id}/default (PUT)
export const setDefaultAddress = (id) => {
    return put(`/member/addresses/${id}/default`)
}

// ==================== 积分商城兑换 ====================

// 积分兑换下单
// 正确路径: /api/member/mall/redeem (POST)
export const redeemPoints = (data) => {
    // data: { productId, quantity, fulfillmentType?, storeId?, addressId?, receiverName?, receiverPhone?, receiverAddress?, remark? }
    return post('/member/mall/redeem', data)
}

// 获取我的积分兑换订单列表
// 正确路径: /api/member/mall/orders
export const getMyRedemptions = () => {
    return get('/member/mall/orders')
}

// 获取积分兑换订单详情
// 正确路径: /api/member/mall/orders/{id}
export const getRedemptionDetail = (orderId) => {
    return get(`/member/mall/orders/${orderId}`)
}

// 取消积分兑换订单
// 正确路径: /api/member/mall/orders/{id}/cancel (POST)
export const cancelRedemption = (orderId) => {
    return post(`/member/mall/orders/${orderId}/cancel`)
}
