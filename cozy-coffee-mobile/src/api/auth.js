/**
 * 用户认证相关 API
 * 基于 openapi.json 中的 /api/auth/* 接口
 */
import { get, post, put } from '@/api/request'

// 登录
export const login = (data) => {
    return post('/auth/login', data)
}

export const exchangeWechatSession = (code) => {
    return post('/auth/wechat/session', { code })
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

// 填写邀请码获取积分
export const applyInviteCode = (inviteCode) => {
    return post('/auth/invite/apply', { inviteCode })
}

// 验证邀请码是否有效
export const validateInviteCode = (inviteCode) => {
    return get('/auth/invite/validate', { inviteCode })
}
