// API 配置 - 统一后端接口地址
const API_BASE_URL = 'http://localhost:8080'

export const API = {
    // 认证相关
    AUTH: {
        REGISTER: `${API_BASE_URL}/api/auth/register`,
        LOGIN: `${API_BASE_URL}/api/auth/login`,
        USER_INFO: `${API_BASE_URL}/api/auth/userinfo`,
        UPDATE_PROFILE: `${API_BASE_URL}/api/auth/profile`,
        INVITE_APPLY: `${API_BASE_URL}/api/auth/invite/apply`,        // 填写邀请码
        INVITE_VALIDATE: `${API_BASE_URL}/api/auth/invite/validate`,  // 验证邀请码
    },
    // 会员相关
    MEMBER: {
        INFO: `${API_BASE_URL}/api/member/info`,
        SIGNIN: `${API_BASE_URL}/api/member/signin`,
    },
    // 地址管理
    ADDRESS: {
        LIST: `${API_BASE_URL}/api/member/addresses`,
        CREATE: `${API_BASE_URL}/api/member/addresses`,
        DEFAULT: `${API_BASE_URL}/api/member/addresses/default`,
    },
    // 积分商城
    MALL: {
        PRODUCTS: `${API_BASE_URL}/api/member/mall/products`,
        REDEEM: `${API_BASE_URL}/api/member/mall/redeem`,
        ORDERS: `${API_BASE_URL}/api/member/mall/orders`,
    }
}

export default API_BASE_URL
