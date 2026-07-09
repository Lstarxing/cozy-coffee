import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * Countdown composable — each component instance gets its own 1 s ticker.
 * This avoids a full table re-render when only the countdown text changes.
 *
 * @param {import('vue').Ref<string|number>|string|number|null} expireAtVal
 *        Expiry timestamp (ms since epoch or ISO string).  Accepts a ref or
 *        a raw value.
 * @returns {{ remainingSeconds: import('vue').ComputedRef<number|null> }}
 */
export function useCountdown(expireAtVal) {
  const now = ref(Date.now())
  let timer = null

  onMounted(() => {
    timer = window.setInterval(() => {
      now.value = Date.now()
    }, 1000)
  })

  onUnmounted(() => {
    if (timer) {
      window.clearInterval(timer)
      timer = null
    }
  })

  const remainingSeconds = computed(() => {
    const raw = expireAtVal?.value ?? expireAtVal
    if (!raw) return null
    const expireMs = new Date(raw).getTime()
    if (Number.isNaN(expireMs)) return null
    const seconds = Math.floor((expireMs - now.value) / 1000)
    return seconds > 0 ? seconds : 0
  })

  return { remainingSeconds }
}
