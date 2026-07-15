<template>
  <section class="origins-journey" aria-labelledby="origins-title">
    <div ref="journeyRoot" id="origins" class="origins-layout origins-shell">
      <div class="origins-left-column">
        <div class="origins-heading">
          <h2 id="origins-title">风味从土地开始</h2>
          <p class="origins-slogan">Eight Origins. One Cup.</p>
          <p>沿着赤道咖啡带，探索八处产地如何在杭州汇聚成 Cozy Coffee 的风味语言。</p>
        </div>

        <div class="origins-map-sticky">
          <CoffeeBeltMap
            :origins="COFFEE_ORIGINS"
            :active-origin="activeOrigin"
            :visited-origins="visitedOrigins"
            :is-summary="activeChapter?.type === 'summary'"
          />
          <div class="journey-progress" aria-live="polite">
            <div class="journey-progress__copy">
              <span>{{ activeChapter?.type === 'summary' ? '08 / 08' : `${activeOriginNumber} / 08` }}</span>
              <strong>{{ activeChapter?.type === 'summary' ? 'HANGZHOU' : activeOrigin?.englishName }}</strong>
              <span v-if="activeChapter?.type !== 'summary'">→ 杭州烘焙中心</span>
            </div>
            <div class="journey-progress__track" aria-hidden="true">
              <span :style="{ '--journey-scale': journeyProgress / 100 }"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="origin-chapters" aria-label="咖啡产区章节">
        <OriginChapter
          v-for="(chapter, index) in COFFEE_JOURNEY"
          :key="chapter.id"
          :chapter="chapter"
          :number="String(index + 1).padStart(2, '0')"
          :active="activeChapterId === chapter.id"
          :static-mode="!observerSupported"
        />
      </div>
    </div>

    <div class="origins-to-menu origins-shell">
      <p>接下来，选择今天杯中的风味表达。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import CoffeeBeltMap from './CoffeeBeltMap.vue'
import OriginChapter from './OriginChapter.vue'
import { COFFEE_JOURNEY, COFFEE_ORIGINS } from '@/data/coffeeOrigins'

const activeChapterId = ref(COFFEE_JOURNEY[0].id)
const visitedOrigins = ref(new Set())
const journeyRoot = ref(null)
const observerSupported = ref(true)
let chapterObserver = null
let lastObservedScrollY = 0

const activeChapter = computed(() =>
  COFFEE_JOURNEY.find(chapter => chapter.id === activeChapterId.value) || COFFEE_JOURNEY[0]
)
const activeOrigin = computed(() =>
  activeChapter.value.type === 'origin' ? activeChapter.value : null
)
const activeOriginIndex = computed(() =>
  Math.max(0, COFFEE_ORIGINS.findIndex(origin => origin.id === activeChapterId.value))
)
const activeOriginNumber = computed(() => String(activeOriginIndex.value + 1).padStart(2, '0'))
const journeyProgress = computed(() =>
  activeChapter.value.type === 'summary' ? 100 : ((activeOriginIndex.value + 1) / COFFEE_ORIGINS.length) * 100
)

function activateChapter(id) {
  activeChapterId.value = id
  const chapter = COFFEE_ORIGINS.find(origin => origin.id === id)
  if (chapter) visitedOrigins.value = new Set(visitedOrigins.value).add(id)
}

function resetJourneyForNavigation() {
  if (window.location.hash !== '#origins') return

  visitedOrigins.value = new Set()
  activateChapter(COFFEE_JOURNEY[0].id)
}

onMounted(() => {
  const chapters = journeyRoot.value?.querySelectorAll('[data-origin-id]') || []
  lastObservedScrollY = window.scrollY
  window.addEventListener('hashchange', resetJourneyForNavigation)
  if (window.location.hash === '#origins') requestAnimationFrame(resetJourneyForNavigation)

  if (!('IntersectionObserver' in window)) {
    observerSupported.value = false
    return
  }

  chapterObserver = new IntersectionObserver(
    entries => {
      const scrollingDown = window.scrollY >= lastObservedScrollY
      lastObservedScrollY = window.scrollY
      const entering = entries.filter(entry => entry.isIntersecting)
      if (!entering.length) return

      entering.sort((a, b) => scrollingDown
        ? b.boundingClientRect.top - a.boundingClientRect.top
        : a.boundingClientRect.top - b.boundingClientRect.top
      )
      activateChapter(entering[0].target.dataset.originId)
    },
    { rootMargin: '-22% 0px -76% 0px', threshold: 0 }
  )
  chapters.forEach(chapter => chapterObserver.observe(chapter))
})

