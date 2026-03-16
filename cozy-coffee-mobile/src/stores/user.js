/**
 * 用户状态管理 (Pinia Store)
 * 
 * 功能：
 * 1. 用户登录状态
 * 2. 用户信息存储
 * 3. 会员等级信息
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    // ==================== State ====================

    /**
     * 登录凭证
     */
    const token = ref('')

    /**
     * 用户基本信息
     */
    const userInfo = ref({
        id: null,
        nickname: '',
        avatar: '',
        phone: '',
        email: '',
        birthday: ''
    })

    /**
     * 会员信息
     */
    const memberInfo = ref({
        memberLevel: 'basic',
        currentPoints: 0,
        totalPoints: 0,
        expTotal: 0,
        couponCount: 0
    })

    // ==================== Getters ====================

    /**
     * 是否已登录
     */
    const isLoggedIn = computed(() => !!token.value)

    /**
     * 用户等级
     */
    const userLevel = computed(() => memberInfo.value.memberLevel || 'basic')

    // ==================== Actions ====================

    /**
     * 设置登录信息
     */
    const setLoginInfo = (tokenVal, user) => {
        token.value = tokenVal
        userInfo.value = { ...userInfo.value, ...user }

        // 持久化
        uni.setStorageSync('token', tokenVal)
        uni.setStorageSync('userInfo', JSON.stringify(userInfo.value))
    }

    /**
     * 设置会员信息
     */
    const setMemberInfo = (info) => {
        memberInfo.value = { ...memberInfo.value, ...info }
        uni.setStorageSync('memberInfo', JSON.stringify(memberInfo.value))
    }

    /**
     * 登出
     */
    const logout = () => {
        token.value = ''
        userInfo.value = { id: null, nickname: '', avatar: '', phone: '', email: '', birthday: '' }
        memberInfo.value = { memberLevel: 'basic', currentPoints: 0, totalPoints: 0, expTotal: 0, couponCount: 0 }

        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.removeStorageSync('memberInfo')
    }

    /**
     * 从本地存储恢复状态
     */
    const loadFromStorage = () => {
        try {
            const savedToken = uni.getStorageSync('token')
            const savedUser = uni.getStorageSync('userInfo')
            const savedMember = uni.getStorageSync('memberInfo')

            if (savedToken) token.value = savedToken
            if (savedUser) userInfo.value = JSON.parse(savedUser)
            if (savedMember) memberInfo.value = JSON.parse(savedMember)
        } catch (e) {
            console.error('恢复用户状态失败:', e)
        }
    }

    // 初始化时恢复
    loadFromStorage()

    return {
        token,
        userInfo,
        memberInfo,
        isLoggedIn,
        userLevel,
        setLoginInfo,
        setMemberInfo,
        logout
    }
})
