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
// TODO(backend): 后端尚未提供 /order/banners 接口，目前返回空数组。
// 前端 index.vue 会 fallback 到 defaultBanners，不会影响用户体验。
// 后端补充接口后，将 Promise.resolve 改为 return get('/order/banners')。
export const getBanners = () => {
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
