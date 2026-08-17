/**
 * 用户认证相关 API
 * 基于 openapi.json 中的 /api/auth/* 接口
 */
import { config, get, post, put } from '@/api/request'

// 登录
export const login = (data) => {
    return post('/auth/login', data)
}

export const exchangeWechatSession = (code, deviceId) => {
    return post('/auth/wechat/session', { code, deviceId })
}

export const resetPasswordDev = (username, newPassword) => {
    return post('/auth/password/reset-dev', { username, newPassword })
}

export const changePassword = (oldPassword, newPassword) => {
    return post('/auth/password/change', { oldPassword, newPassword })
}

export const getCurrentSession = () => {
    return get('/auth/me')
}

export const logout = () => {
    return post('/auth/logout', {})
}

// 注册
export const register = (data) => {
    return post('/auth/register', data)
}

// 获取当前用户信息
export const getUserInfo = () => {
    return get('/auth/userinfo')
}

// 更新个人资料 (PUT)
export const updateProfile = (data) => {
    return put('/auth/profile', data)
}

// 更新个人资料 (POST, 兼容旧版本)
export const updateProfileLegacy = (data) => {
    return post('/auth/update-profile', data)
}

export const uploadAvatar = (filePath) => new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
        url: `${config.baseURL}/auth/avatar`,
        filePath,
        name: 'file',
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success: (response) => {
            try {
                const payload = typeof response.data === 'string' ? JSON.parse(response.data) : response.data
                const success = payload?.success === true || payload?.code === 1 || payload?.code === 200
                if (!success) {
                    reject(new Error(payload?.message || payload?.msg || '头像上传失败'))
                    return
                }
                resolve(payload)
            } catch (error) {
                reject(new Error('头像上传响应格式不正确'))
            }
        },
        fail: (error) => reject(new Error(error?.errMsg || '头像上传失败'))
    })
})

// 填写邀请码获取积分
export const applyInviteCode = (inviteCode) => {
    return post('/auth/invite/apply', { inviteCode })
}

// 验证邀请码是否有效
export const validateInviteCode = (inviteCode) => {
    return get('/auth/invite/validate', { inviteCode })
}
