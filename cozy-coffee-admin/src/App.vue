<template>
  <router-view />
  <GlobalNotifications />
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import sseService from './api/sse'
import GlobalNotifications from './components/GlobalNotifications.vue'

const route = useRoute()

// 检查登录状态并连接 SSE
const checkAndConnectSse = () => {
  const token = localStorage.getItem('adminToken')
  if (token && route.path !== '/login') {
    sseService.connect()
  }
}

// 路由变化时检查
watch(() => route.path, (newPath) => {
  if (newPath === '/login') {
    sseService.disconnect()
  } else {
    checkAndConnectSse()
  }
})

onMounted(() => {
  checkAndConnectSse()
})

onUnmounted(() => {
  sseService.disconnect()
})
</script>
