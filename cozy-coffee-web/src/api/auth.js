import request from './request'

export function login(username, password) {
    return request.post('/auth/login', { username, password })
}

export function register(username, password, inviterCode) {
    return request.post('/auth/register', { username, password, inviterCode })
}

export function logout() {
    return request.post('/auth/logout')
}

export function getUserInfo() {
    return request.get('/auth/userinfo')
}

export function getProfile() {
    return request.get('/auth/profile')
}

export function updateProfile(data) {
    return request.put('/auth/profile', data)
}

// 头像先上传到 MinIO（经 gateway /api/auth/avatar），拿回 URL 再写入 profile
export function uploadAvatar(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/auth/avatar', formData)
}

export function changePassword(oldPassword, newPassword) {
    return request.post('/auth/password/change', { oldPassword, newPassword })
}

export function applyInviteCode(code) {
    return request.post('/auth/invite/apply', { inviterCode: code })
}
