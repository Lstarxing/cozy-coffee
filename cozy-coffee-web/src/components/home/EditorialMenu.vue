<template>
  <section id="menu" class="editorial-menu" aria-labelledby="editorial-menu-title">
    <div class="editorial-menu__shell">
      <header class="editorial-menu__header">
        <h2 id="editorial-menu-title">今天，想喝哪一杯</h2>
        <router-link class="editorial-menu__all" to="/member/order">查看完整菜单 <span aria-hidden="true">→</span></router-link>
      </header>

      <nav class="flavor-directory" aria-label="按风味查看今日推荐">
        <button
          v-for="flavor in flavorDirections"
          :key="flavor.id"
          type="button"
          :class="{ 'is-active': selectedFlavor === flavor.id }"
          :aria-pressed="selectedFlavor === flavor.id"
          @click="selectedFlavor = flavor.id"
        >
          {{ flavor.label }}
        </button>
      </nav>

      <div class="editorial-menu__body">
        <router-link
          :key="featuredCoffee.id"
          class="featured-coffee"
          to="/member/order"
          :aria-label="`查看今日推荐：${featuredCoffee.name}`"
        >
          <div class="featured-coffee__image">
            <img
              :src="featuredCoffee.image"
              :alt="featuredCoffee.alt"
              loading="lazy"
              width="720"
              height="900"
              @error="$emit('image-error', $event)"
            >
          </div>
          <div class="featured-coffee__content">
            <p class="featured-coffee__kicker">今日推荐</p>
            <h3>{{ featuredCoffee.name }}</h3>
            <p class="featured-coffee__roast">{{ roastLabel(featuredCoffee.roastLevel) }}</p>
            <p class="featured-coffee__flavors">{{ featuredCoffee.flavors.join(' · ') }}</p>
            <p class="featured-coffee__story">{{ featuredCoffee.story }}</p>
            <strong class="featured-coffee__price">{{ featuredCoffee.price }}</strong>
          </div>
        </router-link>

        <div class="coffee-groups">
          <section v-for="group in displayGroups" :key="group.id" class="coffee-group" :aria-labelledby="`coffee-group-${group.id}`">
            <header class="coffee-group__header">
              <h3 :id="`coffee-group-${group.id}`">{{ group.title }}</h3>
              <p>{{ group.subtitle }}</p>
            </header>

            <div class="coffee-group__list">
              <router-link
                v-for="coffee in group.items"
                :key="coffee.id"
                class="coffee-row"
                to="/member/order"
              >
                <span class="coffee-row__name">{{ coffee.name }}</span>
                <span class="coffee-row__flavors">{{ coffee.flavors.slice(0, 2).join(' · ') }}</span>
                <strong>{{ coffee.price }}</strong>
              </router-link>
            </div>
          </section>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  products: { type: Array, required: true }
})

defineEmits(['image-error'])

const flavorDirections = Object.freeze([
  { id: 'floral', label: '花香' },
  { id: 'nutty', label: '坚果' },
  { id: 'milky', label: '奶香' },
  { id: 'cocoa', label: '可可' },
  { id: 'refreshing', label: '清爽' }
])

const initialFeatured = props.products.find(product => product.featured) || props.products[0]
const selectedFlavor = ref(initialFeatured?.flavorFamily || flavorDirections[0].id)

const featuredCoffee = computed(() =>
  props.products.find(product => product.featured && product.flavorFamily === selectedFlavor.value)
  || props.products.find(product => product.flavorFamily === selectedFlavor.value)
  || initialFeatured
)

const displayGroups = computed(() => [
  {
    id: 'classic',
    title: '经典咖啡',
    subtitle: 'Espresso Based',
    items: props.products
      .filter(product => product.group === 'classic' && product.available !== false)
      .sort((a, b) => a.displayOrder - b.displayOrder)
  },
  {
    id: 'single-origin',
    title: '单一产地',
    subtitle: 'Single Origin',
    items: props.products
      .filter(product => product.group === 'single-origin' && product.available !== false)
      .sort((a, b) => a.displayOrder - b.displayOrder)
  }
])

function roastLabel(level) {
  return ({ light: 'Light Roast', medium: 'Medium Roast', dark: 'Dark Roast' })[level] || level
}
</script>

<style scoped>
.editorial-menu { padding-block: clamp(88px, 9vw, 144px); color: var(--cozy-ink); background: var(--cozy-surface); }
.editorial-menu__shell { width: min(1180px, calc(100% - 48px)); margin-inline: auto; }
.editorial-menu :where(a, button):focus-visible { outline: 3px solid var(--cozy-primary); outline-offset: 4px; }
.editorial-menu__header { display: flex; align-items: end; justify-content: space-between; gap: 32px; }
.editorial-menu__header h2 { max-width: 8em; margin: 0; font-family: var(--font-display); font-size: clamp(2.5rem, 4.8vw, 4.5rem); font-weight: 500; line-height: 1.12; letter-spacing: .01em; text-wrap: balance; }
.editorial-menu__all { min-height: 44px; display: inline-flex; align-items: center; gap: 14px; color: var(--cozy-primary); font-weight: 650; text-decoration: none; }
.editorial-menu__all span { transition: transform .24s cubic-bezier(.22, 1, .36, 1); }
.editorial-menu__all:hover span { transform: translateX(4px); }

