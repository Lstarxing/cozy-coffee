<template>
  <div class="app-container">
    <NavBar v-if="!$route.meta.hideNavBar" />
    <router-view />
    <Footer v-if="!$route.meta.hideFooter" />
  </div>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'
import Footer from '@/components/Footer.vue'
import { useUserStore } from '@/stores/user'
import { useRoute } from 'vue-router'
import { watch, onMounted } from 'vue'

const route = useRoute()
const userStore = useUserStore()

onMounted(() => { userStore.init() })

watch(
  () => route.meta?.hideNavBar,
  (hideNavBar) => {
    if (hideNavBar) {
      document.body.style.paddingTop = '0'
    } else {
      document.body.style.paddingTop = 'var(--nav-height)'
    }
  },
  { immediate: true }
)
</script>

<style>
/* 确保全屏布局 */
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.app-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
