import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { logout as logoutApi } from '@/api/auth'
import { DEV_MEMBER_MOCK } from '@/constants/member'

const emptyUser = () => ({
  id: null,
  username: '',
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  birthday: '',
  inviteCode: '',
  hasAppliedInviteCode: false
})
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
  const realMemberInfo = ref(emptyMember())
  const devOverride = ref(null)
  const restored = ref(false)

  const memberInfo = computed(() =>
    devOverride.value
      ? (DEV_MEMBER_MOCK[devOverride.value] || realMemberInfo.value)
      : realMemberInfo.value
  )
  const isLoggedIn = computed(() => Boolean(token.value))
  const isAuthenticated = isLoggedIn
  const userLevel = computed(() => memberInfo.value.memberLevel || 'basic')

  function restoreDevOverride() {
    if (!import.meta.env.DEV) {
      uni.removeStorageSync('dev_member_override')
      return
    }
    const saved = uni.getStorageSync('dev_member_override')
    if (saved && DEV_MEMBER_MOCK[saved]) devOverride.value = saved
  }

  function restore() {
    token.value = uni.getStorageSync('token') || ''
    userInfo.value = { ...emptyUser(), ...readJson('userInfo', {}) }
    realMemberInfo.value = { ...emptyMember(), ...readJson('memberInfo', {}) }
    restoreDevOverride()
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
    realMemberInfo.value = { ...realMemberInfo.value, ...info }
    uni.setStorageSync('memberInfo', JSON.stringify(realMemberInfo.value))
  }

  function setDevLevel(level) {
    if (!import.meta.env.DEV) return
    devOverride.value = level
    uni.setStorageSync('dev_member_override', level)
  }

  function clearDevOverride() {
    devOverride.value = null
    uni.removeStorageSync('dev_member_override')
  }

  async function refreshMemberInfo() {
    try {
      const { getMemberInfo } = await import('@/api/member')
      const res = await getMemberInfo()
      const data = res?.data ?? res
      if (data) setMemberInfo(data)
    } catch (_) {
      // 静默失败：dev 恢复时后端可能不可达
    }
  }

  function clearSession() {
    token.value = ''
    userInfo.value = emptyUser()
    realMemberInfo.value = emptyMember()
    devOverride.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('memberInfo')
    uni.removeStorageSync('dev_member_override')
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
    realMemberInfo,
    devOverride,
    restored,
    isLoggedIn,
    isAuthenticated,
    userLevel,
    restore,
    setLoginInfo,
    setMemberInfo,
    setDevLevel,
    clearDevOverride,
    refreshMemberInfo,
    clearSession,
    logout
  }
})
