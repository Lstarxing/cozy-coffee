/**
 * 优惠券相关 API
 * 基于 openapi.json 的实际接口路径
 * 接口在 PointsMallController: /api/member/mall/*
 */
import { get, post } from '@/api/request'

// 获取用户优惠券列表
// 正确路径: /api/member/mall/coupons
export const getCouponList = (status) => {
    // status: available | used | expired | ISSUED | USED | EXPIRED
    const data = {}
    if (status) {
        data.status = status
    }
    return get('/member/mall/coupons', data)
}

// 获取可用优惠券（用于订单结算页）
// 正确路径: /api/member/mall/coupons/available
export const getAvailableCoupons = (orderAmount) => {
    return get('/member/mall/coupons/available', { orderAmount })
}

// 使用优惠券 - 在订单创建时通过 couponCode 参数传递
// 订单创建接口支持 couponCode 参数
