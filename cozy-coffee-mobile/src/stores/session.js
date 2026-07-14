import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { logout as logoutApi } from '@/api/auth'

const emptyUser = () => ({ id: null, nickname: '', avatar: '', phone: '', email: '', birthday: '' })
const emptyMember = () => ({ memberLevel: 'basic', currentPoints: 0, totalPoints: 0, expTotal: 0, couponCount: 0 })

function readJson(key, fallback) {
  try {
    const value = uni.getStorageSync(key)
    return value ? JSON.parse(value) : fallback
  } catch (_) {
    return fallback
  }
}

export const useSessionStore = defineStore('session', () => {
  const token = ref('')
  const userInfo = ref(emptyUser())
  const memberInfo = ref(emptyMember())
  const restored = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const isAuthenticated = isLoggedIn
  const userLevel = computed(() => memberInfo.value.memberLevel || 'basic')

  function restore() {
    token.value = uni.getStorageSync('token') || ''
    userInfo.value = { ...emptyUser(), ...readJson('userInfo', {}) }
    memberInfo.value = { ...emptyMember(), ...readJson('memberInfo', {}) }
    restored.value = true
    return isLoggedIn.value
  }

  function setLoginInfo(tokenValue, user = {}) {
    token.value = tokenValue || ''
    userInfo.value = { ...userInfo.value, ...user }
    uni.setStorageSync('token', token.value)
    uni.setStorageSync('userInfo', JSON.stringify(userInfo.value))
  }

  function setMemberInfo(info = {}) {
    memberInfo.value = { ...memberInfo.value, ...info }
    uni.setStorageSync('memberInfo', JSON.stringify(memberInfo.value))
  }

  function clearSession() {
    token.value = ''
    userInfo.value = emptyUser()
    memberInfo.value = emptyMember()
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('memberInfo')
  }

  async function logout() {
    try {
      if (token.value) await logoutApi()
    } finally {
      clearSession()
    }
  }

  restore()

  return {
    token,
    userInfo,
    memberInfo,
    restored,
    isLoggedIn,
    isAuthenticated,
    userLevel,
    restore,
    setLoginInfo,
    setMemberInfo,
    clearSession,
    logout
  }
})
