<template>
  <main class="warm-home">
    <section id="home" class="warm-hero" aria-labelledby="hero-title">
      <picture class="warm-hero__media">
        <img
          :src="heroImage"
          alt="阳光下的 Cozy Coffee 拿铁与杭州烘焙名片"
          fetchpriority="high"
          width="3600"
          height="1600"
          @error="handleImageError"
        >
      </picture>
      <div class="warm-hero__content">
        <h1 id="hero-title">
          <span class="hero-title-line">循着风味</span>
          <span class="hero-title-line">走进一段地理日志</span>
        </h1>
        <p class="hero-description">
          <span class="hero-description-line">从世界八大产区，到杭州匠心烘焙</span>
          <span class="hero-description-line">每一杯，都是土地与时间留下的答案</span>
        </p>
      </div>
      <a class="hero-cta" href="#origins">
        <img class="hero-cta__arrow" :src="heroArrow" alt="" aria-hidden="true">
        <span class="hero-cta__text">探索产地风味之旅</span>
      </a>
    </section>

    <OriginsJourney />

    <EditorialMenu :products="menuItems" :flavor-routes="flavorRoutes" @image-error="handleImageError" />

    <!-- Bridge：菜单 → 荣誉的节奏过渡。无 CTA，只保留向下视觉引导。
         SVG line+chevron opacity 0.4 持续 2.4s 呼吸，告诉用户下面还有内容。
         荣誉 banner 自带「了解我们的坚持 →」按钮，避免 CTA 重复。 -->
    <section class="menu-bridge" aria-label="menu to brand rhythm">
      <div class="warm-shell menu-bridge__shell">
        <p class="menu-bridge__kicker">OUR PHILOSOPHY</p>
        <span class="menu-bridge__rule" aria-hidden="true"></span>
        <h2 class="menu-bridge__title">
          <span>每一颗豆子</span>
          <span>都值得认真对待</span>
        </h2>
        <span class="menu-bridge__hint" aria-hidden="true">
          <svg width="14" height="40" viewBox="0 0 14 40">
            <line x1="7" y1="0" x2="7" y2="28" stroke="currentColor" stroke-width="1" />
            <polyline points="3,25 7,30 11,25" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <span class="menu-bridge__rule" aria-hidden="true"></span>
      </div>
    </section>

    <section class="honor-banner" aria-labelledby="honor-banner-title">
      <picture class="honor-banner__media" aria-hidden="true">
        <source
          type="image/avif"
          :srcset="honorSrcsetAvif"
          sizes="(min-width: 1716px) 1716px, 100vw"
        >
        <source
          type="image/webp"
          :srcset="honorSrcsetWebp"
          sizes="(min-width: 1716px) 1716px, 100vw"
        >
        <img
          :src="honorFallback"
          :srcset="honorSrcsetJpg"
          sizes="(min-width: 1716px) 1716px, 100vw"
          alt="Cozy Coffee 连续五年获得 IIAC 金奖"
          loading="lazy"
          width="4096"
          height="928"
        >
      </picture>
      <div class="honor-banner__scrim" aria-hidden="true"></div>
      <div class="warm-shell honor-banner__content">
        <h2 id="honor-banner-title">荣誉与坚持</h2>
        <p>连续 5 年 IIAC 金奖认可，源自对品质的执着。</p>
        <router-link class="honor-banner__link" to="/about">了解我们的坚持 <span aria-hidden="true">→</span></router-link>
      </div>
    </section>

    <HomeMembership :is-mobile="isMobile" />
  </main>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import OriginsJourney from '@/components/home/OriginsJourney.vue'
import EditorialMenu from '@/components/home/EditorialMenu.vue'
import HomeMembership from '@/components/home/HomeMembership.vue'
import { useUserStore } from '@/stores/user'
import { HOME_FLAVOR_ROUTES, HOME_MENU_PRODUCTS } from '@/data/homeMenu'
import { useHomeMembership } from '@/composables/useHomeMembership'

// 静态资源：有 OSS 时走 OSS，否则用本地占位图
const OSS_BASE = import.meta.env.VITE_ASSET_BASE_URL || ''
const asset = (ossPath, localPath) => OSS_BASE ? `${OSS_BASE}${ossPath}` : localPath