.flavor-directory { display: flex; flex-wrap: wrap; gap: 18px 32px; margin-top: clamp(38px, 5vw, 64px); padding-bottom: 20px; border-bottom: 1px solid var(--cozy-border); }
.flavor-directory button { position: relative; min-height: 36px; padding: 0; border: 0; color: var(--cozy-muted); background: transparent; font: inherit; font-size: 13px; font-weight: 500; cursor: pointer; }
.flavor-directory button::after { position: absolute; right: 0; bottom: 0; left: 0; height: 1px; background: var(--cozy-primary); content: ''; transform: scaleX(0); transform-origin: left; transition: transform .24s ease; }
.flavor-directory button:hover,
.flavor-directory button.is-active { color: var(--cozy-primary); font-weight: 650; }
.flavor-directory button:hover::after,
.flavor-directory button.is-active::after { transform: scaleX(1); }

.editorial-menu__body { display: grid; grid-template-columns: minmax(0, .82fr) minmax(0, 1.18fr); gap: clamp(48px, 6vw, 88px); padding-top: clamp(48px, 6vw, 80px); }
.featured-coffee { min-width: 0; color: inherit; text-decoration: none; }
.featured-coffee__image { aspect-ratio: 4 / 5; overflow: hidden; background: var(--cozy-bg); }
.featured-coffee__image img { width: 100%; height: 100%; display: block; object-fit: cover; transition: transform .28s cubic-bezier(.22, 1, .36, 1); }
.featured-coffee:hover .featured-coffee__image img,
.featured-coffee:focus-visible .featured-coffee__image img { transform: scale(1.03); }
.featured-coffee__content { padding-top: 28px; animation: menu-content-in .26s ease-out both; }
.featured-coffee__kicker { margin: 0; color: var(--cozy-muted); font-size: 12px; letter-spacing: .08em; }
.featured-coffee h3 { margin: 10px 0 0; font-family: var(--font-display); font-size: clamp(2rem, 3.4vw, 3.3rem); font-weight: 500; line-height: 1.12; }
.featured-coffee__roast { margin: 13px 0 0; color: var(--cozy-primary); font-size: 12px; font-weight: 650; letter-spacing: .06em; }
.featured-coffee__flavors { margin: 22px 0 0; color: var(--cozy-ink); font-size: 15px; }
.featured-coffee__story { max-width: 28em; margin: 20px 0 0; color: var(--cozy-muted); font-size: 16px; line-height: 1.85; text-wrap: pretty; }
.featured-coffee__price { display: block; margin-top: 25px; font-size: 17px; font-weight: 650; }

.coffee-groups { min-width: 0; }
.coffee-group + .coffee-group { margin-top: clamp(54px, 6vw, 78px); }
.coffee-group__header { display: flex; align-items: baseline; justify-content: space-between; gap: 24px; padding-bottom: 18px; border-bottom: 1px solid var(--cozy-ink); }
.coffee-group__header h3 { margin: 0; font-family: var(--font-display); font-size: clamp(1.5rem, 2vw, 2rem); font-weight: 500; }
.coffee-group__header p { margin: 0; color: var(--cozy-muted); font-size: 12px; letter-spacing: .06em; }
.coffee-row { position: relative; min-height: 72px; display: grid; grid-template-columns: minmax(120px, .8fr) minmax(160px, 1.2fr) auto; align-items: center; gap: 20px; color: inherit; border-bottom: 1px solid var(--cozy-border); text-decoration: none; transition: border-color .24s ease, color .24s ease; }
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

@keyframes menu-content-in { from { opacity: .35; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 820px) {
  .editorial-menu__body { grid-template-columns: 1fr; }
  .featured-coffee { display: grid; grid-template-columns: minmax(220px, .9fr) minmax(0, 1.1fr); gap: 32px; align-items: center; }
  .featured-coffee__content { padding-top: 0; }
}

@media (max-width: 600px) {
  .editorial-menu { padding-block: 72px; }
  .editorial-menu__shell { width: min(100% - 32px, 520px); }
  .editorial-menu__header { align-items: flex-start; flex-direction: column; gap: 18px; }
  .editorial-menu__header h2 { max-width: 7em; }
  .flavor-directory { flex-wrap: nowrap; gap: 26px; margin-right: -16px; padding-right: 16px; overflow-x: auto; scrollbar-width: none; }
  .flavor-directory::-webkit-scrollbar { display: none; }
  .featured-coffee { display: block; }
  .featured-coffee__content { padding-top: 24px; }
  .coffee-group__header { align-items: flex-start; flex-direction: column; gap: 8px; }
  .coffee-row { min-height: 78px; grid-template-columns: 1fr auto; gap: 7px 16px; padding-block: 12px; }
  .coffee-row__flavors { grid-column: 1; }
  .coffee-row strong { grid-row: 1 / 3; grid-column: 2; }
}

@media (prefers-reduced-motion: reduce) {
  .featured-coffee__content { animation: none; }
  .featured-coffee__image img,
  .editorial-menu__all span,
  .flavor-directory button::after,
  .coffee-row,
  .coffee-row::before { transition: none; }
}
</style>
