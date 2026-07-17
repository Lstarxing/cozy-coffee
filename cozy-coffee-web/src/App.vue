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
import { computed, watch, onMounted } from 'vue'

const route = useRoute()
const userStore = useUserStore()
const isHome = computed(() => route.path === '/')

onMounted(() => { userStore.init() })

watch(
  () => [route.meta?.hideNavBar, isHome.value],
  ([hideNavBar, home]) => {
    document.body.classList.toggle('no-nav-pad', Boolean(hideNavBar))
    document.body.classList.toggle('home-transparent-nav', !hideNavBar && home)
    document.body.style.paddingTop = hideNavBar ? '0' : (!home ? 'var(--nav-height)' : '0')
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
  user-select: none;
  -webkit-user-select: none;
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
