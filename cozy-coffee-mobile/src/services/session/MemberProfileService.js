import { getMemberInfo } from '@/api/member'

export async function refreshMemberProfile(sessionStore, memberApi = getMemberInfo) {
  if (!sessionStore?.setMemberInfo) return null
  if (!sessionStore.token && !sessionStore.isAuthenticated) return null

  const response = await memberApi()
  const memberInfo = response?.data ?? response
  if (!memberInfo || typeof memberInfo !== 'object' || Array.isArray(memberInfo)) return null

  sessionStore.setMemberInfo(memberInfo)
  return memberInfo
}
