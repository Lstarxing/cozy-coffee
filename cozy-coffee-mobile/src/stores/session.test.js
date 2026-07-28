import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

// 在导入 store 前先 stub uni 全局
const storage = new Map()
vi.stubGlobal('uni', {
  getStorageSync: (k) => storage.get(k) ?? '',
  setStorageSync: (k, v) => storage.set(k, v),
  removeStorageSync: (k) => storage.delete(k)
})

const { useSessionStore } = await import('./session')
const { DEV_MEMBER_MOCK } = await import('@/constants/member')

beforeEach(() => {
  storage.clear()
  setActivePinia(createPinia())
})

describe('session store dev member override', () => {
  it('memberInfo reflects devOverride when set', () => {
    const store = useSessionStore()
    store.setDevLevel('diamond')
    expect(store.userLevel).toBe('diamond')
    expect(store.memberInfo.expTotal).toBe(DEV_MEMBER_MOCK.diamond.expTotal)
    expect(store.memberInfo.currentPoints).toBe(DEV_MEMBER_MOCK.diamond.currentPoints)
  })

  it('clearDevOverride restores real member info', () => {
    const store = useSessionStore()
    store.setDevLevel('gold')
    expect(store.userLevel).toBe('gold')
    store.clearDevOverride()
    expect(store.userLevel).toBe('basic')
  })

  it('setMemberInfo writes realMemberInfo without clobbering dev override', () => {
    const store = useSessionStore()
    store.setDevLevel('black')
    store.setMemberInfo({ expTotal: 9999, memberLevel: 'basic' })
    expect(store.userLevel).toBe('black')
    store.clearDevOverride()
    expect(store.memberInfo.expTotal).toBe(9999)
  })
})
