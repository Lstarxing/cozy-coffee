import request from './request'

// 获取商品列表 (积分商城)
// 获取商品列表 (积分商城)
export function getPointsProducts() {
    return request.get('/member/mall/products')
}

// 兑换商品
export function redeemProduct(data) {
    return request.post('/member/mall/redeem', data)
}

// 获取用户兑换记录
export function getUserRedemptions() {
    return request.get('/member/mall/orders')
}

// 获取用户优惠券列表
// status: 'ISSUED' (可使用), 'USED' (已使用), 'EXPIRED' (已过期)
export function getUserCoupons(status) {
    return request.get('/member/mall/coupons', {
        params: { status }
    })
}

// 获取当单可用优惠券
// data: { orderAmount, items }
export function getAvailableCoupons(data) {
    return request.post('/member/mall/coupons/available', data)
}
