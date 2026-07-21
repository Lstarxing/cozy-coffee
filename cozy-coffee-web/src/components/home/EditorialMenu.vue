<template>
  <section
    id="menu"
    class="editorial-menu"
    aria-labelledby="editorial-menu-title"
    :style="accentStyle"
  >
    <div class="editorial-menu__shell">
      <header class="menu-hero">
        <div class="menu-hero__copy">
          <p class="menu-hero__chapter">MENU</p>
          <h2 id="editorial-menu-title">今天，想喝哪一种风味？</h2>
          <p class="menu-hero__lead">每一种风味，都来自不同土地的表达。</p>
        </div>
        <div class="menu-hero__media">
          <img
            :src="heroImage.fallback"
            :alt="heroImage.alt"
            width="960"
            height="600"
            fetchpriority="high"
            decoding="async"
            @error="onImageError"
          >
        </div>
      </header>

      <nav class="flavor-nav" aria-label="选择今天的风味">
        <p class="flavor-nav__kicker">FLAVOR MAP</p>
        <p class="flavor-nav__lead">从产地，到风味。探索咖啡世界中的六种风味表达。</p>
        <div class="flavor-nav__items">
          <button
          v-for="route in routes"
          :key="route.id"
          type="button"
          class="flavor-nav__item"
          :class="{ 'is-active': activeRouteId === route.id }"
          :aria-pressed="activeRouteId === route.id"
          @click="selectRoute(route.id)"
        >
          <span class="flavor-nav__label">{{ route.label }}</span>
          <span class="flavor-nav__hint">{{ route.originHint }}</span>
        </button>
        </div>
      </nav>

      <div class="todays-cup" aria-live="polite">
        <div v-if="loading" class="todays-cup__skeleton" aria-hidden="true">
          <div class="skel skel--media"></div>
          <div class="skel-stack">
            <div class="skel skel--line skel--sm"></div>
            <div class="skel skel--line skel--lg"></div>
            <div class="skel skel--line"></div>
            <div class="skel skel--line skel--md"></div>
            <div class="skel skel--line skel--sm"></div>
          </div>
        </div>

        <Transition v-else name="menu-fade" mode="out-in">
          <article
            v-if="featuredCoffee"
            :key="activeRoute.id + '-' + featuredCoffee.id"
            class="todays-cup__body"
          >
            <div class="todays-cup__media">
              <picture>
                <source
                  v-if="featuredCover.avif"
                  type="image/avif"
                  :srcset="featuredCover.avif"
                  sizes="(max-width: 900px) calc(100vw - 32px), 42vw"
                >
                <source
                  v-if="featuredCover.webp"
                  type="image/webp"
                  :srcset="featuredCover.webp"
                  sizes="(max-width: 900px) calc(100vw - 32px), 42vw"
                >
                <img
                  :src="featuredCover.fallback"
                  :srcset="featuredCover.jpg || undefined"
                  sizes="(max-width: 900px) calc(100vw - 32px), 42vw"
                  :alt="featuredCover.alt || activeRoute.imageAlt || featuredCoffee.name"
                  width="1200"
                  height="900"
                  loading="eager"
                  decoding="async"
                  @error="onImageError"
                >
              </picture>
            </div>

            <div class="todays-cup__copy">
              <p class="todays-cup__kicker">今日推荐</p>
              <h3 class="todays-cup__name">{{ featuredCoffee.name }}</h3>
              <span v-if="featuredCoffee.originName" class="todays-cup__origin">{{ featuredCoffee.originName }}</span>
              <p class="todays-cup__notes">{{ formatNotes(featuredCoffee) }}</p>
              <p class="todays-cup__story">{{ featuredStory }}</p>
              <p class="todays-cup__price">¥{{ featuredCoffee.price }}</p>
              <router-link class="todays-cup__cta" to="/member/order">
                了解更多
                <span class="todays-cup__cta-arrow" aria-hidden="true">→</span>
              </router-link>
            </div>
          </article>

          <div v-else class="todays-cup__empty" role="status">
            <p>今日推荐准备中，请稍后再看。</p>
          </div>
        </Transition>
      </div>

      <section class="more-flavor" :aria-labelledby="moreFlavorTitleId">
        <header class="more-flavor__header">
          <h3 :id="moreFlavorTitleId">{{ moreFlavorTitle }}</h3>
        </header>

        <div class="more-flavor__list" :style="listMinHeightStyle">
          <Transition name="menu-fade" mode="out-in">
            <div :key="activeRoute.id" class="more-flavor__items">
              <template v-if="recommendedProducts.length">
                <router-link
                  v-for="coffee in recommendedProducts"
                  :key="coffee.id"
                  class="coffee-row"
                  :class="{ 'is-featured': coffee.id === featuredCoffee?.id }"
                  to="/member/order"
                  :aria-label="`${coffee.name}，¥${coffee.price}`"
                >
                  <span class="coffee-row__top">
                    <span>
                      <span class="coffee-row__name">{{ coffee.name }}</span>
                      <span v-if="coffee.originName" class="coffee-row__origin">{{ coffee.originName }}</span>
                    </span>
                    <span class="coffee-row__price">¥{{ coffee.price }}</span>
                  </span>
                  <span class="coffee-row__notes">{{ formatNotes(coffee) }}</span>
                </router-link>
              </template>
              <p v-else class="more-flavor__empty" role="status">
                更多产地正在准备中。
              </p>
            </div>
          </Transition>
        </div>
      </section>

      <section class="menu-series" aria-labelledby="menu-series-title">
        <header class="menu-series__header">
          <p class="menu-series__kicker">COLLECTION</p>
          <h3 id="menu-series-title">探索更多系列</h3>
          <p class="menu-series__lead">也可以，从另一种方式开始。</p>
        </header>

        <div class="menu-series__grid">
          <router-link
            v-for="item in seriesItems"
            :key="item.id"
            class="series-card"
            :to="item.href || '/member/order'"
            :aria-label="item.description ? `${item.name}，${item.description}` : item.name"
          >
            <div class="series-card__media">
              <img
                :src="item.image?.fallback || item.image?.jpg"
                :alt="item.image?.alt || item.name"
                width="400"
                height="400"
                loading="lazy"
                decoding="async"
                @error="onImageError"
              >
            </div>
            <span v-if="item.englishName" class="series-card__en">{{ item.englishName }}</span>
            <span class="series-card__name">{{ item.name }}</span>
            <span class="series-card__explore" aria-hidden="true">了解更多 →</span>
          </router-link>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { MENU_HERO_IMAGE, HOME_MENU_SERIES } from '@/data/homeMenu'

