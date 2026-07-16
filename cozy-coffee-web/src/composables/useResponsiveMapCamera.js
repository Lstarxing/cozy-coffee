import { computed, onMounted, onUnmounted, ref } from 'vue'
import { HANGZHOU_POINT } from '@/data/coffeeOrigins'
import {
  createMapCamera,
  MAP_CAMERA_MEDIA_QUERY,
  MAP_CAMERA_PRESETS,
} from '@/utils/coffeeMapCamera'

export function useResponsiveMapCamera() {
  const isMobileMap = ref(false)
  let mediaQuery = null

  const updateCameraMode = event => {
    isMobileMap.value = event.matches
  }

  const camera = computed(() => createMapCamera({
    focus: HANGZHOU_POINT,
    ...(isMobileMap.value
      ? MAP_CAMERA_PRESETS.mobile
      : MAP_CAMERA_PRESETS.desktop),
  }))

  onMounted(() => {
    mediaQuery = window.matchMedia(MAP_CAMERA_MEDIA_QUERY)
    isMobileMap.value = mediaQuery.matches
    mediaQuery.addEventListener('change', updateCameraMode)
  })

  onUnmounted(() => {
    mediaQuery?.removeEventListener('change', updateCameraMode)
  })

  return { camera, isMobileMap }
}
