<template>
  <div v-if="remainingSeconds > 0" class="expire-text" :class="{ urgent: remainingSeconds <= 30 }">
    {{ formatted }}
  </div>
</template>

<script setup>
import { computed, toRef } from 'vue'
import { useCountdown } from '@/composables/useCountdown'

const props = defineProps({
  /** Expiry timestamp — ms since epoch or ISO-8601 string */
  expireAt: { type: [String, Number], default: null },
  /** Fallback: if expireAt is missing, createdAt + 60 s is used */
  createdAt: { type: [String, Number], default: null }
})

const resolvedExpireAt = computed(() => {
  if (props.expireAt) {
    const ts = new Date(props.expireAt).getTime()
    if (!Number.isNaN(ts)) return ts
  }
  if (props.createdAt) {
    const ts = new Date(props.createdAt).getTime()
    if (!Number.isNaN(ts)) return ts + 60 * 1000
  }
  return null
})

const { remainingSeconds } = useCountdown(toRef(resolvedExpireAt))

const formatted = computed(() => {
  if (remainingSeconds.value == null) return '即将超时'
  if (remainingSeconds.value <= 0) return '即将自动取消'
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `剩余 ${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
</script>

<style scoped>
.expire-text {
  font-size: 11px;
  color: #6b7280;
}
.expire-text.urgent {
  color: #dc2626;
  font-weight: 600;
}
</style>