const heroImage = asset(
  '/images/home/hero-prototype-20260717-v3.jpg',
  '/images/home/hero-prototype-20260717-v3.jpg'
)
const heroArrow = asset('/images/hero/hero-arrow.svg', '/images/hero/hero-arrow.svg')

const honorBannerBase = OSS_BASE
  ? `${OSS_BASE}/images/home/honor-banner`
  : '/images/home/honor-banner'
const honorSrcsetAvif = [768, 1440, 1920].map(w => `${honorBannerBase}-${w}.avif ${w}w`).join(', ')
const honorSrcsetWebp = [768, 1440, 1920].map(w => `${honorBannerBase}-${w}.webp ${w}w`).join(', ')
const honorSrcsetJpg = [768, 1440, 1920].map(w => `${honorBannerBase}-${w}.jpg ${w}w`).join(', ')
const honorFallback = `${honorBannerBase}-1440.jpg`

const userStore = useUserStore()
const isMobile = ref(false)
let mediaQuery = null

const { initMembership, resetMembership, resetDisposed } = useHomeMembership(userStore)

const menuItems = HOME_MENU_PRODUCTS
const flavorRoutes = HOME_FLAVOR_ROUTES

function handleImageError(event) {
  const image = event.currentTarget
  if (image.dataset.fallbackApplied) return
  image.dataset.fallbackApplied = 'true'
  image.srcset = ''
  image.src = '/images/beans.jpg'
  image.classList.add('image-fallback')
}

function updateMobileState(event) {
  isMobile.value = event.matches
}

onMounted(async () => {
  mediaQuery = window.matchMedia('(max-width: 700px)')
  isMobile.value = mediaQuery.matches
  mediaQuery.addEventListener('change', updateMobileState)

  resetDisposed()
  await userStore.init()
  await initMembership()
})

onUnmounted(() => {
  resetMembership()
  mediaQuery?.removeEventListener('change', updateMobileState)
})
</script>
<style>
.warm-hero h1, .warm-hero h1 span,
.warm-hero .hero-cta__text {
  font-family: "Source Han Serif SC", "思源宋体", "Noto Serif SC", "Songti SC", "SimSun", serif;
}

/* Hero 图片底部淡出：图片底边自然消融到 #eee1d2，消除与下一段的硬切 */
.warm-hero__media img {
  mask-image: linear-gradient(to bottom, black 85%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, black 85%, transparent 100%);
}

/* Hero 底部渐变：从透明过渡到下一段背景色，消除硬切留白 */
.warm-hero::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 20%;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(to bottom, transparent 0%, var(--cozy-bg) 100%);
}

/* 跨组件共享的布局工具：不受 scoped 限制，可穿透到子组件 */
.warm-shell {
  width: min(var(--content-max), calc(100% - var(--content-gutter) - var(--content-gutter)));
  margin-inline: auto;
}

.warm-home :where(a, button, summary):focus-visible {
  outline: 3px solid var(--cozy-primary);
  outline-offset: 4px;
}

.warm-home :where(#home, #origins, #menu, #membership) {
  scroll-margin-top: var(--nav-height);
}

