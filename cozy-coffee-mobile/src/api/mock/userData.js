/**
 * 用户和会员相关模拟数据
 */

/**
 * 模拟登录
 */
export const mockLogin = (username, password) => {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            // 简单验证
            if (username && password) {
                resolve({
                    code: 200,
                    msg: 'success',
                    data: {
                        token: 'mock_token_' + Date.now(),
                        user: {
                            id: 1,
                            nickname: 'CozyCoffee 用户',
                            avatar: '/static/images/default-avatar.png',
                            phone: '138****8888'
                        }
                    }
                })
            } else {
                reject({
                    code: 400,
                    msg: '用户名或密码错误'
                })
            }
        }, 500)
    })
}

/**
 * 获取会员信息
 */
export const getMemberInfo = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                code: 200,
                msg: 'success',
                data: {
                    memberLevel: 'silver',
                    currentPoints: 1580,
                    totalPoints: 2350,
                    expTotal: 2350,
                    couponCount: 3,
                    // 月度任务进度
                    monthlySpent: 280,
                    monthlyDeliveryOrders: 2,
                    // 黑卡加速包 (仅黑卡用户有效)
                    monthlyAccelerateRemaining: 300
                }
            })
        }, 300)
    })
}

/**
 * 获取积分流水
 */
export const getPointsHistory = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                code: 200,
                msg: 'success',
                data: [
                    {
                        id: 1,
                        type: 'earn',
                        amount: 28,
                        description: '消费获得',
                        createTime: '2026-01-02 10:30:00',
                        balance: 1580
                    },
                    {
                        id: 2,
                        type: 'earn',
                        amount: 25,
                        description: '每日签到',
                        createTime: '2026-01-02 09:00:00',
                        balance: 1552
                    },
                    {
                        id: 3,
                        type: 'spend',
                        amount: -240,
                        description: '兑换5元优惠券',
                        createTime: '2026-01-01 14:20:00',
                        balance: 1527
                    }
                ]
            })
        }, 200)
    })
}

/**
 * 每日签到
 */
export const dailySignin = () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            const dayInWeek = new Date().getDay() || 7
            const pointsMap = [10, 15, 20, 25, 30, 35, 40]
            const points = pointsMap[dayInWeek - 1]

            resolve({
                code: 200,
                msg: 'success',
                data: {
                    pointsEarned: points,
                    consecutiveDays: dayInWeek,
                    currentPoints: 1580 + points
                }
            })
        }, 300)
    })
}
