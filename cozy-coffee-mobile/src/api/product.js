/**
 * 商品相关 API
 * 基于 openapi.json 的实际接口路径
 */
import { get } from '@/api/request'

// ==================== 咖啡商品 (点单用) ====================

// 获取咖啡商品列表 (菜单页使用)
// 正确路径: /api/order/products
export const getCoffeeProducts = () => {
    return get('/order/products')
}

// 获取菜单数据 (分类+商品) - 同上
export const getMenuData = () => {
    return get('/order/products')
}

// 获取商品详情
// 正确路径: /api/order/products/{id}
export const getProductDetail = (id) => {
    return get(`/order/products/${id}`)
}

// 获取推荐商品 (首页)
// 注意: 后端可能没有专门的推荐接口，使用商品列表+前端筛选
export const getRecommendProducts = () => {
    return get('/order/products')
}

// 获取轮播图
// 注意: 后端可能没有移动端专用的 Banner 接口，需要后端补充或使用静态数据
export const getBanners = () => {
    // 暂时返回 Promise 模拟空数据，待后端补充接口
    return Promise.resolve({ code: 200, data: [] })
}

// ==================== 积分商城商品 ====================

// 获取积分商城商品列表
// 正确路径: /api/member/mall/products
export const getPointsProducts = () => {
    return get('/member/mall/products')
}

// 积分商品详情
// 正确路径: /api/member/mall/products/{id}
export const getPointsProductDetail = (id) => {
    return get(`/member/mall/products/${id}`)
}