@media (prefers-reduced-motion: reduce) {
  .warm-home *,
  .warm-home *::before,
  .warm-home *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>

<style scoped>
.warm-home {
  background: var(--cozy-bg);
  color: var(--cozy-ink);
  font-family: var(--font-sans);
  overflow: clip;
}

.warm-hero {
  position: relative;
  aspect-ratio: 3600 / 1730;
  width: 100%;
  min-height: 520px;
  container-type: size;
  isolation: isolate;
  overflow-x: clip;
  overflow-y: visible;
  background: #eee1d2;
}

/* Bridge 升级：上下短分隔线 + OUR PHILOSOPHY 副标 + 两行标题 + ↓ 呼吸 SVG（无 CTA）。
   padding-block 收紧让 Series → Slogan 间距落在 100–140px，不空。 */
.menu-bridge {
  padding-block: clamp(40px, 4vw, 56px);
  background: var(--cozy-surface);
  color: var(--cozy-ink);
}
.menu-bridge__shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: clamp(14px, 2vw, 22px);
}
.menu-bridge__kicker {
  margin: 0;
  color: var(--cozy-muted);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
.menu-bridge__rule {
  width: clamp(40px, 6vw, 96px);
  height: 1px;
  background: color-mix(in oklch, var(--cozy-primary) 35%, transparent);
}
.menu-bridge__title {
  margin: 0;
  text-align: center;
  font-family: var(--font-display);
  font-size: clamp(1.4rem, 2.4vw, 2rem);
  font-weight: 500;
  color: var(--cozy-primary);
  letter-spacing: .02em;
  line-height: 1.4;
  text-wrap: balance;
}
.menu-bridge__title span { display: block; }

/* ↓ 视觉引导：opacity 0.4，2.4s 呼吸 translateY(4px)；reduced-motion 关掉动画。 */
.menu-bridge__hint {
  display: block;
  color: var(--cozy-primary);
  opacity: .4;
  animation: menu-bridge-breathe 2.4s ease-in-out infinite;
}
@keyframes menu-bridge-breathe {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(4px); }
}

@media (max-width: 560px) {
  .menu-bridge { padding-block: 48px; }
  .menu-bridge__shell { gap: 16px; }
}

.warm-hero__media,
.warm-hero__media img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.warm-hero__media img {
  object-fit: cover;
  object-position: center;
}

/* 文字组：原型画布 3600×1600 内 left:569，top 下移让出顶部留白 */
.warm-hero__content {
  position: absolute;
  z-index: 2;
  left: 15.805556cqw;
  top: 32cqh;
  width: 35.083333cqw;
  margin: 0;
  padding: 0;
  color: #2d170a;
  animation: warm-fade 700ms ease-out both;
}

/* H1 主标题：思源宋体（--font-display），macOS→Noto Serif SC，Windows→SimSun 兜底 */
.warm-hero h1 {
  margin: 0;
  max-width: none;
  font-family: var(--font-display);
  font-weight: 400;
  line-height: 1.355556;
  letter-spacing: 0;
  text-align: left;
  text-wrap: wrap;
  color: #2d170a;
}

.warm-hero h1 .hero-title-line {
  display: block;
}

.warm-hero h1 .hero-title-line:nth-child(1) {
  font-size: 3.35cqw;
}

.warm-hero h1 .hero-title-line:nth-child(2) {
  font-size: 3.45cqw;
}

/* 副文案两行：字号 48、行高 68、行1字距 2px 行2字距 7px、色 #3a1f12、微软雅黑 Regular */
.warm-hero__content .hero-description {
  margin: 4.05cqh 0 0;
  padding: 0;
  font-family: var(--font-sans);
  font-size: 1.333333cqw;
  font-weight: 400;
  line-height: 1.416667;
  text-align: left;
  text-wrap: wrap;
  color: #3a1f12;
}

.warm-hero__content .hero-description .hero-description-line {
  display: block;
}

.warm-hero__content .hero-description .hero-description-line:nth-child(1) {
  letter-spacing: 0.0416667em;
}

.warm-hero__content .hero-description .hero-description-line:nth-child(2) {
  letter-spacing: 0.145833em;
}

/* CTA：左右居中、贴 Hero 底部。直接挂在 .warm-hero 下（与 __content 同级），
   绝对定位参考 .warm-hero 容器；cq 单位按容器宽高缩放。
   呼吸动画驱动 scale，keyframes 内同时保留 translateX(-50%) 以维持居中。 */
.warm-hero .hero-cta {
  position: absolute;
  z-index: 2;
  bottom: 2cqh;
  left: 50%;
  width: 15.361111cqw;
  height: 14.4375cqh;
  display: block;
  transform: translateX(-50%) scale(1);
  transform-origin: center bottom;
  animation: hero-cta-breathe 2.6s ease-in-out infinite;
  text-decoration: none;
  color: #3b3028;
  cursor: pointer;
}

