import { describe, expect, it } from 'vitest'
import { MEMBER_LEVELS, MEMBER_LEVEL_THEMES, DEV_MEMBER_MOCK } from './member'

describe('member level themes', () => {
  it('every level has a complete theme', () => {
    for (const level of MEMBER_LEVELS) {
      const theme = MEMBER_LEVEL_THEMES[level]
      expect(theme).toBeDefined()
      expect(typeof theme.surface).toBe('string')
      expect(typeof theme.text).toBe('string')
      expect(typeof theme.accent).toBe('string')
      expect(typeof theme.isDark).toBe('boolean')
    }
  })

  it('black is the only dark theme', () => {
    const darks = MEMBER_LEVELS.filter(l => MEMBER_LEVEL_THEMES[l].isDark)
    expect(darks).toEqual(['black'])
  })
})

describe('dev member mock', () => {
  it('every level has a mock matching memberLevel', () => {
    for (const level of MEMBER_LEVELS) {
      const mock = DEV_MEMBER_MOCK[level]
      expect(mock).toBeDefined()
      expect(mock.memberLevel).toBe(level)
      expect(typeof mock.levelName).toBe('string')
      expect(mock.expTotal).toBeGreaterThan(0)
      expect(mock.currentPoints).toBeGreaterThanOrEqual(0)
    }
  })

  it('diamond mock exp clears the diamond threshold', () => {
    expect(DEV_MEMBER_MOCK.diamond.expTotal).toBeGreaterThanOrEqual(4000)
  })
})
