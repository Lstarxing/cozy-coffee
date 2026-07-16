<template>
  <section id="menu" class="editorial-menu" aria-labelledby="editorial-menu-title">
    <div class="editorial-menu__shell">
      <header class="editorial-menu__header">
        <h2 id="editorial-menu-title">今天，想喝哪一杯</h2>
      </header>

      <div class="flavor-direction">
        <p class="flavor-direction__label">风味</p>
        <nav class="flavor-directory" aria-label="选择今天的风味">
          <button
            v-for="route in flavorRoutes"
            :key="route.id"
            type="button"
            :class="{ 'is-active': activeRouteId === route.id }"
            :aria-pressed="activeRouteId === route.id"
            @click="activeRouteId = route.id"
          >
            {{ route.label }}
          </button>
        </nav>
        <Transition name="menu-fade" mode="out-in">
          <p :key="activeRoute.id" class="flavor-direction__description">{{ activeRoute.description }}</p>
        </Transition>
      </div>

      <div class="editorial-menu__body">
        <div class="editorial-menu__column editorial-menu__column--left">
          <Transition name="menu-fade" mode="out-in">
            <router-link
              :key="activeRoute.id"
              class="featured-coffee"
              to="/member/order"
              :aria-label="`查看今日推荐：${featuredCoffee.name}`"
            >
              <div class="featured-coffee__image">
                <picture>
                  <source type="image/avif" :srcset="activeRoute.coverImage.avif" sizes="(max-width: 600px) calc(100vw - 32px), (max-width: 820px) 44vw, 42vw">
                  <source type="image/webp" :srcset="activeRoute.coverImage.webp" sizes="(max-width: 600px) calc(100vw - 32px), (max-width: 820px) 44vw, 42vw">
                  <img
                    :src="activeRoute.coverImage.fallback"
                    :srcset="activeRoute.coverImage.jpg"
                    sizes="(max-width: 600px) calc(100vw - 32px), (max-width: 820px) 44vw, 42vw"
                    :alt="activeRoute.imageAlt"
                    loading="lazy"
                    width="1200"
                    height="896"
                    @error="$emit('image-error', $event)"
                  >
                </picture>
              </div>
              <div class="featured-coffee__content">
                <p class="featured-coffee__kicker">今日推荐</p>
                <h3>{{ featuredCoffee.name }}</h3>
                <p class="featured-coffee__roast">{{ roastLabel(featuredCoffee.roastLevel) }}</p>
                <p class="featured-coffee__flavors">{{ featuredCoffee.flavorNotes.join(' · ') }}</p>
                <p class="featured-coffee__story">{{ activeRoute.featured.story }}</p>
                <strong class="featured-coffee__price">{{ featuredCoffee.price }}</strong>
              </div>
            </router-link>
          </Transition>

          <router-link class="editorial-menu__all" to="/member/order">查看完整菜单 <span aria-hidden="true">→</span></router-link>
        </div>

        <Transition name="menu-fade" mode="out-in">
          <section :key="activeRoute.id" class="coffee-group" aria-labelledby="active-coffee-menu-title">
            <header class="coffee-group__header">
              <h3 id="active-coffee-menu-title">{{ activeRoute.menu.title }}</h3>
            </header>

            <div class="coffee-group__list">
              <router-link
                v-for="coffee in recommendedProducts"
                :key="coffee.id"
                class="coffee-row"
                to="/member/order"
              >
                <span class="coffee-row__name">{{ coffee.name }}</span>
                <span class="coffee-row__flavors">{{ coffee.flavorNotes.slice(0, 2).join(' · ') }}</span>
                <strong>{{ coffee.price }}</strong>
              </router-link>
            </div>
          </section>
        </Transition>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  products: { type: Array, required: true },
  flavorRoutes: { type: Array, required: true }
})

defineEmits(['image-error'])

const routes = computed(() => (Array.isArray(props.flavorRoutes) && props.flavorRoutes.length ? props.flavorRoutes : []))
const activeRouteId = ref(props.flavorRoutes?.[0]?.id)
const activeRoute = computed(() =>
  routes.value.find(route => route.id === activeRouteId.value) || routes.value[0]
)
const featuredCoffee = computed(() =>
  props.products.find(product => product.id === activeRoute.value?.featured?.productId) || props.products[0]
)
const recommendedProducts = computed(() =>
  (activeRoute.value?.menu?.productIds || [])
    .map(id => props.products.find(product => product.id === id))
    .filter(product => product?.available !== false)
)

