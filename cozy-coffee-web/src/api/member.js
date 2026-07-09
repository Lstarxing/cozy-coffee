import request from './request'

// 会员信息
export function getMemberInfo() {
    return request.get('/member/info')
}

// 签到
export function signIn() {
    return request.post('/member/signin')
}

// 会员等级权益
export function getMemberBenefits() {
    return request.get('/member/benefits/status')
}

export function receiveMonthlyBenefit() {
    return request.post('/member/benefits/receive-monthly')
}

// 月度任务
export function getMonthlyTask() {
    return request.get('/member/monthly-task')
}

// 积分
export function getPointsTransactions(params) {
    return request.get('/member/points/transactions', { params })
}

// 地址
export function getAddresses() {
    return request.get('/member/addresses')
}

export function createAddress(data) {
    return request.post('/member/addresses', data)
}

export function updateAddress(id, data) {
    return request.put(`/member/addresses/${id}`, data)
}

export function deleteAddress(id) {
    return request.delete(`/member/addresses/${id}`)
}

export function setDefaultAddress(id) {
    return request.put(`/member/addresses/${id}/default`)
}

// SSE Ticket
export function getSseTicket() {
    return request.post('/member/sse/ticket')
}
