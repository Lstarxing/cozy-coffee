import { computed } from 'vue'
import { useSessionStore } from '@/stores/session'
import { MEMBER_LEVEL_THEMES } from '@/constants/member'

export function useMemberTheme() {
  const store = useSessionStore()
  const level = computed(() => store.userLevel)
  const levelTheme = computed(() => MEMBER_LEVEL_THEMES[level.value] || MEMBER_LEVEL_THEMES.basic)
  const themeStyle = computed(() => ({
    '--member-surface': levelTheme.value.surface,
    '--member-text': levelTheme.value.text,
    '--member-accent': levelTheme.value.accent
  }))
  const isDark = computed(() => Boolean(levelTheme.value.isDark))
  return { themeStyle, levelTheme, isDark, level }
}
