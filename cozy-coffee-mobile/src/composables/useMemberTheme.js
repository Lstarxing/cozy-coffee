import { computed } from 'vue'
import { useSessionStore } from '@/stores/session'
import { MEMBER_LEVEL_THEMES } from '@/constants/member'

export function useMemberTheme() {
  const store = useSessionStore()
  const level = computed(() => store.userLevel)
  const levelTheme = computed(() => MEMBER_LEVEL_THEMES[level.value] || MEMBER_LEVEL_THEMES.basic)
  const isDark = computed(() => Boolean(levelTheme.value.isDark))
  const themeStyle = computed(() => ({
    '--member-surface': levelTheme.value.surface,
    '--member-text': levelTheme.value.text,
    '--member-accent': levelTheme.value.accent,
    '--member-line': isDark.value ? 'rgba(255,255,255,.18)' : 'rgba(60,40,30,.14)',
    '--member-track': isDark.value ? 'rgba(255,255,255,.18)' : 'rgba(60,40,30,.10)'
  }))
  return { themeStyle, levelTheme, isDark, level }
}