function roastLabel(level) {
  return ({ light: 'Light Roast', medium: 'Medium Roast', dark: 'Dark Roast' })[level] || level
}
</script>

<style scoped>
.editorial-menu { padding-block: clamp(80px, 7.5vw, 120px); color: var(--cozy-ink); background: var(--cozy-surface); }
.editorial-menu__shell { width: min(1180px, calc(100% - 48px)); margin-inline: auto; }
.editorial-menu :where(a, button):focus-visible { outline: 3px solid var(--cozy-primary); outline-offset: 4px; }
.editorial-menu__header { display: flex; align-items: end; justify-content: space-between; gap: 32px; }
.editorial-menu__header h2 { max-width: 8em; margin: 0; font-family: var(--font-display); font-size: clamp(2rem, 4vw, 4rem); font-weight: 500; line-height: 1.14; letter-spacing: .01em; text-wrap: balance; }
.editorial-menu__all { min-height: 44px; display: inline-flex; align-items: center; gap: 14px; margin-top: 28px; color: var(--cozy-primary); font-weight: 650; text-decoration: none; }
.editorial-menu__all span { transition: transform .24s cubic-bezier(.22, 1, .36, 1); }
.editorial-menu__all:hover span { transform: translateX(4px); }

.flavor-direction { margin-top: clamp(20px, 2.4vw, 30px); }
.flavor-direction__label { margin: 0 0 10px; color: var(--cozy-muted); font-size: 12px; letter-spacing: .08em; }
.flavor-directory { display: flex; flex-wrap: wrap; gap: 18px 32px; padding-bottom: 14px; border-bottom: 1px solid var(--cozy-border); }
.flavor-directory button { position: relative; min-height: 36px; padding: 0; border: 0; color: var(--cozy-muted); background: transparent; font: inherit; font-size: 13px; font-weight: 500; cursor: pointer; }
.flavor-directory button::after { position: absolute; right: 0; bottom: 0; left: 0; height: 1px; background: var(--cozy-primary); content: ''; transform: scaleX(0); transform-origin: left; transition: transform .24s ease; }
.flavor-directory button:hover,
.flavor-directory button.is-active { color: var(--cozy-primary); font-weight: 650; }
.flavor-directory button:hover::after,
.flavor-directory button.is-active::after { transform: scaleX(1); }
.flavor-direction__description { width: min(31em, 100%); min-height: 3.6em; margin: 16px 0 0; color: var(--cozy-muted); font-family: var(--font-display); font-size: clamp(1.05rem, 1.45vw, 1.3rem); line-height: 1.6; }

.editorial-menu__body { min-height: 560px; display: grid; grid-template-columns: minmax(460px, .95fr) minmax(0, 1.05fr); gap: clamp(44px, 5vw, 68px); padding-top: 28px; }
.editorial-menu__column--left { display: flex; flex-direction: column; min-width: 0; }
.featured-coffee { min-width: 0; flex: 1; color: inherit; text-decoration: none; }
.featured-coffee__image { width: 100%; max-width: 520px; height: clamp(200px, 18vw, 220px); overflow: hidden; background: var(--cozy-bg); }
.featured-coffee__image picture,
.featured-coffee__image img { width: 100%; height: 100%; display: block; }
.featured-coffee__image img { object-fit: cover; transition: transform .28s cubic-bezier(.22, 1, .36, 1); }
.featured-coffee:hover .featured-coffee__image img,
.featured-coffee:focus-visible .featured-coffee__image img { transform: scale(1.03); }
.featured-coffee__content { min-height: 274px; padding-top: 22px; }
.featured-coffee__kicker { margin: 0; color: var(--cozy-muted); font-size: 12px; letter-spacing: .08em; }
.featured-coffee h3 { margin: 10px 0 0; font-family: var(--font-display); font-size: clamp(2rem, 3.4vw, 3.3rem); font-weight: 500; line-height: 1.12; }
.featured-coffee__roast { margin: 13px 0 0; color: var(--cozy-primary); font-size: 12px; font-weight: 650; letter-spacing: .06em; }
.featured-coffee__flavors { margin: 18px 0 0; color: var(--cozy-ink); font-size: 15px; }
.featured-coffee__story { max-width: 28em; margin: 17px 0 0; color: var(--cozy-muted); font-size: 16px; line-height: 1.75; text-wrap: pretty; }
.featured-coffee__price { display: block; margin-top: 22px; font-size: 17px; font-weight: 650; }

