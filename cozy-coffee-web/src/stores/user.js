import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api/request'
import { getUserInfo as fetchUserInfoApi } from '@/api/auth'
import { getMemberInfo as fetchMemberInfoApi } from '@/api/member'

export const useUserStore = defineStore('user', () => {
    const token = ref(null)
    const userInfo = ref(null)
    const isLoggedIn = computed(() => !!token.value)
    const userLevel = computed(() => userInfo.value?.memberLevel || userInfo.value?.level || 'basic')

    let initPromise = null

    async function init() {
        if (initPromise) return initPromise
        initPromise = (async () => {
            try {
                const baseURL = (import.meta.env.VITE_API_BASE_URL || '') + '/api'
                const response = await fetch(baseURL + '/auth/me', { credentials: 'include' })
                if (!response.ok) return
                token.value = 'cookie-based'
                await fetchUserInfo()
                await fetchMemberInfo()
            } catch (e) {
                console.warn('Store init failed:', e)
            }
        })()
        return initPromise
    }

    function login(user, existingToken) {
        userInfo.value = {
            ...user,
            level: user.memberLevel || user.level || 'basic',
            memberLevel: user.memberLevel || user.level || 'basic',
            signInDays: user.consecutiveSignDays || user.signInDays || 0
        }
        token.value = existingToken || 'cookie-based'
    }

    async function logout() {
        try {
            await request.post('/auth/logout')
            token.value = null
            userInfo.value = null
            return true
        } catch (error) {
            console.error('Logout API failed:', error)
            return false
        }
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

    async function fetchUserInfo() {
        if (!token.value) return

        try {
            const data = await fetchUserInfoApi()
            if (data && data.data) {
                const { currentPoints, totalPoints, memberLevel, level, expTotal, ...authData } = data.data

                userInfo.value = {
                    ...userInfo.value,
                    ...authData,
                    signInDays: data.data.signInDays || userInfo.value?.signInDays || 0,
                    hasAppliedInviteCode: data.data.hasAppliedInviteCode !== undefined ? data.data.hasAppliedInviteCode : (userInfo.value?.hasAppliedInviteCode || false)
                }
            }
        } catch (error) {
            console.error('Failed to fetch user info:', error)
        }
    }

    async function fetchMemberInfo() {
        if (!token.value) return

        try {
            const data = await fetchMemberInfoApi()
            if (data && data.data) {
                userInfo.value = {
                    ...userInfo.value,
                    ...data.data,
                    level: data.data.memberLevel || userInfo.value?.level || 'basic',
                    memberLevel: data.data.memberLevel || userInfo.value?.memberLevel || 'basic',
                    signInDays: data.data.consecutiveSignDays || userInfo.value?.signInDays || 0,
                    expTotal: data.data.expTotal || 0,
                    expiringPoints: data.data.expiringPoints || 0
                }
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
        init,
        login,
        logout,
        signIn,
        fetchUserInfo,
        fetchMemberInfo
    }
})
