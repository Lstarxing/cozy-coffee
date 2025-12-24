import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(null)
    const userInfo = ref(null)

    // 模拟的初始数据
    const mockUser = {
        id: 1001,
        nickname: "CozyUser",
        totalPoints: 500,
        currentPoints: 500,
        email: "basic@example.com",
        phoneNumber: "13800138001",
        avatar: "/images/default-avatar.png",
        invitationCode: "BASIC001",
        level: "basic",
        memberLevel: "basic",
        signInDays: 2,
        makeupCards: 1,
        signInHistory: ['2023-12-01', '2023-12-02'],
        lastSignIn: '',
        addresses: []
    }

    // 初始化：从localStorage读取token和userInfo
    const storedToken = localStorage.getItem('token')
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedToken) {
        token.value = storedToken
        // 优先使用存储的真实用户信息
        if (storedUserInfo) {
            userInfo.value = JSON.parse(storedUserInfo)
        } else {
            userInfo.value = { ...mockUser }
        }
        console.log('User store initialized with token from localStorage')
    }

    const isLoggedIn = computed(() => !!token.value)
    // 兼容 level 和 memberLevel 两个字段
    const userLevel = computed(() => userInfo.value?.memberLevel || userInfo.value?.level || 'basic')

    function login(user, existingToken) {
        // 映射后端字段名到前端
        userInfo.value = {
            ...mockUser,  // 默认值
            ...user,       // 传入的数据覆盖
            // 确保字段名一致
            level: user.memberLevel || user.level || 'basic',
            memberLevel: user.memberLevel || user.level || 'basic',
            signInDays: user.consecutiveSignDays || user.signInDays || 0
        }
        token.value = existingToken || 'mock-token-' + Date.now()
        localStorage.setItem('token', token.value)
        // 同时存储用户信息到localStorage
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }

    function logout() {
        token.value = null
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    function signIn() {
        if (!userInfo.value) return false

        const d = new Date()
        const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        if (userInfo.value.lastSignIn === today) return false

        userInfo.value.lastSignIn = today
        userInfo.value.signInDays = (userInfo.value.signInDays % 7) + 1
        userInfo.value.currentPoints += 5
        userInfo.value.totalPoints += 5

        if (!userInfo.value.signInHistory) userInfo.value.signInHistory = []
        userInfo.value.signInHistory.push(today)

        return true
    }

    // 获取基本用户信息 (包含邀请码等)
    async function fetchUserInfo() {
        if (!token.value) return

        try {
            const response = await fetch('http://localhost:8080/api/auth/userinfo', {
                headers: {
                    'Authorization': `Bearer ${token.value}`
                }
            })
            const data = await response.json()
            if (data.success && data.data) {
                userInfo.value = {
                    ...userInfo.value,
                    ...data.data,
                    // 映射可能的字段名差异
                    signInDays: data.data.signInDays || userInfo.value?.signInDays || 0,
                    hasAppliedInviteCode: data.data.hasAppliedInviteCode !== undefined ? data.data.hasAppliedInviteCode : (userInfo.value?.hasAppliedInviteCode || false)
                }
                localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
                console.log('User info updated (with inviteCode)')
            }
        } catch (error) {
            console.error('Failed to fetch user info:', error)
        }
    }

    // 获取最新会员信息
    async function fetchMemberInfo() {
        if (!token.value) return

        try {
            const response = await fetch('http://localhost:8080/api/member/info', {
                headers: {
                    'Authorization': `Bearer ${token.value}`
                }
            })
            const data = await response.json()
            if (data.success && data.data) {
                // 合并会员信息到 userInfo
                userInfo.value = {
                    ...userInfo.value,
                    ...data.data,
                    level: data.data.memberLevel || userInfo.value?.level || 'basic',
                    memberLevel: data.data.memberLevel || userInfo.value?.memberLevel || 'basic',
                    signInDays: data.data.consecutiveSignDays || userInfo.value?.signInDays || 0
                }
                localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
                console.log('Member info updated:', userInfo.value.memberLevel)
            }
        } catch (error) {
            console.error('Failed to fetch member info:', error)
        }
    }

    return {
        token,
        userInfo,
        isLoggedIn,
        userLevel,
        login,
        logout,
        signIn,
        fetchUserInfo,
        fetchMemberInfo
    }
})