const props = defineProps({
  products: { type: Array, required: true },
  flavorRoutes: { type: Array, required: true },
  series: { type: Array, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['image-error'])

const routes = computed(() =>
  Array.isArray(props.flavorRoutes) && props.flavorRoutes.length ? props.flavorRoutes : []
)

const activeRouteId = ref(props.flavorRoutes?.[0]?.id || 'floral')

const activeRoute = computed(
  () => routes.value.find(route => route.id === activeRouteId.value) || routes.value[0] || {}
)

const productMap = computed(() => {
  const map = new Map()
  for (const product of props.products || []) {
    if (product?.id) map.set(product.id, product)
  }
  return map
})

const featuredCoffee = computed(() => {
  const id = activeRoute.value?.featuredProductId
  return productMap.value.get(id) || props.products?.[0] || null
})

const recommendedProducts = computed(() =>
  (activeRoute.value?.productIds || [])
    .map(id => productMap.value.get(id))
    .filter(product => product && product.available !== false)
)

const featuredCover = computed(() => {
  const route = activeRoute.value
  const product = featuredCoffee.value
  return (
    route?.coverImage ||
    product?.heroImage || {
      fallback: '/images/beans.jpg',
      jpg: '',
      avif: '',
      webp: '',
      alt: product?.name || '咖啡'
    }
  )
})

const featuredStory = computed(() => {
  const product = featuredCoffee.value
  if (!product) return ''
  return product.story || activeRoute.value?.description || ''
})

const moreFlavorTitle = computed(() => {
  if (!activeRoute.value?.id || activeRoute.value.id === 'all') return '探索今日菜单'
  return `探索更多${activeRoute.value.label || ''}`
})

const moreFlavorTitleId = 'more-flavor-title'

const seriesItems = computed(() =>
  Array.isArray(props.series) && props.series.length ? props.series : HOME_MENU_SERIES
)

const heroImage = MENU_HERO_IMAGE

const accentStyle = computed(() => ({
  '--menu-accent': activeRoute.value?.accentColor || 'var(--cozy-primary)'
}))

const listMinHeightStyle = computed(() => ({
  minHeight: 'min(288px, 50vw)'
}))

function formatNotes(product) {
  const notes = product?.notes || product?.flavorNotes || []
  const clipped = notes.slice(0, 3)
  if (!clipped.length) return ''
  let text = clipped.join(' · ')
  if (notes.length > 3) text += ' …'
  if (text.length > 55) text = `${text.slice(0, 52)}…`
  return text
}

function selectRoute(id) {
  activeRouteId.value = id
}

function onImageError(event) {
  emit('image-error', event)
  const image = event?.currentTarget
  if (!image || image.dataset.fallbackApplied) return
  image.dataset.fallbackApplied = 'true'
  image.removeAttribute('srcset')
  image.src = '/images/beans.jpg'
  image.classList.add('image-fallback')
}

function preloadFeaturedImages() {
  if (typeof window === 'undefined') return
  const urls = new Set()
  for (const route of routes.value) {
    const cover = route.coverImage
    if (cover?.fallback) urls.add(cover.fallback)
    if (cover?.localFallback) urls.add(cover.localFallback)
  }
  for (const url of urls) {
    const img = new Image()
    img.decoding = 'async'
    img.src = url
  }
}

onMounted(() => {
  preloadFeaturedImages()
})

watch(
  () => props.flavorRoutes,
  (next) => {
    if (!next?.length) return
    if (!next.some(route => route.id === activeRouteId.value)) {
      activeRouteId.value = next[0].id
    }
    preloadFeaturedImages()
  }
)
</script>

<style scoped>
.editorial-menu {
  padding-block: clamp(72px, 8vw, 100px) clamp(32px, 3vw, 48px);
  color: var(--cozy-ink);
  background: var(--cozy-surface);
}
.editorial-menu__shell {
  width: min(1240px, calc(100% - 80px));
  margin-inline: auto;
}
.editorial-menu :where(a, button):focus-visible {
  outline: 3px solid var(--cozy-primary);
  outline-offset: 4px;
}
.menu-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: clamp(40px, 5vw, 56px);
  align-items: center;
}
.menu-hero__chapter {
  margin: 0 0 16px;
  color: var(--cozy-muted);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}
.menu-hero h2 {
  margin: 0;
  max-width: 12em;
  font-family: var(--font-display);
  font-size: clamp(1.85rem, 3.2vw, 2.75rem);
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0.01em;
}
.menu-hero__lead {
  max-width: 22em;
  margin: 18px 0 0;
  color: var(--cozy-muted);
  font-size: clamp(15px, 1.2vw, 16px);
  font-weight: 400;
  line-height: 1.7;
}
.menu-hero__media {
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: 2px;
  background: oklch(0.95 0.01 55);
  margin-top: clamp(24px, 3vw, 40px);
}
.menu-hero__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 50% 35%;
  display: block;
}
.flavor-nav {
  margin-top: clamp(80px, 9vw, 96px);
  padding-bottom: 4px;
}
.flavor-nav__lead {
  margin: 0 0 20px;
  max-width: 32em;
  color: var(--cozy-muted);
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.03em;
  line-height: 1.6;
}
.flavor-nav__kicker {
  margin: 0 0 6px;
  color: var(--cozy-muted);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
.flavor-nav__items {
  display: flex;
  flex-wrap: wrap;
  gap: clamp(32px, 4vw, 64px);
}
.flavor-nav__item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  margin: 0;
  padding: 8px 0 10px;
  border: 0;
  background: transparent;
  color: var(--cozy-ink);
  cursor: pointer;
  text-align: left;
}
.flavor-nav__item::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: var(--menu-accent, var(--cozy-primary));
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}
.flavor-nav__item:hover::after,
.flavor-nav__item:focus-visible::after,
.flavor-nav__item.is-active::after {
  transform: scaleX(1);
}
.flavor-nav__label {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.04em;
  transition: color 0.2s ease;
}
.flavor-nav__item.is-active .flavor-nav__label {
  color: var(--menu-accent, var(--cozy-primary));
}
.flavor-nav__hint {
  color: var(--cozy-muted);
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0.06em;
  opacity: 0.6;
  transition: opacity 0.2s ease;
}
.flavor-nav__item:hover .flavor-nav__hint,
.flavor-nav__item.is-active .flavor-nav__hint {
  opacity: 0.85;
}
.todays-cup {
  margin-top: clamp(48px, 6vw, 72px);
  min-height: 320px;
}
.todays-cup__body {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: clamp(48px, 5vw, 64px);
  align-items: center;
}
.todays-cup__media {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 2px;
  background: oklch(0.95 0.01 55);
}
.todays-cup__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
.todays-cup__body:hover .todays-cup__media img {
  transform: scale(1.025);
}
.todays-cup__copy {
  min-width: 0;
  padding-block: 8px;
}
.todays-cup__kicker {
  margin: 0 0 24px;
  color: var(--menu-accent, var(--cozy-primary));
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.16em;
}
.todays-cup__name {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.75rem, 2.8vw, 2.4rem);
  font-weight: 500;
  line-height: 1.18;
}
.todays-cup__origin {
  display: block;
  margin-top: 6px;
  color: var(--cozy-muted);
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.04em;
}
.todays-cup__notes {
  margin: 14px 0 0;
  color: var(--cozy-muted);
  font-size: 14px;
  letter-spacing: 0.02em;
}
.todays-cup__story {
  margin: 18px 0 0;
  max-width: 28em;
  min-height: 3.2em;
  color: var(--cozy-ink);
  font-size: 15px;
  font-weight: 400;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}
