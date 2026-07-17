<template>
  <nav class="site-navbar" :class="{ 'site-navbar--on-dark': isOnDarkSection, 'site-navbar--transparent': isTransparentTop, 'site-navbar--scrolled': isScrolled }" aria-label="主导航">
    <div class="site-navbar__inner">
      <router-link class="site-navbar__logo" to="/" aria-label="CozyCoffee 首页">
        <img src="/images/cozycafe_logo.png" alt="CozyCoffee" width="180" height="60">
      </router-link>

      <div class="site-navbar__links">
        <router-link to="/" :class="{ 'is-active': activeRouteKey === '/' }">首页</router-link>
        <a
          v-for="link in sectionLinks"
          :key="link.to"
          :href="link.to"
          :class="{ 'is-active': activeRouteKey === link.to }"
          @click="handleSectionClick(link.to, $event)"
        >{{ link.label }}</a>
        <router-link to="/about" :class="{ 'is-active': activeRouteKey === '/about' }">关于我们</router-link>
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
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const isOnDarkSection = ref(false)
const isScrolled = ref(false)
let membershipObserver = null
let bindTimer = null
let bindAttempts = 0
let scrollHandler = null

const isHomePage = computed(() => route.path === '/')

// 锚点导航激活判断：按 path + hash 精确匹配，避免 hash URL 被视作同名路由导致多链接同时亮
// 首页（path='/' 且无 hash）→ '/'；带 hash 的首页 → '/#origins' 等；关于我们 → '/about'
const activeRouteKey = computed(() => {
  if (route.path === '/about') return '/about'
  return `${route.path}${route.hash}`
})

// 三个 section 锚点（path 均为 '/'，仅 hash 不同）
const sectionLinks = Object.freeze([
  { to: '/#origins', label: '豆源' },
  { to: '/#menu', label: '菜单' },
  { to: '/#membership', label: '会员' }
])

// 首页第一屏：导航透明浮在 Hero 上，深咖文字；滚出 Hero 区后切白底 + 发丝边 + 微阴影。
const isTransparentTop = computed(() => isHomePage.value && !isScrolled.value)

// 同页 hash 跳转：与 Hero CTA「<a href="#origins">」行为完全一致 ——
// 用原生 scrollIntoView 让 scroll-margin-top 生效，绕开 Vue Router 的 el 滚动
// （后者在某些布局下与 sentinel observer / sticky 容器配合不一致，会落到第一章节而非标题区）。
// 跨页（如 /about → /#origins）：router.push 到首页，落地后下一帧再 scrollIntoView
async function handleSectionClick(to, event) {
  event.preventDefault()
  const hash = to.slice(to.indexOf('#'))
  const targetPath = to.slice(0, to.indexOf('#')) || '/'

  if (route.path !== targetPath) {
    // 跨页：先 push 到首页路由，hash 由 router scrollBehavior 接管
    await router.push(to)
    return
  }

  // 同页：更新 hash（激活态 + 分享链接），再用原生滚动定位
  if (route.hash !== hash) {
    router.replace(to)
  }
  const el = document.querySelector(hash)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

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

// 滚动监听：首页 Hero 内透明，滚出 Hero 区后切白。
// 阈值 = Hero 元素相对视口顶部下沿越过导航底边 -> 即 scrollY > heroBottom - navHeight。
function updateScrolledState() {
  if (!isHomePage.value) {
    isScrolled.value = false
    return
  }
  const hero = document.querySelector('.warm-hero')
  if (!hero) {
    isScrolled.value = window.scrollY > 8
    return
  }
  const navHeight = Number(getComputedStyle(document.documentElement).getPropertyValue('--nav-height').replace('px', '')) || 76
  const heroBottom = hero.getBoundingClientRect().bottom + window.scrollY
  isScrolled.value = window.scrollY + navHeight >= heroBottom - 1
}

function bindScroll() {
  unbindScroll()
  scrollHandler = () => updateScrolledState()
  window.addEventListener('scroll', scrollHandler, { passive: true })
  window.addEventListener('resize', scrollHandler)
  updateScrolledState()
}

function unbindScroll() {
  if (!scrollHandler) return
  window.removeEventListener('scroll', scrollHandler)
  window.removeEventListener('resize', scrollHandler)
  scrollHandler = null
}

watch(() => route.path, () => {
  bindMembershipObserver()
  if (isHomePage.value) {
    nextTick(bindScroll)
  } else {
    unbindScroll()
    isScrolled.value = false
  }
})
onMounted(() => {
  bindMembershipObserver()
  if (isHomePage.value) nextTick(bindScroll)
})
onUnmounted(() => {
  disconnectMembershipObserver()
  unbindScroll()
})
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
  transition: background-color 300ms ease, border-color 300ms ease,
              box-shadow 300ms ease, color 300ms ease;
}

