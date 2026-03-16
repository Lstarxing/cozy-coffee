/**
 * 积分商城模拟数据
 */

/**
 * 获取积分商品列表
 */
export const getMallProducts = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                code: 200,
                msg: 'success',
                data: [
                    {
                        id: 1,
                        name: '5元优惠券',
                        pointsPrice: 240,
                        image: '/static/images/coupon-5.png',
                        type: 'coupon',
                        stock: 999,
                        monthlyLimit: 2,
                        desc: '满40元可用'
                    },
                    {
                        id: 2,
                        name: '美式兑换券',
                        pointsPrice: 520,
                        image: '/static/images/americano.png',
                        type: 'coupon',
                        stock: 100,
                        monthlyLimit: 2,
                        desc: '中杯美式免费兑换'
                    },
                    {
                        id: 3,
                        name: '拿铁兑换券',
                        pointsPrice: 680,
                        image: '/static/images/latte.png',
                        type: 'coupon',
                        stock: 50,
                        monthlyLimit: 2,
                        desc: '中杯拿铁免费兑换'
                    },
                    {
                        id: 4,
                        name: '品牌马克杯',
                        pointsPrice: 2600,
                        image: '/static/images/mug.png',
                        type: 'goods',
                        stock: 20,
                        monthlyLimit: 1,
                        desc: '限量版咖啡杯'
                    }
                ]
            })
        }, 200)
    })
}

/**
 * 获取用户优惠券
 */
export const getUserCoupons = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                code: 200,
                msg: 'success',
                data: [
                    {
                        id: 101,
                        name: '5元优惠券',
                        value: 5,
                        minAmount: 40,
                        expireDate: '2026-01-15',
                        status: 'available'
                    },
                    {
                        id: 102,
                        name: '8折饮品券',
                        value: 0,
                        discount: 0.8,
                        maxDiscount: 10,
                        expireDate: '2026-01-10',
                        status: 'available'
                    }
                ]
            })
        }, 200)
    })
}