/* 箭头：CTA 区内居中、向下指。垂直定位用百分比（相对 CTA 盒子），字号 cq 按 .warm-hero 缩放。 */
.warm-hero .hero-cta__arrow {
  position: absolute;
  left: 50%;
  top: 38.528139%; /* 89px / 231px */
  width: auto;
  height: 26.190476%; /* 60.5px / 231px */
  transform: translateX(-50%);
}

/* CTA 文字：思源宋体 Medium、底部居中。bottom 用百分比相对 CTA，字号 cq 按 .warm-hero 缩放。 */
.warm-hero .hero-cta__text {
  position: absolute;
  left: 50%;
  bottom: 6.060606%; /* 14px / 231px */
  transform: translateX(-50%);
  font-family: var(--font-display);
  font-size: 1cqw;
  font-weight: 500;
  text-align: center;
  letter-spacing: 0.138889em;
  line-height: 1.888889;
  white-space: nowrap;
  color: #3b3028;
}

@keyframes hero-cta-breathe {
  0%, 100% { transform: translateX(-50%) scale(1); }
  50%      { transform: translateX(-50%) scale(1.06); }
}

.honor-banner {
  position: relative;
  width: 100%;
  min-height: 333px;
  margin-inline: 0;
  display: grid;
  align-items: start;
  isolation: isolate;
  overflow: hidden;
  color: #f1d3a3;
  background: #1d1009;
}
.honor-banner__media,
.honor-banner__media img,
.honor-banner__scrim {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.honor-banner__media,
.honor-banner__scrim {
  width: min(100%, 1716px);
  margin-inline: auto;
}
.honor-banner__media img { display: block; object-fit: cover; object-position: center; }
.honor-banner__scrim { z-index: 1; background: linear-gradient(90deg, rgba(19, 10, 6, .26), rgba(19, 10, 6, .08) 42%, transparent 60%); }
.honor-banner__content { position: relative; z-index: 2; align-self: center; width: min(100%, 1716px); margin-inline: auto; padding: 0 clamp(32px, 6.5vw, 112px); }
.honor-banner h2 { margin: 0; color: #e8c99a; font-family: var(--font-display); font-size: clamp(2.25rem, 3.1vw, 3.125rem); line-height: 1.2; font-weight: 500; letter-spacing: 0.05em; text-align: left; text-wrap: balance; }
.honor-banner__content p { max-width: 28em; margin: 23px 0 0; color: #f4e7d3; font-size: clamp(1rem, 1.15vw, 1.22rem); line-height: 1.6; letter-spacing: 0.05em; }
.honor-banner__link { min-height: 56px; display: inline-flex; align-items: center; gap: 12px; margin-top: 28px; padding: 13px 46px; border: 1px solid rgba(232, 201, 154, .72); border-radius: 999px; color: #f3d7aa; font-size: 17px; font-weight: 400; text-decoration: none; transition: color .22s ease, background .22s ease, border-color .22s ease; }
.honor-banner__link:hover { color: #24140b; border-color: #f0cf9d; background: #f0cf9d; }

@keyframes warm-fade { from { opacity: 0; } to { opacity: 1; } }

@media (max-width: 760px) {
  .warm-shell { width: min(100% - 32px, 620px); }
  /* 窄屏画面宽远小于 3600×1600 的设计画布，aspect-ratio 会让 Hero 过矮。
     放开比例约束、给一个保底高度；图切换到 cover 满铺（裁切优于上下留白）。
     cqw/cqh 自动按容器宽等比缩放所有字号与位置，无需在此重写字号。 */
  .warm-hero { aspect-ratio: auto; height: min(90svh, 640px); min-height: 520px; }
  .warm-hero__media img { object-fit: cover; object-position: 70% center; }
  .honor-banner { min-height: 430px; align-items: start; }
  .honor-banner__media img { object-position: 66% center; }
  .honor-banner__scrim { background: linear-gradient(90deg, rgba(20, 10, 6, .94) 0%, rgba(20, 10, 6, .72) 55%, rgba(20, 10, 6, .12) 100%); }
  .honor-banner__content { align-self: start; padding-block: 48px; }
}
</style>