/* 首页第一屏：半透明微毛玻璃浮在 Hero 上，深咖文字 */
.site-navbar--transparent {
  background: rgba(255, 255, 255, 0.25);
  border-bottom-color: transparent;
  color: #3B2418;
  backdrop-filter: blur(2px);
  box-shadow: none;
}

/* 滚出 Hero / 非首页：白底 + 发丝边 + 微阴影 */
.site-navbar--scrolled,
.site-navbar--on-dark {
  background: rgba(255, 255, 255, 0.96);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.site-navbar--on-dark {
  background: var(--cozy-bg);
  border-bottom-color: var(--cozy-border);
  box-shadow: none;
}

.site-navbar__inner {
  width: 100%;
  height: 100%;
  padding-inline: clamp(24px, 3.9vw, 60px);
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 40px;
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
  width: 176px;
  height: 60px;
  object-fit: contain;
}

.site-navbar__logo { justify-self: start; }

.site-navbar__links {
  display: flex;
  align-items: center;
  justify-self: center;
  gap: 28px;
}

.site-navbar__links a {
  position: relative;
  padding-inline: 10px;
  font-size: 16px;
  font-weight: 550;
  transition: color 180ms ease;
}

.site-navbar__links a::after {
  content: '';
  position: absolute;
  right: 10px;
  bottom: 3px;
  left: 10px;
  height: 2px;
  border-radius: 999px;
  background: var(--cozy-primary);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 180ms ease;
}

.site-navbar__links a:hover,
.site-navbar__links a.is-active,
.site-navbar__text-action:hover { color: var(--cozy-primary); }

.site-navbar__links a:hover::after,
.site-navbar__links a.is-active::after { transform: scaleX(1); }

.site-navbar__actions { display: flex; align-items: center; justify-self: end; gap: 8px; }
.site-navbar__text-action { padding-inline: 12px; border: 0; background: transparent; cursor: pointer; font-size: 15px; }
.site-navbar__button { min-width: 80px; padding: 9px 18px; border-radius: 10px; color: var(--cozy-on-primary); background: var(--cozy-primary); font-size: 15px; }
.site-navbar__button:hover { background: var(--cozy-primary-hover); }
.user-greeting { max-width: 112px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.site-navbar :where(a, button):focus-visible {
  outline: 3px solid var(--cozy-primary);
  outline-offset: 3px;
}

@media (max-width: 820px) {
  .site-navbar__inner { padding-inline: 16px; gap: 16px; }
  .site-navbar__links { display: none; }
  .site-navbar__inner { grid-template-columns: 1fr auto; }
}

@media (max-width: 440px) {
  .site-navbar__logo img { width: 132px; }
  .site-navbar__text-action { padding-inline: 8px; }
  .site-navbar__button { min-width: 68px; padding-inline: 12px; }
}

@media (prefers-reduced-motion: reduce) {
  .site-navbar,
  .site-navbar__links a,
  .site-navbar__links a::after { transition: none; }
}
</style>
