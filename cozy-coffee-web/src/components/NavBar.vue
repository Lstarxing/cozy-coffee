<template>
  <nav class="site-navbar" :class="{ 'site-navbar--on-dark': isOnDarkSection }" aria-label="主导航">
    <div class="site-navbar__inner">
      <router-link class="site-navbar__logo" to="/" aria-label="CozyCoffee 首页">
        <img src="/images/cozycafe_logo.png" alt="CozyCoffee" width="180" height="60">
      </router-link>

      <div class="site-navbar__links">
        <router-link to="/">首页</router-link>
        <a href="/#origins">豆源</a>
        <a href="/#menu">菜单</a>
        <a href="/#membership">会员</a>
        <router-link to="/about">关于我们</router-link>
      </div>

      <div class="site-navbar__actions">
        <template v-if="!userStore.isLoggedIn">
          <router-link to="/login" class="site-navbar__text-action">登录</router-link>
          <router-link to="/register" class="site-navbar__button">注册</router-link>
        </template>
        <template v-else>
          <router-link to="/member" class="site-navbar__button">
            <span class="user-greeting">{{ userStore.userInfo?.nickname || '会员中心' }}</span>
          </router-link>
          <button class="site-navbar__text-action" type="button" @click="handleLogout">退出</button>
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const isOnDarkSection = ref(false)
let membershipObserver = null
let bindTimer = null
let bindAttempts = 0

function disconnectMembershipObserver() {
  membershipObserver?.disconnect()
  membershipObserver = null
  isOnDarkSection.value = false
  if (bindTimer) {
    window.clearTimeout(bindTimer)
    bindTimer = null
  }
}

async function bindMembershipObserver() {
  disconnectMembershipObserver()
  if (route.path !== '/') return

  await nextTick()
  const membership = document.querySelector('#membership')
  if (!membership) {
    if (bindAttempts >= 10) return
    bindAttempts += 1
    bindTimer = window.setTimeout(bindMembershipObserver, 50)
    return
  }
  bindAttempts = 0

  membershipObserver = new IntersectionObserver(
    ([entry]) => {
      isOnDarkSection.value = entry.isIntersecting
    },
    {
      root: null,
      rootMargin: '0px 0px -70% 0px',
      threshold: 0
    }
  )
  membershipObserver.observe(membership)
}

const handleLogout = async () => {
  const ok = await userStore.logout()
  if (ok) {
    router.push('/')
    return
  }
  window.alert('退出失败，请检查后端服务或网络后重试')
}

watch(() => route.path, bindMembershipObserver)
onMounted(bindMembershipObserver)
onUnmounted(disconnectMembershipObserver)
</script>

<style scoped>
.site-navbar {
  position: fixed;
  inset: 0 0 auto;
  z-index: 1000;
  height: var(--nav-height);
  color: var(--cozy-ink);
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid transparent;
  backdrop-filter: blur(10px);
  transition: background-color 180ms ease, border-color 180ms ease;
}

.site-navbar--on-dark {
  background: var(--cozy-bg);
  border-bottom-color: var(--cozy-border);
  backdrop-filter: none;
}

.site-navbar__inner {
  width: min(1280px, calc(100% - 48px));
  height: 100%;
  margin-inline: auto;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 32px;
}

.site-navbar__logo,
.site-navbar__links a,
.site-navbar__text-action,
.site-navbar__button {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: inherit;
  font-family: var(--font-sans);
  font-weight: 600;
  text-decoration: none;
}

.site-navbar__logo img {
  width: 154px;
  height: 52px;
  object-fit: contain;
}

.site-navbar__links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.site-navbar__links a {
  padding-inline: 12px;
  font-size: 14px;
  font-weight: 500;
}

.site-navbar__links a:hover,
.site-navbar__text-action:hover { text-decoration: underline; text-underline-offset: 5px; }

.site-navbar__actions { display: flex; align-items: center; gap: 8px; }
.site-navbar__text-action { padding-inline: 12px; border: 0; background: transparent; cursor: pointer; font-size: 14px; }
.site-navbar__button { min-width: 76px; padding: 8px 16px; border-radius: 9px; color: var(--cozy-on-primary); background: var(--cozy-primary); font-size: 14px; }
.site-navbar__button:hover { background: var(--cozy-primary-hover); }
.user-greeting { max-width: 112px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.site-navbar :where(a, button):focus-visible {
  outline: 3px solid var(--cozy-primary);
  outline-offset: 3px;
}

@media (max-width: 820px) {
  .site-navbar__inner { width: min(100% - 32px, 760px); gap: 16px; }
  .site-navbar__links { display: none; }
  .site-navbar__inner { grid-template-columns: 1fr auto; }
}

@media (max-width: 440px) {
  .site-navbar__logo img { width: 132px; }
  .site-navbar__text-action { padding-inline: 8px; }
  .site-navbar__button { min-width: 68px; padding-inline: 12px; }
}

@media (prefers-reduced-motion: reduce) {
  .site-navbar { transition: none; }
}
</style>