.todays-cup__price {
  margin: 20px 0 0;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 400;
  color: var(--cozy-muted);
  letter-spacing: 0.02em;
}
.todays-cup__cta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
  color: var(--menu-accent, var(--cozy-primary));
  font-size: 15px;
  font-weight: 500;
  text-decoration: none;
  letter-spacing: 0.02em;
}
.todays-cup__cta-arrow {
  transition: transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}
.todays-cup__cta:hover .todays-cup__cta-arrow {
  transform: translateX(4px);
}
.todays-cup__empty {
  padding: 48px 0;
  color: var(--cozy-muted);
  font-size: 15px;
}
.todays-cup__skeleton {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: clamp(48px, 5vw, 64px);
  align-items: center;
}
.skel {
  border-radius: 2px;
  background: linear-gradient(90deg, oklch(0.94 0.01 55) 0%, oklch(0.97 0.008 55) 50%, oklch(0.94 0.01 55) 100%);
  background-size: 200% 100%;
  animation: skel-shine 1.2s ease-in-out infinite;
}
.skel--media { aspect-ratio: 4 / 3; width: 100%; }
.skel-stack { display: grid; gap: 14px; }
.skel--line { height: 14px; width: 70%; }
.skel--sm { width: 28%; height: 12px; }
.skel--md { width: 42%; }
.skel--lg { width: 55%; height: 28px; }
@keyframes skel-shine {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
.more-flavor {
  margin-top: clamp(48px, 6vw, 72px);
}
.more-flavor__header h3 {
  margin: 0 0 28px;
  font-family: var(--font-display);
  font-size: clamp(1.35rem, 2vw, 1.75rem);
  font-weight: 500;
}
.more-flavor__list {
  border-top: 1px solid var(--cozy-ink);
}
.coffee-row {
  position: relative;
  display: grid;
  gap: 6px;
  padding: 22px 4px 22px 12px;
  color: inherit;
  border-bottom: 1px solid var(--cozy-border);
  text-decoration: none;
  transition: background-color 0.22s ease;
}
.coffee-row::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  width: 2px;
  height: 22px;
  border-radius: 999px;
  background: var(--menu-accent, var(--cozy-primary));
  transform: translateY(-50%) scaleY(0);
  transform-origin: center;
  transition: transform 0.2s cubic-bezier(0.22, 1, 0.36, 1);
}
.coffee-row.is-featured::before {
  transform: translateY(-50%) scaleY(1);
}
.coffee-row:hover,
.coffee-row:focus-visible {
  background: oklch(0.97 0.008 55);
}
.coffee-row__top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 24px;
}
.coffee-row__name {
  font-size: 17px;
  font-weight: 500;
  transition: color 0.2s ease;
}
.coffee-row.is-featured .coffee-row__name,
.coffee-row:hover .coffee-row__name {
  color: var(--menu-accent, var(--cozy-primary));
}
.coffee-row__origin {
  display: block;
  margin-top: 2px;
  color: var(--cozy-muted);
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.04em;
}
.coffee-row__price {
  font-size: 14px;
  font-weight: 400;
  color: var(--cozy-muted);
  white-space: nowrap;
}
.coffee-row__notes {
  color: var(--cozy-muted);
  font-size: 13px;
  letter-spacing: 0.02em;
  max-width: 55ch;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.more-flavor__empty {
  margin: 0;
  padding: 36px 4px;
  color: var(--cozy-muted);
  font-size: 14px;
}
.menu-series {
  margin-top: clamp(96px, 11vw, 120px);
}
.menu-series__header {
  margin-bottom: 36px;
}
.menu-series__kicker {
  margin: 0 0 12px;
  color: var(--cozy-muted);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
.menu-series__header h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.35rem, 2vw, 1.75rem);
  font-weight: 500;
}
.menu-series__lead {
  margin: 12px 0 0;
  color: var(--cozy-muted);
  font-size: 15px;
  font-weight: 400;
  line-height: 1.6;
}
.menu-series__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 24px 28px;
}
.series-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: inherit;
  text-decoration: none;
  text-align: center;
}
.series-card__media {
  aspect-ratio: 1 / 1;
  overflow: hidden;
  border-radius: 2px;
  background: oklch(0.95 0.012 55);
}
.series-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.series-card:hover .series-card__media img,
.series-card:focus-visible .series-card__media img {
  transform: scale(1.03);
}
.series-card__en {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--cozy-ink);
}
.series-card__name {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 400;
  color: var(--cozy-muted);
  letter-spacing: 0.02em;
}
.series-card__explore {
  color: var(--menu-accent, var(--cozy-primary));
  font-size: 13px;
  font-weight: 500;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.series-card:hover .series-card__explore,
.series-card:focus-visible .series-card__explore {
  opacity: 1;
  transform: translateY(0);
}
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.28s ease-out, transform 0.28s ease-out;
}
.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
@media (max-width: 1024px) {
  .editorial-menu__shell {
    width: min(100% - 48px, 960px);
  }
}
@media (max-width: 900px) {
  .menu-hero,
  .todays-cup__body,
  .todays-cup__skeleton {
    grid-template-columns: 1fr;
    gap: 32px;
  }
  .menu-hero__media {
    order: -1;
    max-width: 520px;
  }
  .todays-cup__kicker { margin-bottom: 18px; }
  .todays-cup__story { min-height: 0; }
}
@media (max-width: 600px) {
  .editorial-menu { padding-block: 64px; }
  .editorial-menu__shell { width: min(100% - 32px, 520px); }
  .flavor-nav__lead { font-size: 13px; }
  .flavor-nav__items {
    flex-wrap: nowrap;
    gap: 0 28px;
    margin-right: -16px;
    padding-right: 16px;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .flavor-nav__items::-webkit-scrollbar { display: none; }
  .flavor-nav__item { flex: 0 0 auto; }
  .todays-cup,
  .more-flavor,
  .menu-series { margin-top: 56px; }
  .menu-series__grid {
    grid-auto-flow: column;
    grid-auto-columns: minmax(132px, 42vw);
    grid-template-columns: none;
    overflow-x: auto;
    margin-right: -16px;
    padding-right: 16px;
    scrollbar-width: none;
  }
  .menu-series__grid::-webkit-scrollbar { display: none; }
  .series-card__explore { opacity: 0.7; transform: none; }
}
@media (prefers-reduced-motion: reduce) {
  .menu-fade-enter-active,
  .menu-fade-leave-active,
  .flavor-nav__item::after,
  .todays-cup__media img,
  .todays-cup__cta-arrow,
  .series-card__media img,
  .series-card__explore,
  .coffee-row,
  .coffee-row::before,
  .skel {
    transition: none !important;
    animation: none !important;
  }
  .menu-fade-enter-from,
  .menu-fade-leave-to { transform: none; }
}
</style>