.coffee-group { min-width: 0; }
.coffee-group__header { display: flex; align-items: baseline; justify-content: space-between; gap: 24px; padding-bottom: 18px; border-bottom: 1px solid var(--cozy-ink); }
.coffee-group__header h3 { margin: 0; font-family: var(--font-display); font-size: clamp(1.5rem, 2vw, 2rem); font-weight: 500; }
.coffee-group__header p { margin: 0; color: var(--cozy-muted); font-size: 12px; letter-spacing: .06em; }
.coffee-group__list { min-height: 192px; }
.coffee-row { position: relative; min-height: 64px; display: grid; grid-template-columns: minmax(120px, .8fr) minmax(120px, 1fr) auto; align-items: center; gap: 12px 20px; color: inherit; border-bottom: 1px solid var(--cozy-border); text-decoration: none; transition: border-color .24s ease, color .24s ease; }
.coffee-row::before { position: absolute; top: 0; bottom: -1px; left: -12px; width: 2px; background: var(--cozy-primary); content: ''; opacity: 0; transform: scaleY(.4); transition: opacity .24s ease, transform .24s ease; }
.coffee-row:hover,
.coffee-row:focus-visible { border-bottom-color: color-mix(in oklch, var(--cozy-primary) 55%, var(--cozy-border)); }
.coffee-row:hover::before,
.coffee-row:focus-visible::before { opacity: .72; transform: scaleY(1); }
.coffee-row__name { font-size: 17px; font-weight: 550; transition: font-weight .2s ease; }
.coffee-row:hover .coffee-row__name,
.coffee-row:focus-visible .coffee-row__name { color: var(--cozy-primary); font-weight: 700; }
.coffee-row__flavors { color: var(--cozy-muted); font-size: 13px; }
.coffee-row strong { font-size: 14px; font-weight: 600; white-space: nowrap; }

.menu-fade-enter-active,
.menu-fade-leave-active { transition: opacity .13s ease; }
.menu-fade-enter-from,
.menu-fade-leave-to { opacity: 0; }

@media (max-width: 880px) {
  .editorial-menu__body { min-height: 0; grid-template-columns: 1fr; gap: 40px; }
  .editorial-menu__column--left { max-width: none; }
  .featured-coffee { display: grid; grid-template-columns: minmax(220px, 1fr) minmax(0, 1fr); gap: 28px; align-items: center; }
  .featured-coffee__content { padding-top: 0; }
  .coffee-group { margin-top: 8px; }
}

@media (max-width: 600px) {
  .editorial-menu { padding-block: 72px; }
  .editorial-menu__shell { width: min(100% - 32px, 520px); }
  .editorial-menu__header h2 { max-width: 7em; }
  .flavor-directory { flex-wrap: nowrap; gap: 26px; margin-right: -16px; padding-right: 16px; overflow-x: auto; scrollbar-width: none; }
  .flavor-directory::-webkit-scrollbar { display: none; }
  .flavor-direction__description { min-height: 4.8em; }
  .editorial-menu__all { margin-top: 36px; }
  .featured-coffee { display: block; }
  .featured-coffee__image { height: clamp(200px, 56vw, 220px); max-width: none; }
  .featured-coffee__content { min-height: 260px; padding-top: 20px; }
  .coffee-group__header { padding-bottom: 14px; }
  .coffee-row { min-height: 72px; grid-template-columns: 1fr auto; gap: 7px 16px; padding-block: 12px; }
  .coffee-row__flavors { grid-column: 1; }
  .coffee-row strong { grid-row: 1 / 3; grid-column: 2; }
}

@media (prefers-reduced-motion: reduce) {
  .menu-fade-enter-active,
  .menu-fade-leave-active { transition: none; }
  .featured-coffee__image img,
  .editorial-menu__all span,
  .flavor-directory button::after,
  .coffee-row,
  .coffee-row::before { transition: none; }
}
</style>
