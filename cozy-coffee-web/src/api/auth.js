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

export function applyInviteCode(code) {
    return request.post('/auth/invite/apply', { inviterCode: code })
}
