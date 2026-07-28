import { describe, expect, it, vi } from 'vitest'
import { refreshMemberProfile } from './MemberProfileService'

describe('refreshMemberProfile', () => {
  it('refreshes points, EXP and level in the session store', async () => {
    const sessionStore = { token: 'token', setMemberInfo: vi.fn() }
    const memberApi = vi.fn().mockResolvedValue({
      data: { currentPoints: 88, expTotal: 520, memberLevel: 'silver' }
    })

    const result = await refreshMemberProfile(sessionStore, memberApi)

    expect(sessionStore.setMemberInfo).toHaveBeenCalledWith({ currentPoints: 88, expTotal: 520, memberLevel: 'silver' })
    expect(result.memberLevel).toBe('silver')
  })

  it('does not request member data for a logged-out session', async () => {
    const sessionStore = { token: '', isAuthenticated: false, setMemberInfo: vi.fn() }
    const memberApi = vi.fn()

    await expect(refreshMemberProfile(sessionStore, memberApi)).resolves.toBeNull()
    expect(memberApi).not.toHaveBeenCalled()
  })
})