onUnmounted(() => {
  chapterObserver?.disconnect()
  window.removeEventListener('hashchange', resetJourneyForNavigation)
})
</script>

<style scoped>
.origins-journey { padding-block: clamp(80px, 9vw, 128px) 0; background: var(--cozy-bg); color: var(--cozy-ink); }
.origins-shell { width: min(1320px, calc(100% - 48px)); margin-inline: auto; }
.origins-left-column { position: sticky; top: calc(var(--nav-height) + 12px); min-width: 0; height: calc(100svh - var(--nav-height) - 24px); align-self: start; display: flex; flex-direction: column; }
.origins-heading { max-width: 720px; padding-bottom: 12px; }
.origins-heading h2 { margin: 0; font-size: clamp(2rem, 4vw, 3.25rem); line-height: 1.15; font-weight: 600; text-wrap: balance; }
.origins-slogan { margin: 8px 0 0; color: var(--cozy-primary); font-size: 15px; font-weight: 650; letter-spacing: -.01em; }
.origins-heading > p:last-child { max-width: 34em; margin: 8px 0 0; color: var(--cozy-muted); font-size: 17px; line-height: 1.6; text-wrap: pretty; }
.origins-layout { display: grid; grid-template-columns: minmax(0, 2fr) minmax(340px, .82fr); gap: 48px; align-items: start; scroll-margin-top: calc(var(--nav-height) + 12px); }
.origins-map-sticky { min-width: 0; min-height: 0; flex: 0 0 auto; display: flex; flex-direction: column; padding-top: 10px; }
.origins-map-sticky :deep(.coffee-map) { width: 110%; max-width: none; margin-left: -5%; }
.journey-progress { margin: 16px 0 0; padding-block: 12px 0; border-top: 1px solid var(--cozy-border); color: var(--cozy-muted); font-size: 12px; }
.journey-progress__copy { display: flex; align-items: baseline; gap: 10px 14px; }
.journey-progress__copy strong { color: var(--cozy-ink); font-size: 14px; }
.journey-progress__track { height: 2px; margin-top: 12px; overflow: hidden; background: var(--cozy-border); }
.journey-progress__track span { display: block; width: 100%; height: 100%; background: var(--cozy-primary); transform: scaleX(var(--journey-scale)); transform-origin: left center; transition: transform .5s cubic-bezier(.22, 1, .36, 1); }
.origin-chapters { min-width: 0; }
.origins-to-menu { padding-block: 56px 104px; text-align: center; }
.origins-to-menu p { max-width: 21em; margin: 0 auto; color: var(--cozy-primary); font-size: clamp(1.3rem, 2.4vw, 2rem); line-height: 1.45; text-wrap: balance; }

@media (max-width: 820px) {
  .origins-journey { padding-top: 96px; }
  .origins-layout { display: block; width: 100%; }
  .origins-left-column { position: static; height: auto; display: block; }
  .origins-heading { width: min(100% - 32px, 620px); margin-inline: auto; }
  .origins-map-sticky { position: sticky; top: var(--nav-height); z-index: 2; height: 40svh; padding: 8px 16px 0; background: var(--cozy-bg); border-bottom: 1px solid var(--cozy-border); }
  .origins-map-sticky :deep(.coffee-map) { width: 100%; height: calc(40svh - 44px); margin-left: 0; display: grid; align-items: center; }
  .journey-progress { margin: 4px 0 0; padding: 7px 0 5px; }
  .journey-progress__copy { justify-content: center; }
  .journey-progress__track { margin-top: 7px; }
  .origin-chapters { width: min(100% - 32px, 620px); margin-inline: auto; }
}

@media (max-width: 520px) {
  .origins-shell { width: min(100% - 32px, 520px); }
  .origins-heading { padding-bottom: 48px; }
}

@media (prefers-reduced-motion: reduce) {
  .journey-progress__track span { transition: none; }
}
</style>
