/**
 * 菜单模拟数据
 * 
 * 结构说明：
 * - 返回格式模拟 Spring Boot 标准响应 { code, msg, data }
 * - data 为分类数组，每个分类包含 products 商品列表
 * - 使用网络占位图避免本地图片缺失问题
 * 
 * 使用方式：
 * import { getMenuData } from '@/api/mock/menuData'
 * const res = await getMenuData()
 */

// 使用 picsum.photos 作为占位图
const PLACEHOLDER_BASE = 'https://picsum.photos/seed'

/**
 * 模拟获取菜单数据
 * @returns {Promise<Object>} 标准响应格式
 */
export const getMenuData = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                code: 200,
                msg: 'success',
                data: [
                    {
                        id: 1,
                        name: '经典咖啡',
                        products: [
                            {
                                id: 101,
                                name: '美式咖啡',
                                description: '浓缩咖啡加水，口感醇厚，保留咖啡原始风味',
                                price: '22',
                                image: `${PLACEHOLDER_BASE}/americano/200/200`
                            },
                            {
                                id: 102,
                                name: '拿铁',
                                description: '意式浓缩搭配丝滑牛奶，经典不败的选择',
                                price: '28',
                                image: `${PLACEHOLDER_BASE}/latte/200/200`
                            },
                            {
                                id: 103,
                                name: '卡布奇诺',
                                description: '浓缩咖啡、蒸奶与奶泡的完美比例，绵密口感',
                                price: '30',
                                image: `${PLACEHOLDER_BASE}/cappuccino/200/200`
                            },
                            {
                                id: 104,
                                name: '摩卡',
                                description: '咖啡与巧克力的完美融合，甜蜜治愈',
                                price: '32',
                                image: `${PLACEHOLDER_BASE}/mocha/200/200`
                            }
                        ]
                    },
                    {
                        id: 2,
                        name: '特调饮品',
                        products: [
                            {
                                id: 201,
                                name: '生椰拿铁',
                                description: '椰子水+椰浆+espresso，清爽不腻',
                                price: '32',
                                image: `${PLACEHOLDER_BASE}/coconut/200/200`
                            },
                            {
                                id: 202,
                                name: '燕麦拿铁',
                                description: '植物奶新选择，燕麦香气与咖啡完美融合',
                                price: '30',
                                image: `${PLACEHOLDER_BASE}/oat/200/200`
                            },
                            {
                                id: 203,
                                name: '冰博克拿铁',
                                description: '冰博克牛奶制作，奶香浓郁，口感醇厚',
                                price: '35',
                                image: `${PLACEHOLDER_BASE}/icedlatte/200/200`
                            }
                        ]
                    },
                    {
                        id: 3,
                        name: '茶饮系列',
                        products: [
                            {
                                id: 301,
                                name: '茉莉奶绿',
                                description: '清香茉莉绿茶与牛奶的清爽组合',
                                price: '25',
                                image: `${PLACEHOLDER_BASE}/jasmine/200/200`
                            },
                            {
                                id: 302,
                                name: '桂花乌龙拿铁',
                                description: '桂花香气与乌龙茶韵，秋日限定',
                                price: '28',
                                image: `${PLACEHOLDER_BASE}/oolong/200/200`
                            }
                        ]
                    },
                    {
                        id: 4,
                        name: '甜品小食',
                        products: [
                            {
                                id: 401,
                                name: '提拉米苏',
                                description: '意式经典甜品，浓郁咖啡与奶酪的完美融合',
                                price: '38',
                                image: `${PLACEHOLDER_BASE}/tiramisu/200/200`
                            },
                            {
                                id: 402,
                                name: '可颂面包',
                                description: '法式酥脆可颂，黄油香气四溢',
                                price: '18',
                                image: `${PLACEHOLDER_BASE}/croissant/200/200`
                            },
                            {
                                id: 403,
                                name: '蛋挞',
                                description: '葡式蛋挞，外酥内嫩',
                                price: '12',
                                image: `${PLACEHOLDER_BASE}/eggtart/200/200`
                            }
                        ]
                    }
                ]
            })
        }, 300)
    })
}

/**
 * 模拟获取推荐商品
 */
export const getRecommendProducts = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                code: 200,
                msg: 'success',
                data: [
                    { id: 102, name: '拿铁', price: '28', image: `${PLACEHOLDER_BASE}/latte/200/200` },
                    { id: 201, name: '生椰拿铁', price: '32', image: `${PLACEHOLDER_BASE}/coconut/200/200` },
                    { id: 101, name: '美式咖啡', price: '22', image: `${PLACEHOLDER_BASE}/americano/200/200` }
                ]
            })
        }, 200)
    })
}

/**
 * 模拟获取轮播图
 */
export const getBanners = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                code: 200,
                msg: 'success',
                data: [
                    { id: 1, image: `${PLACEHOLDER_BASE}/banner1/750/320`, link: '' },
                    { id: 2, image: `${PLACEHOLDER_BASE}/banner2/750/320`, link: '' }
                ]
            })
        }, 100)
    })
}
